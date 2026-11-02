package fr.adrien.reelsblocker

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Bloque les Reels d'Instagram, sauf ceux ouverts depuis une conversation privée.
 *
 * Écrans reconnus (via les identifiants de vues) :
 *  - REELS_TAB    : lecteur de Reels avec la barre d'onglets visible (onglet Reels)
 *                   -> on clique sur l'onglet Accueil (jamais de retour arrière : ça fermerait Instagram).
 *  - REELS_VIEWER : lecteur plein écran ouvert depuis un autre écran.
 *                   -> autorisé si on venait d'un DM, sinon un seul retour arrière (ferme le lecteur).
 *  - DM           : conversation privée.
 *  - MAIN         : un onglet principal (accueil, recherche, profil...).
 *  - OTHER        : le reste (commentaires, profil d'un ami, transitions...) : ne change rien.
 *
 * L'origine d'un Reel = le dernier écran « sûr » (DM ou MAIN) vu avant son ouverture,
 * et elle ne s'efface qu'en revenant sur un écran sûr : un passage par les commentaires
 * ne transforme plus un Reel de DM en Reel bloqué.
 */
class ReelsBlockerService : AccessibilityService() {

    enum class Screen { REELS_TAB, REELS_VIEWER, DM, MAIN, OTHER }

    private class Scan(
        val ids: Set<String>,
        val selected: Set<String>,
        val homeTab: AccessibilityNodeInfo?,
        /** Identifiants de la pile de vues réellement au premier plan au centre de l'écran. */
        val topCenter: List<String>,
        /** Idem au niveau de la barre d'onglets, en bas de l'écran. */
        val topBottom: List<String>,
    )

    private var origin = Screen.MAIN       // dernier écran sûr (DM ou MAIN)
    private var inReelSession = false
    private var reelSessionAllowed = false
    private var reelSessionStartedAt = 0L
    private var lastScanAt = 0L
    private var lastActionAt = 0L
    private var lastSwipeAt = 0L
    private var swipeCount = 0
    private val recentActions = ArrayDeque<Long>()
    private var pausedUntil = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Prefs.setDiag(this, "connected", clock())
        Log.i(TAG, "Service connecté")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val debug = Prefs.debug(this)
        val pkg = event.packageName?.toString()
        if (debug) diagEvent(pkg)
        if (pkg != IG_PACKAGE) return
        if (!Prefs.enabled(this)) return

        val now = SystemClock.elapsedRealtime()
        if (now < pausedUntil) return
        val type = event.eventType

        // Les content-changed arrivent en rafale : on limite la fréquence des scans.
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && now - lastScanAt < SCAN_THROTTLE_MS) return
        lastScanAt = now

        val root = instagramRoot()
        if (root == null) {
            if (debug) Prefs.setDiag(this, "root", "${clock()} racine Instagram introuvable")
            return
        }
        val scan = scan(root)
        if (debug) Prefs.setDiag(this, "root", "${clock()} racine OK, ${scan.ids.size} identifiants")
        if (scan.ids.isEmpty()) {
            if (debug) Prefs.saveDebug(this, "SANS_ID", describeNodes(root))
            return // écran en transition : on ne conclut rien
        }
        val screen = classify(scan)

        if (debug) {
            val summary = "écran=$screen  origine=$origin  event=${AccessibilityEvent.eventTypeToString(type)}\n" +
                "1er plan centre: ${scan.topCenter}\n1er plan bas: ${scan.topBottom}\n" +
                "sélectionnés: ${scan.selected.sorted()}\ntous visibles:\n" + scan.ids.sorted().joinToString("\n")
            Log.d(TAG, summary)
            Prefs.saveDebug(this, screen.name, summary)
        }

        when (screen) {
            Screen.DM, Screen.MAIN -> {
                origin = screen
                inReelSession = false
            }
            Screen.OTHER -> Unit // ni origine ni session modifiées
            Screen.REELS_TAB -> {
                inReelSession = false
                goHomeTab(now, scan.homeTab)
            }
            Screen.REELS_VIEWER -> {
                if (!inReelSession) {
                    inReelSession = true
                    reelSessionAllowed = origin == Screen.DM
                    reelSessionStartedAt = now
                    swipeCount = 0
                    lastSwipeAt = 0L
                    log(if (reelSessionAllowed) "Reel envoyé par un ami, laissé ouvert" else "Reel du fil, fermé")
                }
                if (!reelSessionAllowed) {
                    closeViewer(now)
                } else if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED && Prefs.blockDmSwipe(this) && isPagerScroll(event)) {
                    handleSwipeInDmReel(now)
                }
            }
        }
    }

    /** Depuis un DM : le Reel partagé est OK, mais swiper vers les suivants = doomscroll. */
    private fun handleSwipeInDmReel(now: Long) {
        if (now - reelSessionStartedAt < OPENING_GRACE_MS) return // scrolls émis à l'ouverture
        if (now - lastSwipeAt < SWIPE_DEBOUNCE_MS) return          // un swipe = plusieurs événements
        lastSwipeAt = now
        swipeCount++
        if (swipeCount > ALLOWED_SWIPES_FROM_DM) {
            log("Swipe après un Reel d'ami, fermé")
            reelSessionAllowed = false
            closeViewer(now)
        }
    }

    /** Seuls les scrolls du pager vertical des Reels comptent comme un swipe. */
    private fun isPagerScroll(event: AccessibilityEvent): Boolean {
        val id = event.source?.viewIdResourceName ?: return false
        return REELS_PAGER_IDS.any { id.contains(it) }
    }

    /** Lecteur empilé par-dessus un autre écran : un retour arrière le ferme sans quitter Instagram. */
    private fun closeViewer(now: Long) {
        if (!mayAct(now)) return
        performGlobalAction(GLOBAL_ACTION_BACK)
        Prefs.incrementBlocked(this)
    }

    /** Onglet Reels : on bascule sur l'onglet Accueil plutôt que de faire « retour ». */
    private fun goHomeTab(now: Long, homeTab: AccessibilityNodeInfo?) {
        if (!mayAct(now)) return
        val clicked = homeTab?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        log(if (clicked) "Onglet Reels, retour à l'accueil" else "Onglet Reels : accueil introuvable, rien fait")
        if (clicked) Prefs.incrementBlocked(this)
    }

    /** Délai entre deux actions + coupe-circuit : trop d'actions d'affilée = boucle, on se met en pause. */
    private fun mayAct(now: Long): Boolean {
        if (now - lastActionAt < ACTION_COOLDOWN_MS) return false
        while (recentActions.isNotEmpty() && now - recentActions.first() > LOOP_WINDOW_MS) recentActions.removeFirst()
        if (recentActions.size >= LOOP_MAX_ACTIONS) {
            pausedUntil = now + LOOP_PAUSE_MS
            recentActions.clear()
            log("Trop de blocages d'affilée, pause d'une minute")
            return false
        }
        recentActions.addLast(now)
        lastActionAt = now
        return true
    }

    private fun log(msg: String) {
        Log.i(TAG, msg)
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.FRANCE).format(java.util.Date())
        Prefs.appendLog(this, "$stamp $msg")
    }

    private fun clock() = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.FRANCE).format(java.util.Date())

    /**
     * Seul ce qui est AU PREMIER PLAN compte : Instagram garde les onglets visités derrière l'écran
     * courant, et Android les déclare « visibles » même recouverts.
     */
    private fun classify(scan: Scan): Screen {
        val viewerOnTop = scan.topCenter.any { id -> REELS_VIEWER_IDS.any { id.contains(it) } }
        val tabBarOnTop = scan.topBottom.any { id -> TAB_BAR_IDS.any { id == it } }
        return when {
            viewerOnTop && tabBarOnTop -> Screen.REELS_TAB
            viewerOnTop -> Screen.REELS_VIEWER
            scan.ids.any { id -> DM_IDS.any { id.contains(it) } } -> Screen.DM
            tabBarOnTop -> Screen.MAIN
            else -> Screen.OTHER
        }
    }

    /**
     * « Hit-test » : descend l'arbre en suivant, à chaque niveau, l'enfant dessiné en dernier
     * (donc au-dessus) qui contient le point. Renvoie les identifiants rencontrés sur ce chemin.
     */
    private fun topmostIds(root: AccessibilityNodeInfo, x: Int, y: Int): List<String> {
        val out = ArrayList<String>()
        val r = android.graphics.Rect()
        var node: AccessibilityNodeInfo? = root
        var depth = 0
        while (node != null && depth < 60) {
            depth++
            node.viewIdResourceName?.takeIf { it.startsWith(IG_ID_PREFIX) }?.let { out.add(it.removePrefix(IG_ID_PREFIX)) }
            var best: AccessibilityNodeInfo? = null
            var bestOrder = Int.MIN_VALUE
            for (i in 0 until node.childCount) {
                val c = node.getChild(i) ?: continue
                if (!c.isVisibleToUser) continue
                c.getBoundsInScreen(r)
                if (!r.contains(x, y)) continue
                val order = c.drawingOrder
                if (order >= bestOrder) { bestOrder = order; best = c }
            }
            node = best
        }
        return out
    }

    /** Fenêtre active, ou à défaut la fenêtre Instagram parmi celles à l'écran. */
    private fun instagramRoot(): AccessibilityNodeInfo? {
        rootInActiveWindow?.takeIf { it.packageName == IG_PACKAGE }?.let { return it }
        return windows.firstNotNullOfOrNull { w -> w.root?.takeIf { it.packageName == IG_PACKAGE } }
    }

    private var diagEvents = 0
    private var lastDiagWrite = 0L
    private fun diagEvent(pkg: String?) {
        diagEvents++
        val now = SystemClock.elapsedRealtime()
        if (now - lastDiagWrite < 1000) return
        lastDiagWrite = now
        Prefs.setDiag(this, "events", "${clock()} $diagEvents événements reçus, dernier de $pkg")
    }

    /** Quand il n'y a aucun identifiant : classes, descriptions et textes des premiers nœuds. */
    private fun describeNodes(root: AccessibilityNodeInfo): String {
        val out = StringBuilder()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var n = 0
        while (queue.isNotEmpty() && n < 80) {
            val node = queue.removeFirst()
            n++
            val desc = node.contentDescription?.toString()?.take(40)
            val txt = node.text?.toString()?.take(40)
            out.append(node.className?.toString()?.substringAfterLast('.'))
            if (!desc.isNullOrBlank()) out.append(" desc=\"").append(desc).append('"')
            if (!txt.isNullOrBlank()) out.append(" txt=\"").append(txt).append('"')
            if (node.isSelected) out.append(" [selected]")
            out.append('\n')
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return out.toString()
    }

    /** Parcours en largeur de l'arbre de vues, borné pour rester léger. */
    private fun scan(root: AccessibilityNodeInfo): Scan {
        val ids = HashSet<String>()
        val selected = HashSet<String>()
        var homeTab: AccessibilityNodeInfo? = null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            // Instagram garde les onglets visités en mémoire, cachés : on ignore tout ce qui n'est pas à l'écran.
            if (node !== root && !node.isVisibleToUser) continue
            node.viewIdResourceName?.takeIf { it.startsWith(IG_ID_PREFIX) }?.let { full ->
                val id = full.removePrefix(IG_ID_PREFIX)
                ids.add(id)
                if (node.isSelected) selected.add(id)
                if (id == HOME_TAB_ID) homeTab = node
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        val screen = android.graphics.Rect().also(root::getBoundsInScreen)
        val cx = screen.centerX()
        val topCenter = topmostIds(root, cx, screen.top + (screen.height() * 0.45f).toInt())
        val tabBarY = screen.bottom - (TAB_BAR_PROBE_DP * resources.displayMetrics.density).toInt()
        val topBottom = topmostIds(root, cx, tabBarY)
        return Scan(ids, selected, homeTab, topCenter, topBottom)
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "ReelsBlocker"
        private const val IG_PACKAGE = "com.instagram.android"
        private const val IG_ID_PREFIX = "com.instagram.android:id/"

        /** Fragments d'identifiants présents dans le lecteur plein écran de Reels. */
        val REELS_VIEWER_IDS = listOf(
            "clips_viewer_view_pager",
            "clips_viewer_container",
            "clips_video_container",
            "root_clips_layout",
        )
        val REELS_PAGER_IDS = listOf("clips_viewer_view_pager")

        /** Fragments d'identifiants présents dans une conversation privée. */
        val DM_IDS = listOf(
            "direct_thread",
            "row_thread_composer",
            "thread_title",
            "direct_message_list",
        )

        /** Barre d'onglets du bas (identifiants exacts). */
        val TAB_BAR_IDS = listOf("tab_bar", "feed_tab", "clips_tab", "profile_tab")
        const val HOME_TAB_ID = "feed_tab"

        private const val TAB_BAR_PROBE_DP = 28
        private const val LOOP_WINDOW_MS = 15_000L
        private const val LOOP_MAX_ACTIONS = 3
        private const val LOOP_PAUSE_MS = 60_000L
        private const val SCAN_THROTTLE_MS = 150L
        private const val OPENING_GRACE_MS = 1500L
        private const val SWIPE_DEBOUNCE_MS = 700L
        private const val ACTION_COOLDOWN_MS = 2000L
        private const val ALLOWED_SWIPES_FROM_DM = 0
        private const val MAX_NODES = 800
    }
}

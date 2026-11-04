package fr.adrien.reelsblocker

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Blocks Instagram Reels, except the ones opened from a direct message.
 *
 * Screens it recognizes (from view resource ids):
 *  - REELS_TAB    : the Reels player with the bottom tab bar visible (the Reels tab)
 *                   -> taps the Home tab (never Back: that would close Instagram).
 *  - REELS_VIEWER : the fullscreen player opened on top of another screen.
 *                   -> allowed if we came from a DM, otherwise a single Back (closes the player).
 *  - DM           : a direct message thread.
 *  - MAIN         : a main tab (home, search, profile...).
 *  - OTHER        : anything else (comments, a friend's profile, transitions...): changes nothing.
 *
 * A Reel's origin is the last "safe" screen (DM or MAIN) seen before it opened, and it is only
 * cleared when a safe screen shows again: going through the comments no longer turns a Reel
 * from a DM into a blocked one.
 */
class ReelsBlockerService : AccessibilityService() {

    enum class Screen { REELS_TAB, REELS_VIEWER, DM, MAIN, OTHER }

    private class Scan(
        val ids: Set<String>,
        val selected: Set<String>,
        val homeTab: AccessibilityNodeInfo?,
        /** Ids along the view stack that is actually on top at the center of the screen. */
        val topCenter: List<String>,
        /** Same, at the height of the bottom tab bar. */
        val topBottom: List<String>,
    )

    private var origin = Screen.MAIN       // last safe screen (DM or MAIN)
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
        Prefs.setServiceRunning(this, true)
        PauseTile.refresh(this)
        Log.i(TAG, "Service connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Prefs.setServiceRunning(this, false)
        PauseTile.refresh(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Prefs.setServiceRunning(this, false)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val debug = Prefs.debug(this)
        val pkg = event.packageName?.toString()
        if (debug) diagEvent(pkg)
        if (pkg != IG_PACKAGE) return
        if (!Prefs.enabled(this)) return
        if (Prefs.isPaused(this)) return // temporary pause chosen by the user

        val now = SystemClock.elapsedRealtime()
        if (now < pausedUntil) return
        val type = event.eventType

        // Content-changed events come in bursts: throttle the scans.
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && now - lastScanAt < SCAN_THROTTLE_MS) return
        lastScanAt = now

        val root = instagramRoot()
        if (root == null) {
            if (debug) Prefs.setDiag(this, "root", "${clock()} Instagram root not found")
            return
        }
        val scan = scan(root)
        if (debug) Prefs.setDiag(this, "root", "${clock()} root OK, ${scan.ids.size} ids")
        if (scan.ids.isEmpty()) {
            if (debug) Prefs.saveDebug(this, NO_ID, describeNodes(root))
            return // screen in transition: conclude nothing
        }
        val screen = classify(scan)

        if (debug) {
            val summary = "screen=$screen  origin=$origin  event=${AccessibilityEvent.eventTypeToString(type)}\n" +
                "on top (center): ${scan.topCenter}\non top (bottom): ${scan.topBottom}\n" +
                "selected: ${scan.selected.sorted()}\nall visible:\n" + scan.ids.sorted().joinToString("\n")
            Log.d(TAG, summary)
            Prefs.saveDebug(this, screen.name, summary)
        }

        when (screen) {
            Screen.DM, Screen.MAIN -> {
                origin = screen
                inReelSession = false
            }
            Screen.OTHER -> Unit // neither origin nor session change
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
                    log(if (reelSessionAllowed) R.string.log_dm_reel_allowed else R.string.log_feed_reel_closed)
                }
                if (!reelSessionAllowed) {
                    closeViewer(now)
                } else if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED && Prefs.blockDmSwipe(this) && isPagerScroll(event)) {
                    handleSwipeInDmReel(now)
                }
            }
        }
    }

    /** From a DM, the shared Reel is fine, but swiping on to the next ones is doomscrolling. */
    private fun handleSwipeInDmReel(now: Long) {
        if (now - reelSessionStartedAt < OPENING_GRACE_MS) return // scroll events fired while opening
        if (now - lastSwipeAt < SWIPE_DEBOUNCE_MS) return          // one swipe fires several events
        lastSwipeAt = now
        swipeCount++
        if (swipeCount > ALLOWED_SWIPES_FROM_DM) {
            log(R.string.log_dm_swipe_closed)
            reelSessionAllowed = false
            closeViewer(now)
        }
    }

    /** Only scrolls of the vertical Reels pager count as a swipe. */
    private fun isPagerScroll(event: AccessibilityEvent): Boolean {
        val id = event.source?.viewIdResourceName ?: return false
        return REELS_PAGER_IDS.any { id.contains(it) }
    }

    /** The player is stacked on top of another screen: one Back closes it without leaving Instagram. */
    private fun closeViewer(now: Long) {
        if (!mayAct(now)) return
        performGlobalAction(GLOBAL_ACTION_BACK)
        Prefs.incrementBlocked(this)
    }

    /** Reels tab: switch to the Home tab instead of pressing Back. */
    private fun goHomeTab(now: Long, homeTab: AccessibilityNodeInfo?) {
        if (!mayAct(now)) return
        val clicked = homeTab?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        log(if (clicked) R.string.log_reels_tab_home else R.string.log_reels_tab_failed)
        if (clicked) Prefs.incrementBlocked(this)
    }

    /** Cooldown between actions + circuit breaker: too many actions in a row means a loop, so pause. */
    private fun mayAct(now: Long): Boolean {
        if (now - lastActionAt < ACTION_COOLDOWN_MS) return false
        while (recentActions.isNotEmpty() && now - recentActions.first() > LOOP_WINDOW_MS) recentActions.removeFirst()
        if (recentActions.size >= LOOP_MAX_ACTIONS) {
            pausedUntil = now + LOOP_PAUSE_MS
            recentActions.clear()
            log(R.string.log_loop_pause)
            return false
        }
        recentActions.addLast(now)
        lastActionAt = now
        return true
    }

    /** Log lines are stored already translated, in the language active when they happened. */
    private fun log(msgRes: Int) {
        Log.i(TAG, getString(msgRes))
        Prefs.logEvent(this, msgRes)
    }

    private fun clock() = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())

    /**
     * Only what is ON TOP matters: Instagram keeps visited tabs alive behind the current screen,
     * and Android reports them as "visible" even when covered.
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
     * Hit test: walks down the tree, following at each level the child drawn last (so on top)
     * that contains the point. Returns the ids met along that path.
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

    /** The active window, or else the Instagram window among those on screen. */
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
        Prefs.setDiag(this, "events", "${clock()} $diagEvents events received, last from $pkg")
    }

    /** When there are no ids at all: class names, descriptions and texts of the first nodes. */
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

    /** Breadth-first walk of the view tree, bounded to stay cheap. */
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
            // Instagram keeps visited tabs in memory, hidden: skip everything that is not on screen.
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
        /** Debug dump key for screens that expose no view ids at all. */
        const val NO_ID = "NO_ID"
        private const val IG_PACKAGE = "com.instagram.android"
        private const val IG_ID_PREFIX = "com.instagram.android:id/"

        /** Id fragments found in the fullscreen Reels player. */
        val REELS_VIEWER_IDS = listOf(
            "clips_viewer_view_pager",
            "clips_viewer_container",
            "clips_video_container",
            "root_clips_layout",
        )
        val REELS_PAGER_IDS = listOf("clips_viewer_view_pager")

        /** Id fragments found in a direct message thread. */
        val DM_IDS = listOf(
            "direct_thread",
            "row_thread_composer",
            "thread_title",
            "direct_message_list",
        )

        /** Bottom tab bar (exact ids). */
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

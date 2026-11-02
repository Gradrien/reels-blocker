package fr.adrien.reelsblocker

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Écran unique, construit en code (aucune dépendance).
 * Direction artistique reprise d'Eido : crème / brun, Outfit, cartes très arrondies.
 */
class MainActivity : Activity() {

    // ── typo & couleurs ──
    private val regular by lazy { resources.getFont(R.font.outfit_regular) }
    private val medium by lazy { resources.getFont(R.font.outfit_medium) }
    private val semibold by lazy { resources.getFont(R.font.outfit_semibold) }
    private fun c(id: Int) = getColor(id)

    // ── vues mises à jour à chaque refresh ──
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var counter: TextView
    private lateinit var counterDelta: TextView
    private lateinit var setupCard: View
    private lateinit var settingsBlock: View
    private lateinit var heroCard: View
    private lateinit var logBlock: View
    private lateinit var blockSwitch: ToggleView
    private lateinit var logList: LinearLayout
    private lateinit var advancedBody: LinearLayout
    private lateinit var advancedChevron: ImageView
    private lateinit var debugBlock: LinearLayout
    private lateinit var debugText: TextView

    private var displayedCount = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goEdgeToEdge()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(24))
        }

        col.addView(header())
        // Service coupé : l'activation passe avant tout le reste.
        setupCard = setupCard().also { col.addView(it, lp(top = 8)) }
        heroCard = heroCard().also { col.addView(it, lp(top = 8)) }

        settingsBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(sectionLabel("Blocage"))
            addView(settingsCard())
        }
        col.addView(settingsBlock)

        logList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = card()
            setPadding(0, dp(8), 0, dp(8))
        }
        logBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(sectionLabel("Activité récente"))
            addView(logList)
        }
        col.addView(logBlock)

        col.addView(advancedCard(), lp(top = 32))

        col.addView(text("Rien ne quitte ton téléphone · v${appVersion()}", 13f, regular, R.color.ink_soft).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        })

        col.layoutTransition = LayoutTransition()
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(col)
        }
        // Voile de la couleur du fond sous la barre d'état : le contenu défile dessous sans s'y superposer.
        val scrim = View(this).apply { setBackgroundColor(c(R.color.bg)) }
        val root = FrameLayout(this).apply {
            addView(scroll)
            addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))
        }
        root.setOnApplyWindowInsetsListener { _, insets ->
            val (l, t, r, b) = systemBars(insets).toList()
            // Les insets vont sur la colonne : le contenu passe sous la barre de gestes au lieu d'y être coupé.
            col.setPadding(dp(16) + l, t, dp(16) + r, dp(24) + b)
            scrim.layoutParams = scrim.layoutParams.apply { height = t }
            insets
        }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    // ────────────────────────────── sections ──────────────────────────────

    private fun header() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), dp(16), dp(8), dp(8))
        minimumHeight = dp(64)
        addView(ImageView(context).apply { setImageResource(R.drawable.ic_mark) }, LinearLayout.LayoutParams(dp(32), dp(32)))
        addView(text("Reels Blocker", 22f, medium, R.color.ink).apply { setPadding(dp(12), 0, 0, 0) })
    }

    private fun heroCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = card()
        setPadding(dp(24), dp(20), dp(24), dp(24))

        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            statusDot = View(context).also { addView(it, LinearLayout.LayoutParams(dp(8), dp(8))) }
            statusText = text("", 14f, medium, R.color.ink_soft).apply { setPadding(dp(8), 0, 0, 0) }
            addView(statusText)
        })
        counter = text("0", 72f, semibold, R.color.ink).apply {
            fontFeatureSettings = "tnum"
            letterSpacing = -0.03f
            includeFontPadding = false
            setPadding(0, dp(20), 0, 0)
        }
        addView(counter)
        addView(text("Reels évités", 16f, regular, R.color.ink_soft).apply { setPadding(0, dp(4), 0, 0) })
        counterDelta = text("", 14f, medium, R.color.ink_soft).apply { setPadding(0, dp(12), 0, 0) }
        addView(counterDelta)
    }

    private fun setupCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(c(R.color.warn_bg), 28)
        setPadding(dp(24), dp(20), dp(24), dp(12))
        addView(text("Active la protection", 18f, medium, R.color.ink))
        addView(text("Deux réglages Android, à faire une seule fois.", 14f, regular, R.color.ink_soft).apply {
            setPadding(0, dp(4), 0, dp(8))
        })
        addView(stepRow("1", "Accessibilité", "Active « Reels Blocker » dans la liste") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        addView(stepRow("2", "Autonomie (Xiaomi)", "Démarrage auto + batterie « Aucune restriction »") {
            openAppSettings()
        })
    }

    private fun settingsCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = card()
        blockSwitch = switchRow(this, "Bloquer les Reels", "Onglet Reels, fil d'actu et Explorer", Prefs.enabled(context)) {
            Prefs.setEnabled(context, it); refresh()
        }
        addView(divider())
        switchRow(this, "Bloquer le swipe depuis un DM", "Le Reel envoyé par un ami reste visible", Prefs.blockDmSwipe(context)) {
            Prefs.setBlockDmSwipe(context, it)
        }
    }

    private fun advancedCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = card()

        val head = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            setPadding(dp(24), 0, dp(20), 0)
            background = ripple(28)
            addView(text("Avancé", 16f, medium, R.color.ink), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            advancedChevron = ImageView(context).apply { setImageResource(R.drawable.ic_chevron) }
            addView(advancedChevron, LinearLayout.LayoutParams(dp(24), dp(24)))
            setOnClickListener { toggleAdvanced() }
            contentDescription = "Avancé, afficher ou masquer"
        }
        addView(head)

        layoutTransition = LayoutTransition()
        advancedBody = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(divider())
            addView(linkRow("Accessibilité", "Activer ou couper le service") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
            addView(divider())
            addView(linkRow("Réglages de l'app", "Démarrage auto, batterie, paramètres restreints") { openAppSettings() })
            addView(divider())
            switchRow(this, "Mode diagnostic", "Relève ce que l'app voit dans Instagram", Prefs.debug(context)) {
                Prefs.setDebug(context, it); refresh()
            }
            debugBlock = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), 0, dp(16), dp(16))
                addView(pillButton("Copier le relevé") {
                    getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("reels-blocker", debugText.text))
                    // Android 13+ affiche déjà sa propre confirmation de copie.
                    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Relevé copié", Toast.LENGTH_SHORT).show()
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = dp(8); bottomMargin = dp(12)
                })
                debugText = text("", 12f, Typeface.MONOSPACE, R.color.ink_soft).apply {
                    setTextIsSelectable(true)
                    background = rounded(c(R.color.surface_high), 16)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                }
                addView(debugText)
            }
            addView(debugBlock)
        }
        addView(advancedBody)
    }

    // ────────────────────────────── état ──────────────────────────────

    private fun refresh() {
        val service = isServiceEnabled()
        val on = Prefs.enabled(this)
        val (label, color) = when {
            !service -> "Protection coupée" to R.color.warn
            !on -> "En pause" to R.color.ink_mute
            else -> "Protection active" to R.color.ok
        }
        statusText.text = label
        statusDot.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c(color)) }

        setupCard.visibility = if (service) View.GONE else View.VISIBLE
        // Tant que le service est coupé, seule l'activation compte : le reste s'efface et devient inactif.
        listOf(settingsBlock, logBlock).forEach { setEnabledDeep(it, service); it.alpha = if (service) 1f else 0.45f }
        blockSwitch.isChecked = on

        animateCounter(Prefs.blockedCount(this))
        if (!service) counterDelta.visibility = View.GONE
        renderLog()

        debugBlock.visibility = if (Prefs.debug(this)) View.VISIBLE else View.GONE
        if (Prefs.debug(this)) debugText.text = debugDump()
    }

    /** Signature : le compteur monte depuis la valeur vue à la dernière visite. */
    private fun animateCounter(count: Int) {
        val lastSeen = Prefs.lastSeenCount(this)
        val delta = count - lastSeen
        counterDelta.text = if (delta > 0) "+$delta depuis ta dernière visite" else ""
        counterDelta.visibility = if (delta > 0) View.VISIBLE else View.GONE
        Prefs.setLastSeenCount(this, count)

        val from = if (displayedCount >= 0) displayedCount else lastSeen.coerceAtMost(count)
        displayedCount = count
        if (from == count) { counter.text = count.toString(); return }
        ValueAnimator.ofInt(from, count).apply {
            duration = 700
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { counter.text = (it.animatedValue as Int).toString() }
            start()
        }
    }

    private fun renderLog() {
        logList.removeAllViews()
        val entries = Prefs.log(this).lines().filter { it.isNotBlank() }.take(6)
        if (entries.isEmpty()) {
            logList.addView(text("Rien pour l'instant. Chaque Reel bloqué apparaîtra ici.", 14f, regular, R.color.ink_soft).apply {
                setPadding(dp(24), dp(12), dp(24), dp(12))
            })
            return
        }
        val stamps = entries.map(::logStamp)
        // Colonne horaire alignée : largeur de l'étiquette la plus large, mesurée à la taille de police réelle.
        val probe = text("", 14f, regular, R.color.ink_soft).paint
        val column = stamps.maxOf { probe.measureText(it.first) }.toInt() + dp(16)
        stamps.forEach { (time, msg) ->
            logList.addView(LinearLayout(this).apply {
                setPadding(dp(24), dp(10), dp(24), dp(10))
                addView(text(time, 14f, regular, R.color.ink_soft).apply { fontFeatureSettings = "tnum" },
                    LinearLayout.LayoutParams(column, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(text(msg, 14f, regular, R.color.ink),
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            })
        }
    }

    /** « 2026-11-02 14:32 msg » -> heure si aujourd'hui, « hier » ou la date sinon. Ancien format « 14:32:10 msg » accepté. */
    private fun logStamp(line: String): Pair<String, String> {
        val first = line.substringBefore(' ')
        if (first.contains(':')) return first.take(5) to line.substringAfter(' ')
        val rest = line.substringAfter(' ')
        val time = rest.substringBefore(' ').take(5)
        val msg = rest.substringAfter(' ')
        val date = runCatching { java.time.LocalDate.parse(first) }.getOrNull() ?: return time to msg
        val today = java.time.LocalDate.now()
        val label = when (date) {
            today -> time
            today.minusDays(1) -> "hier"
            else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.FRANCE))
        }
        return label to msg
    }

    private fun debugDump(): String {
        val diag = "Service démarré : " + Prefs.diag(this, "connected").ifBlank { "jamais" } + "\n" +
            "Événements : " + Prefs.diag(this, "events").ifBlank { "aucun" } + "\n" +
            "Lecture écran : " + Prefs.diag(this, "root").ifBlank { "jamais tentée" }
        val screens = ReelsBlockerService.Screen.entries.map { it.name } + "SANS_ID"
        return diag + "\n\n" + screens.joinToString("\n\n") { s ->
            "── $s ──\n" + Prefs.debugDump(this, s).ifBlank { "(pas encore vu)" }
        }
    }

    private fun setEnabledDeep(v: View, enabled: Boolean) {
        v.isEnabled = enabled
        if (v is ViewGroup) for (i in 0 until v.childCount) setEnabledDeep(v.getChildAt(i), enabled)
    }

    private fun toggleAdvanced() {
        val open = advancedBody.visibility != View.VISIBLE
        advancedBody.visibility = if (open) View.VISIBLE else View.GONE
        advancedChevron.animate().rotation(if (open) 180f else 0f).setDuration(200).start()
    }

    // ────────────────────────────── composants ──────────────────────────────

    private fun sectionLabel(t: String) = text(t, 14f, medium, R.color.ink_soft).apply {
        setPadding(dp(24), dp(32), dp(24), dp(10))
    }

    private fun switchRow(parent: LinearLayout, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit): ToggleView {
        val sw = ToggleView(this).apply {
            isChecked = checked
            this.onChange = onChange
            contentDescription = title
        }
        parent.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(76)
            setPadding(dp(24), dp(12), dp(20), dp(12))
            background = ripple(0)
            setOnClickListener { sw.toggle() }
            addView(titleSub(title, sub), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(sw, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(16) })
        })
        return sw
    }

    private fun linkRow(title: String, sub: String, onClick: () -> Unit) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(72)
        setPadding(dp(24), dp(12), dp(24), dp(12))
        background = ripple(0)
        setOnClickListener { onClick() }
        addView(titleSub(title, sub), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(ImageView(context).apply { setImageResource(R.drawable.ic_chevron); rotation = -90f },
            LinearLayout.LayoutParams(dp(24), dp(24)))
    }

    private fun stepRow(n: String, title: String, sub: String, onClick: () -> Unit) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(72)
        setPadding(0, dp(8), 0, dp(8))
        addView(text(n, 14f, semibold, R.color.on_ink).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c(R.color.ink)) }
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        addView(titleSub(title, sub).apply { setPadding(dp(14), 0, dp(12), 0) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(pillButton("Ouvrir", onClick))
    }

    private fun titleSub(title: String, sub: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(title, 16f, medium, R.color.ink))
        addView(text(sub, 14f, regular, R.color.ink_soft).apply { setPadding(0, dp(2), 0, 0) })
    }

    private fun pillButton(label: String, onClick: () -> Unit) = text(label, 14f, medium, R.color.on_ink).apply {
        gravity = Gravity.CENTER
        minHeight = dp(40)
        minWidth = dp(88)
        setPadding(dp(18), 0, dp(18), 0)
        background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), rounded(c(R.color.ink), 20), null)
        isClickable = true
        setOnClickListener { onClick() }
    }.let { btn ->
        // cible tactile de 48 dp autour d'un bouton visuel de 40 dp
        FrameLayout(this).apply {
            setPadding(0, dp(4), 0, dp(4))
            addView(btn)
        }
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(c(R.color.line))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply {
            leftMargin = dp(24); rightMargin = dp(24)
        }
    }

    private fun text(t: String, sp: Float, face: Typeface, color: Int) = TextView(this).apply {
        text = t
        textSize = sp
        typeface = face
        setTextColor(c(color))
        ellipsize = TextUtils.TruncateAt.END
    }

    // ── drawables ──
    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radiusDp).toFloat()
    }

    private fun card() = rounded(c(R.color.surface), 28)

    private fun ripple(radiusDp: Int): Drawable =
        RippleDrawable(ColorStateList.valueOf(c(R.color.line)), null, rounded(0xFF000000.toInt(), radiusDp))

    // ── système ──
    private fun goEdgeToEdge() {
        window.setBackgroundDrawable(ColorDrawable(c(R.color.bg)))
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                (window.decorView.systemUiVisibility and
                    (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR))
        }
    }

    private fun systemBars(insets: WindowInsets): IntArray =
        if (Build.VERSION.SDK_INT >= 30) {
            val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            intArrayOf(i.left, i.top, i.right, i.bottom)
        } else {
            @Suppress("DEPRECATION")
            intArrayOf(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }

    private fun openAppSettings() =
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    private fun isServiceEnabled(): Boolean {
        val me = ComponentName(this, ReelsBlockerService::class.java)
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        // Android stocke la forme longue ou courte (« pkg/.Classe ») selon les versions : on compare les composants.
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun appVersion() = packageManager.getPackageInfo(packageName, 0).versionName

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = dp(top) }
}

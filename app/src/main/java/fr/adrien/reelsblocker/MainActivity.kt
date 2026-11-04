package fr.adrien.reelsblocker

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
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
 * The app's single screen, built in code (no dependencies, no XML layouts).
 * Visual tokens come from resources (colors); copy comes from strings.xml. See DESIGN.md.
 */
class MainActivity : Activity() {

    // ── type & colors ──
    // System sans-serif (Roboto on most phones): zero bytes in the APK, matches the platform.
    private val regular: Typeface = Typeface.create(Typeface.SANS_SERIF, 400, false)
    private val medium: Typeface = Typeface.create(Typeface.SANS_SERIF, 500, false)
    private val bold: Typeface = Typeface.create(Typeface.SANS_SERIF, 700, false)
    private fun c(id: Int) = getColor(id)

    // ── views updated on every refresh ──
    private lateinit var statusChip: LinearLayout
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var counter: GradientTextView
    private lateinit var counterDelta: TextView
    private lateinit var pauseButton: HoldButton
    private lateinit var resumeButton: View
    private lateinit var setupCard: View
    private lateinit var settingsBlock: View
    private lateinit var logBlock: View
    private lateinit var blockSwitch: ToggleView
    private lateinit var logList: LinearLayout
    private lateinit var advancedBody: LinearLayout
    private lateinit var advancedChevron: ImageView
    private lateinit var debugBlock: LinearLayout
    private lateinit var debugText: TextView

    private var displayedCount = -1
    private val refreshRunnable = Runnable { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Prefs.onboarded(this)) {
            // People upgrading from a version without onboarding are already set up: don't send them through it.
            if (Protection.isServiceEnabled(this)) {
                Prefs.setOnboarded(this, true)
            } else {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
                return
            }
        }
        goEdgeToEdge()

        // Full-bleed column: rows own their 16dp gutter so ripples and separators reach the edges.
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        col.addView(header())
        col.addView(hero())
        // When the service is off, the setup card sits right under the counter, before the settings.
        setupCard = setupCard().also {
            col.addView(it, lp(top = 0).apply { leftMargin = dp(GUTTER); rightMargin = dp(GUTTER); bottomMargin = dp(24) })
        }

        settingsBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(separator())
            addView(sectionTitle(getString(R.string.section_blocking)))
            addView(settingsRows())
        }
        col.addView(settingsBlock)

        logList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        logBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(separator())
            addView(sectionTitle(getString(R.string.section_activity)))
            addView(logList)
        }
        col.addView(logBlock)

        col.addView(separator())
        col.addView(advancedSection())

        col.addView(text(getString(R.string.footer, appVersion()), 12f, regular, R.color.ink_soft).apply {
            gravity = Gravity.CENTER
            setPadding(dp(GUTTER), dp(24), dp(GUTTER), 0)
        })

        col.layoutTransition = LayoutTransition()
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(col)
        }
        // Background-colored scrim under the status bar: content scrolls beneath it instead of overlapping it.
        val scrim = View(this).apply { setBackgroundColor(c(R.color.bg)) }
        val root = FrameLayout(this).apply {
            addView(scroll)
            addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))
        }
        root.setOnApplyWindowInsetsListener { _, insets ->
            val (l, t, r, b) = systemBars(insets).toList()
            // Insets go on the column, so content scrolls under the gesture bar instead of being clipped by it.
            col.setPadding(l, t, r, dp(24) + b)
            scrim.layoutParams = scrim.layoutParams.apply { height = t }
            insets
        }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        Watchdog.schedule(this)
        if (Protection.isServiceEnabled(this)) Watchdog.cancelAlert(this)
        refresh()
    }

    override fun onPause() {
        super.onPause()
        counter.removeCallbacks(refreshRunnable)
    }

    // ────────────────────────────── sections ──────────────────────────────

    /** Top bar: the brand tile and the app name, left-aligned like a social app's top bar. */
    private fun header() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(GUTTER), dp(12), dp(GUTTER), dp(4))
        minimumHeight = dp(56)
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_mark)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(30), dp(30)))
        addView(text(getString(R.string.app_name), 22f, bold, R.color.ink).apply {
            setPadding(dp(10), 0, 0, 0)
            isAccessibilityHeading = true
        })
    }

    /** The focal point, laid out like a profile stat: status chip, big gradient number, label. */
    private fun hero() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(GUTTER), dp(24), dp(GUTTER), dp(28))

        statusChip = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(14), 0)
            minimumHeight = dp(32)
            statusDot = View(context).also { addView(it, LinearLayout.LayoutParams(dp(8), dp(8))) }
            statusText = text("", 13f, medium, R.color.ink).apply { setPadding(dp(8), 0, 0, 0) }
            addView(statusText)
        }
        addView(statusChip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        counter = GradientTextView(context, c(R.color.brand_start), c(R.color.brand_end)).apply {
            text = "0"
            textSize = 72f
            typeface = bold
            gravity = Gravity.CENTER
            fontFeatureSettings = "tnum"
            letterSpacing = -0.03f
            includeFontPadding = false
            setPadding(0, dp(20), 0, 0)
        }
        addView(counter)
        addView(text(getString(R.string.counter_label), 15f, medium, R.color.ink).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        counterDelta = text("", 13f, regular, R.color.ink_soft).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        addView(counterDelta)

        // Pause: hold to confirm (deliberate friction), resume with a plain tap.
        pauseButton = HoldButton(
            context, bold,
            fillColor = c(R.color.surface_high),
            sweepColor = (c(R.color.ink) and 0x00FFFFFF) or 0x26000000,
            textColor = c(R.color.ink),
        ).apply {
            text = getString(R.string.action_hold_to_pause, Protection.PAUSE_MINUTES)
            onConfirm = { Protection.pause(context); refresh() }
        }
        addView(pauseButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(20)
        })
        resumeButton = button(getString(R.string.action_resume), primary = true) { Protection.resume(context); refresh() }
        addView(resumeButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(14)
        })
    }

    private fun setupCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(c(R.color.warn_bg), RADIUS_CARD)
        setPadding(dp(GUTTER), dp(16), dp(GUTTER), dp(8))
        addView(text(getString(R.string.setup_title), 17f, bold, R.color.ink).apply { isAccessibilityHeading = true })
        addView(text(getString(R.string.setup_subtitle), 14f, regular, R.color.ink_soft).apply {
            setPadding(0, dp(4), 0, dp(8))
        })
        addView(stepRow("1", getString(R.string.setup_step_accessibility_title), getString(R.string.setup_step_accessibility_subtitle)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        addView(stepRow("2", getString(R.string.setup_step_battery_title), getString(R.string.setup_step_battery_subtitle)) {
            openAppSettings()
        })
    }

    private fun settingsRows() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        blockSwitch = switchRow(this, getString(R.string.setting_block_reels_title), getString(R.string.setting_block_reels_subtitle), Prefs.enabled(context)) {
            Prefs.setEnabled(context, it); PauseTile.refresh(context); refresh()
        }
        switchRow(this, getString(R.string.setting_block_dm_swipe_title), getString(R.string.setting_block_dm_swipe_subtitle), Prefs.blockDmSwipe(context)) {
            Prefs.setBlockDmSwipe(context, it)
        }
    }

    private fun advancedSection() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        val head = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            setPadding(dp(GUTTER), 0, dp(12), 0)
            background = ripple()
            addView(text(getString(R.string.advanced), 16f, bold, R.color.ink), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            advancedChevron = ImageView(context).apply { setImageResource(R.drawable.ic_chevron) }
            addView(advancedChevron, LinearLayout.LayoutParams(dp(24), dp(24)))
            setOnClickListener { toggleAdvanced() }
            contentDescription = getString(R.string.advanced_toggle_description)
        }
        addView(head)

        layoutTransition = LayoutTransition()
        advancedBody = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(linkRow(getString(R.string.link_setup_title), getString(R.string.link_setup_subtitle)) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
            })
            addView(linkRow(getString(R.string.link_accessibility_title), getString(R.string.link_accessibility_subtitle)) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
            addView(linkRow(getString(R.string.link_app_settings_title), getString(R.string.link_app_settings_subtitle)) { openAppSettings() })
            switchRow(this, getString(R.string.setting_debug_title), getString(R.string.setting_debug_subtitle), Prefs.debug(context)) {
                Prefs.setDebug(context, it); refresh()
            }
            debugBlock = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(GUTTER), 0, dp(GUTTER), dp(16))
                addView(button(getString(R.string.action_copy_report), primary = false) {
                    getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("reels-blocker", debugText.text))
                    // Android 13+ already shows its own copy confirmation.
                    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, R.string.report_copied, Toast.LENGTH_SHORT).show()
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(8)
                })
                debugText = text("", 12f, Typeface.MONOSPACE, R.color.ink_soft).apply {
                    setTextIsSelectable(true)
                    ellipsize = null
                    background = rounded(c(R.color.surface), RADIUS_INSET)
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                }
                addView(debugText)
            }
            addView(debugBlock)
        }
        addView(advancedBody)
    }

    // ────────────────────────────── state ──────────────────────────────

    private fun refresh() {
        val state = Protection.state(this)
        val service = state != Protection.State.SERVICE_OFF
        val (label, color) = when (state) {
            Protection.State.SERVICE_OFF -> getString(R.string.status_off) to R.color.warn
            Protection.State.SWITCHED_OFF -> getString(R.string.status_paused) to R.color.ink_mute
            Protection.State.PAUSED -> getString(R.string.status_paused_until, Protection.pausedUntilLabel(this)) to R.color.ink_mute
            Protection.State.ACTIVE -> getString(R.string.status_active) to R.color.ok
        }
        statusText.text = label
        statusDot.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c(color)) }
        statusChip.background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), c(R.color.line))
        }

        setupCard.visibility = if (service) View.GONE else View.VISIBLE
        // While the service is off, only setup matters: everything else fades and is disabled.
        listOf(settingsBlock, logBlock).forEach { setEnabledDeep(it, service); it.alpha = if (service) 1f else 0.45f }
        blockSwitch.isChecked = Prefs.enabled(this)
        pauseButton.visibility = if (state == Protection.State.ACTIVE) View.VISIBLE else View.GONE
        resumeButton.visibility = if (state == Protection.State.PAUSED) View.VISIBLE else View.GONE
        // Refresh by itself when a pause runs out while the screen is open.
        counter.removeCallbacks(refreshRunnable)
        if (state == Protection.State.PAUSED) {
            counter.postDelayed(refreshRunnable, Prefs.pausedUntil(this) - System.currentTimeMillis() + 500)
        }

        animateCounter(Prefs.blockedCount(this))
        if (!service) counterDelta.visibility = View.GONE
        renderLog()

        debugBlock.visibility = if (Prefs.debug(this)) View.VISIBLE else View.GONE
        if (Prefs.debug(this)) debugText.text = debugDump()
    }

    /** Signature moment: the counter climbs from the value seen on the last visit. */
    private fun animateCounter(count: Int) {
        val lastSeen = Prefs.lastSeenCount(this)
        val delta = count - lastSeen
        counterDelta.text = if (delta > 0) getString(R.string.counter_delta, delta) else ""
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
            logList.addView(text(getString(R.string.log_empty), 14f, regular, R.color.ink_soft).apply {
                setPadding(dp(GUTTER), dp(4), dp(GUTTER), dp(12))
            })
            return
        }
        val stamps = entries.map(::logStamp)
        // Aligned time column: width of the widest label, measured at the actual font size.
        val probe = text("", 14f, regular, R.color.ink_soft).paint
        val column = stamps.maxOf { probe.measureText(it.first) }.toInt() + dp(16)
        stamps.forEach { (time, msg) ->
            logList.addView(LinearLayout(this).apply {
                setPadding(dp(GUTTER), dp(8), dp(GUTTER), dp(8))
                addView(text(time, 14f, regular, R.color.ink_soft).apply { fontFeatureSettings = "tnum" },
                    LinearLayout.LayoutParams(column, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(text(msg, 14f, regular, R.color.ink),
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            })
        }
    }

    /** "2026-11-02 14:32 msg" -> time if today, "yesterday", or the date otherwise. Legacy "14:32:10 msg" lines are accepted. */
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
            today.minusDays(1) -> getString(R.string.log_yesterday)
            else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.getDefault()))
        }
        return label to msg
    }

    private fun debugDump(): String {
        // Diagnostic report stays in English on purpose: it is pasted into GitHub issues.
        val diag = "Service started: " + Prefs.diag(this, "connected").ifBlank { "never" } + "\n" +
            "Events: " + Prefs.diag(this, "events").ifBlank { "none" } + "\n" +
            "Screen read: " + Prefs.diag(this, "root").ifBlank { "never attempted" }
        val screens = ReelsBlockerService.Screen.entries.map { it.name } + ReelsBlockerService.NO_ID
        return diag + "\n\n" + screens.joinToString("\n\n") { s ->
            "── $s ──\n" + Prefs.debugDump(this, s).ifBlank { "(not seen yet)" }
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

    // ────────────────────────────── components ──────────────────────────────

    private fun sectionTitle(t: String) = text(t, 16f, bold, R.color.ink).apply {
        setPadding(dp(GUTTER), dp(20), dp(GUTTER), dp(8))
        isAccessibilityHeading = true
    }

    /** Full-bleed hairline between sections. */
    private fun separator() = View(this).apply {
        setBackgroundColor(c(R.color.line))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
    }

    private fun switchRow(parent: LinearLayout, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit): ToggleView {
        val sw = ToggleView(this).apply {
            isChecked = checked
            this.onChange = onChange
            contentDescription = title
        }
        parent.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            setPadding(dp(GUTTER), dp(10), dp(GUTTER), dp(10))
            background = ripple()
            setOnClickListener { sw.toggle() }
            addView(titleSub(title, sub), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(sw, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(16) })
        })
        return sw
    }

    private fun linkRow(title: String, sub: String, onClick: () -> Unit) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(64)
        setPadding(dp(GUTTER), dp(10), dp(12), dp(10))
        background = ripple()
        setOnClickListener { onClick() }
        addView(titleSub(title, sub), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(ImageView(context).apply { setImageResource(R.drawable.ic_chevron); rotation = -90f },
            LinearLayout.LayoutParams(dp(24), dp(24)))
    }

    private fun stepRow(n: String, title: String, sub: String, onClick: () -> Unit) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(64)
        setPadding(0, dp(6), 0, dp(6))
        addView(text(n, 13f, bold, R.color.ink).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(1), c(R.color.ink))
            }
        }, LinearLayout.LayoutParams(dp(26), dp(26)))
        addView(titleSub(title, sub).apply { setPadding(dp(12), 0, dp(12), 0) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(button(getString(R.string.action_open), primary = true, onClick = onClick))
    }

    private fun titleSub(title: String, sub: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(title, 15f, medium, R.color.ink))
        addView(text(sub, 13f, regular, R.color.ink_soft).apply { setPadding(0, dp(2), 0, 0) })
    }

    /**
     * Compact 8dp-radius button. Primary: accent fill, white label. Secondary: neutral fill.
     * The visual button is 36dp tall inside a 48dp touch frame.
     */
    private fun button(label: String, primary: Boolean, onClick: () -> Unit): View {
        val fill = c(if (primary) R.color.accent else R.color.surface_high)
        val btn = text(label, 14f, bold, if (primary) R.color.on_accent else R.color.ink).apply {
            gravity = Gravity.CENTER
            minHeight = dp(36)
            minWidth = dp(72)
            setPadding(dp(16), 0, dp(16), 0)
            background = RippleDrawable(ColorStateList.valueOf(c(R.color.press)), rounded(fill, RADIUS_BUTTON), null)
            isClickable = true
            setOnClickListener { onClick() }
        }
        return FrameLayout(this).apply {
            setPadding(0, dp(6), 0, dp(6))
            addView(btn, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
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

    private fun ripple(): Drawable =
        RippleDrawable(ColorStateList.valueOf(c(R.color.press)), null, ColorDrawable(0xFF000000.toInt()))

    // ── system ──
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

    private fun appVersion() = packageManager.getPackageInfo(packageName, 0).versionName

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = dp(top) }

    private companion object {
        const val GUTTER = 16
        const val RADIUS_CARD = 16
        const val RADIUS_INSET = 12
        const val RADIUS_BUTTON = 8
    }
}

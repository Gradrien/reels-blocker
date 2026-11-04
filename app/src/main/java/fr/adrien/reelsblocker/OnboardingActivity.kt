package fr.adrien.reelsblocker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * First-run guide: welcome, then one screen per setup step. Each step detects when it is done
 * (on return from Android settings) so the user never has to wonder whether it worked.
 */
class OnboardingActivity : Activity() {

    private enum class Page { WELCOME, ACCESSIBILITY, BATTERY, NOTIFICATIONS }

    private val regular: Typeface = Typeface.create(Typeface.SANS_SERIF, 400, false)
    private val medium: Typeface = Typeface.create(Typeface.SANS_SERIF, 500, false)
    private val bold: Typeface = Typeface.create(Typeface.SANS_SERIF, 700, false)

    private val pages: List<Page> = buildList {
        add(Page.WELCOME); add(Page.ACCESSIBILITY); add(Page.BATTERY)
        if (Build.VERSION.SDK_INT >= 33) add(Page.NOTIFICATIONS)
    }
    private var index = 0
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        index = savedInstanceState?.getInt("page") ?: 0
        window.setBackgroundDrawable(ColorDrawable(getColor(R.color.bg)))
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)

        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
        scroll.setOnApplyWindowInsetsListener { _, insets ->
            val (l, t, r, b) = bars(insets)
            content.setPadding(dp(24) + l, dp(16) + t, dp(24) + r, dp(24) + b)
            insets
        }
        setContentView(scroll)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", index)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (index > 0) { index--; render() } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        finishOnboarding()
    }

    // ────────────────────────────── pages ──────────────────────────────

    private fun render() {
        content.removeAllViews()
        val page = pages[index]

        // Top row: step counter on the left, "Skip" on the right.
        content.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(48)
            val step = if (page == Page.WELCOME) "" else getString(R.string.onb_step, index, pages.size - 1)
            addView(text(step, 13f, medium, R.color.ink_soft), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(textButton(getString(R.string.onb_skip)) { finishOnboarding() })
        })

        when (page) {
            Page.WELCOME -> welcome()
            Page.ACCESSIBILITY -> accessibility()
            Page.BATTERY -> battery()
            Page.NOTIFICATIONS -> notifications()
        }
    }

    private fun welcome() {
        content.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_mark)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(64), dp(64)).apply { topMargin = dp(32) })
        content.addView(title(getString(R.string.onb_welcome_title)))
        listOf(R.string.onb_welcome_b1, R.string.onb_welcome_b2, R.string.onb_welcome_b3, R.string.onb_welcome_b4).forEach {
            content.addView(bullet(getString(it)))
        }
        spacer()
        content.addView(primary(getString(R.string.onb_start)) { next() })
    }

    private fun accessibility() {
        val done = Protection.isServiceEnabled(this)
        content.addView(title(getString(R.string.onb_a11y_title)))
        content.addView(body(getString(R.string.onb_a11y_body)))
        content.addView(body(getString(R.string.onb_a11y_how)))
        if (Build.VERSION.SDK_INT >= 33 && !done) {
            content.addView(note(getString(R.string.onb_a11y_restricted)))
            content.addView(textButton(getString(R.string.onb_open_app_info)) { openAppInfo() },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        if (done) content.addView(doneBadge())
        spacer()
        if (done) {
            content.addView(primary(getString(R.string.onb_next)) { next() })
        } else {
            content.addView(primary(getString(R.string.onb_a11y_action)) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
            content.addView(secondary(getString(R.string.onb_later)) { next() })
        }
    }

    private fun battery() {
        val done = getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true
        content.addView(title(getString(R.string.onb_battery_title)))
        content.addView(body(getString(R.string.onb_battery_body)))
        if (done) content.addView(doneBadge())
        spacer()
        if (done) {
            content.addView(primary(getString(R.string.onb_next)) { next() })
        } else {
            content.addView(primary(getString(R.string.onb_open_app_info)) { openAppInfo() })
            content.addView(secondary(getString(R.string.onb_next)) { next() })
        }
    }

    private fun notifications() {
        content.addView(title(getString(R.string.onb_notif_title)))
        content.addView(body(getString(R.string.onb_notif_body)))
        spacer()
        content.addView(primary(getString(R.string.onb_notif_action)) {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            else finishOnboarding()
        })
        content.addView(secondary(getString(R.string.onb_not_now)) { finishOnboarding() })
    }

    private fun next() {
        if (index < pages.lastIndex) { index++; render() } else finishOnboarding()
    }

    private fun finishOnboarding() {
        if (isFinishing) return
        Prefs.setOnboarded(this, true)
        Watchdog.schedule(this)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun openAppInfo() =
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    // ────────────────────────────── components ──────────────────────────────

    private fun title(t: String) = text(t, 26f, bold, R.color.ink).apply {
        setPadding(0, dp(24), 0, dp(12))
        isAccessibilityHeading = true
    }

    private fun body(t: String) = text(t, 16f, regular, R.color.ink).apply {
        setPadding(0, 0, 0, dp(12))
        setLineSpacing(0f, 1.2f)
    }

    private fun note(t: String) = text(t, 14f, regular, R.color.ink_soft).apply {
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setLineSpacing(0f, 1.15f)
        background = GradientDrawable().apply { setColor(getColor(R.color.surface)); cornerRadius = dp(12).toFloat() }
    }

    private fun bullet(t: String) = LinearLayout(this).apply {
        setPadding(0, dp(8), 0, dp(8))
        addView(View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(getColor(R.color.brand_end))
            }
        }, LinearLayout.LayoutParams(dp(6), dp(6)).apply { topMargin = dp(9); rightMargin = dp(14) })
        addView(text(t, 16f, regular, R.color.ink).apply { setLineSpacing(0f, 1.15f) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun doneBadge() = text("✓  " + getString(R.string.onb_done), 15f, bold, R.color.ok).apply {
        setPadding(0, dp(8), 0, 0)
    }

    /** Pushes the buttons to the bottom of the screen on tall phones; collapses on short ones. */
    private fun spacer() = content.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f).apply { topMargin = dp(24) })

    private fun primary(label: String, onClick: () -> Unit) = text(label, 16f, bold, R.color.on_accent).apply {
        gravity = Gravity.CENTER
        minHeight = dp(52)
        background = RippleDrawable(ColorStateList.valueOf(getColor(R.color.press)),
            GradientDrawable().apply { setColor(getColor(R.color.accent)); cornerRadius = dp(10).toFloat() }, null)
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun secondary(label: String, onClick: () -> Unit) = text(label, 16f, bold, R.color.ink).apply {
        gravity = Gravity.CENTER
        minHeight = dp(52)
        background = RippleDrawable(ColorStateList.valueOf(getColor(R.color.press)), null, ColorDrawable(0xFF000000.toInt()))
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun textButton(label: String, onClick: () -> Unit) = text(label, 15f, bold, R.color.accent).apply {
        gravity = Gravity.CENTER
        minHeight = dp(48)
        setPadding(dp(8), 0, dp(8), 0)
        background = RippleDrawable(ColorStateList.valueOf(getColor(R.color.press)), null, ColorDrawable(0xFF000000.toInt()))
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun text(t: String, sp: Float, face: Typeface, color: Int) = TextView(this).apply {
        text = t
        textSize = sp
        typeface = face
        setTextColor(getColor(color))
    }

    private fun bars(insets: WindowInsets): List<Int> =
        if (Build.VERSION.SDK_INT >= 30) {
            val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            listOf(i.left, i.top, i.right, i.bottom)
        } else {
            @Suppress("DEPRECATION")
            listOf(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

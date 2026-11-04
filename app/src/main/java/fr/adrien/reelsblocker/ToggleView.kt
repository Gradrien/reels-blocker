package fr.adrien.reelsblocker

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.widget.Checkable
import android.widget.Switch

/**
 * Compact switch in the style of social apps' settings: a 44×26dp track, solid ink when on,
 * neutral grey when off, with a 22dp thumb that slides across. Drawn by hand because the
 * framework Switch cannot be resized reliably. Announced to TalkBack as a standard Switch.
 */
class ToggleView(context: Context) : View(context), Checkable {

    var onChange: ((Boolean) -> Unit)? = null
    private var checked = false
    private var progress = 0f // 0 = off, 1 = on
    private var anim: ValueAnimator? = null

    private val d = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener { toggle() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension((TRACK_W * d).toInt(), (48 * d).toInt()) // 48dp tall: touch target

    override fun onDraw(canvas: Canvas) {
        val h = TRACK_H * d
        val top = (height - h) / 2
        rect.set(0f, top, width.toFloat(), top + h)
        val r = h / 2

        track.color = blend(context.getColor(R.color.switch_off), context.getColor(R.color.ink), progress)
        canvas.drawRoundRect(rect, r, r, track)

        val inset = (TRACK_H - THUMB) / 2 * d
        val radius = THUMB / 2 * d
        val cx = inset + radius + (width - 2 * (inset + radius)) * progress
        thumb.color = blend(context.getColor(R.color.switch_thumb), context.getColor(R.color.bg), progress)
        canvas.drawCircle(cx, rect.centerY(), radius, thumb)
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = (((a shr shift) and 0xFF) * (1 - t) + ((b shr shift) and 0xFF) * t).toInt() shl shift
        return (0xFF shl 24) or ch(16) or ch(8) or ch(0)
    }

    override fun isChecked() = checked

    override fun setChecked(value: Boolean) = setCheckedInternal(value, animate = false, notify = false)

    override fun toggle() = setCheckedInternal(!checked, animate = true, notify = true)

    private fun setCheckedInternal(value: Boolean, animate: Boolean, notify: Boolean) {
        if (value == checked && (anim == null || !animate)) { progress = if (value) 1f else 0f; invalidate(); return }
        checked = value
        anim?.cancel()
        val target = if (value) 1f else 0f
        if (animate) {
            anim = ValueAnimator.ofFloat(progress, target).apply {
                duration = 160
                interpolator = DecelerateInterpolator()
                addUpdateListener { progress = it.animatedValue as Float; invalidate() }
                start()
            }
        } else {
            progress = target; invalidate()
        }
        if (notify) onChange?.invoke(value)
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = Switch::class.java.name
        info.isCheckable = true
        info.isChecked = checked
    }

    private companion object {
        const val TRACK_W = 44f
        const val TRACK_H = 26f
        const val THUMB = 22f
    }
}

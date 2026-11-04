package fr.adrien.reelsblocker

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView

/**
 * Button that only fires after being held down for [holdMs]: a fill sweeps across while the finger
 * stays down, and releasing early cancels. This is the deliberate friction in front of "pause".
 * For TalkBack and switch access, a regular activation fires it directly (they cannot "hold").
 */
class HoldButton(
    context: Context,
    face: Typeface,
    private val fillColor: Int,
    private val sweepColor: Int,
    textColor: Int,
    private val holdMs: Long = 1500L,
) : TextView(context) {

    var onConfirm: (() -> Unit)? = null

    private val d = resources.displayMetrics.density
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
    private val sweep = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sweepColor }
    private val rect = RectF()
    private var progress = 0f
    private var anim: ValueAnimator? = null

    init {
        typeface = face
        textSize = 14f
        setTextColor(textColor)
        gravity = Gravity.CENTER
        minHeight = (40 * d).toInt()
        setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
        isClickable = true
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        val r = 8 * d
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, r, r, bg)
        if (progress > 0f) {
            canvas.save()
            canvas.clipRect(0f, 0f, width * progress, height.toFloat())
            canvas.drawRoundRect(rect, r, r, sweep)
            canvas.restore()
        }
        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { start(); return true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { cancel(); return true }
            MotionEvent.ACTION_MOVE -> {
                if (event.x < 0 || event.y < 0 || event.x > width || event.y > height) cancel()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun start() {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = holdMs
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
                if (progress >= 1f) fire()
            }
            start()
        }
    }

    private fun cancel() {
        if (anim == null) return
        anim?.cancel()
        anim = null
        // Ease the fill back out instead of snapping, so an early release reads as "not yet".
        ValueAnimator.ofFloat(progress, 0f).apply {
            duration = 180
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun fire() {
        anim?.removeAllUpdateListeners()
        anim = null
        progress = 0f
        invalidate()
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onConfirm?.invoke()
    }

    // Accessibility services click instead of holding: treat that as confirmation.
    override fun performClick(): Boolean {
        super.performClick()
        onConfirm?.invoke()
        return true
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (action == AccessibilityNodeInfo.ACTION_CLICK) return performClick()
        return super.performAccessibilityAction(action, arguments)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = Button::class.java.name
    }
}

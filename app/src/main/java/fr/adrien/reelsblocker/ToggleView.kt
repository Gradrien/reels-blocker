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
 * Interrupteur façon Material 3 (piste 52×32 dp, pouce 24 dp allumé / 16 dp cerclé éteint),
 * dessiné à la main : le Switch du framework ignore les tailles de ses drawables à l'état éteint.
 * Annoncé comme un Switch standard par TalkBack.
 */
class ToggleView(context: Context) : View(context), Checkable {

    var onChange: ((Boolean) -> Unit)? = null
    private var checked = false
    private var progress = 0f // 0 = éteint, 1 = allumé
    private var anim: ValueAnimator? = null

    private val d = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2 * d }
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener { toggle() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension((52 * d).toInt(), (48 * d).toInt()) // 48 dp de haut : cible tactile

    override fun onDraw(canvas: Canvas) {
        val top = (height - 32 * d) / 2
        rect.set(0f, top, width.toFloat(), top + 32 * d)
        val r = 16 * d
        val ink = context.getColor(R.color.ink)
        val soft = context.getColor(R.color.ink_soft)

        if (progress < 1f) {
            track.color = context.getColor(R.color.surface_high)
            canvas.drawRoundRect(rect, r, r, track)
            outline.color = soft
            rect.inset(d, d)
            canvas.drawRoundRect(rect, r - d, r - d, outline)
            rect.inset(-d, -d)
        }
        if (progress > 0f) {
            track.color = ink
            track.alpha = (255 * progress).toInt()
            canvas.drawRoundRect(rect, r, r, track)
            track.alpha = 255
        }
        val radius = (8 + 4 * progress) * d
        val cx = 16 * d + (width - 32 * d) * progress
        thumb.color = if (progress > 0.5f) context.getColor(R.color.bg) else soft
        canvas.drawCircle(cx, rect.centerY(), radius, thumb)
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
}

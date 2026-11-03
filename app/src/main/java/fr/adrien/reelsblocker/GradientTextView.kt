package fr.adrien.reelsblocker

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.TextView

/**
 * TextView whose glyphs are filled with the brand gradient, measured on the text itself
 * so a short number still shows both ends of the gradient.
 */
class GradientTextView(context: Context, private val start: Int, private val end: Int) : TextView(context) {

    private var shaderKey = ""

    override fun onDraw(canvas: Canvas) {
        val textWidth = paint.measureText(text.toString())
        val left = when {
            gravity and android.view.Gravity.HORIZONTAL_GRAVITY_MASK == android.view.Gravity.CENTER_HORIZONTAL ->
                (width - textWidth) / 2f
            else -> paddingLeft.toFloat()
        }
        val key = "$left:$textWidth"
        if (key != shaderKey) {
            shaderKey = key
            paint.shader = LinearGradient(left, 0f, left + textWidth, 0f, start, end, Shader.TileMode.CLAMP)
        }
        super.onDraw(canvas)
    }
}

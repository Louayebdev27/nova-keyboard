package app.novakeys.keyboard

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Small line icons drawn with canvas primitives so they follow the theme colours
 * and need no image files. Everything is drawn in a unit box (-1..1) and scaled.
 */
object KeyIcons {
    private val path = Path()
    private val rect = RectF()

    fun draw(c: Canvas, icon: KeyAction, cx: Float, cy: Float, size: Float, p: Paint, active: Boolean = false, locked: Boolean = false) {
        val r = size / 2f
        c.save()
        c.translate(cx, cy)
        c.scale(r, r)
        val oldStyle = p.style
        val oldWidth = p.strokeWidth
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.14f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        when (icon) {
            KeyAction.SHIFT -> {
                path.reset()
                path.moveTo(0f, -0.95f); path.lineTo(0.95f, 0.05f); path.lineTo(0.5f, 0.05f)
                path.lineTo(0.5f, 0.75f); path.lineTo(-0.5f, 0.75f); path.lineTo(-0.5f, 0.05f)
                path.lineTo(-0.95f, 0.05f); path.close()
                if (active) p.style = Paint.Style.FILL_AND_STROKE
                c.drawPath(path, p)
                if (locked) {
                    p.style = Paint.Style.STROKE
                    c.drawLine(-0.5f, 1.05f, 0.5f, 1.05f, p)
                }
            }
            KeyAction.BACKSPACE -> {
                path.reset()
                path.moveTo(-0.25f, -0.7f); path.lineTo(1f, -0.7f); path.lineTo(1f, 0.7f)
                path.lineTo(-0.25f, 0.7f); path.lineTo(-1f, 0f); path.close()
                c.drawPath(path, p)
                c.drawLine(0.05f, -0.28f, 0.6f, 0.28f, p)
                c.drawLine(0.6f, -0.28f, 0.05f, 0.28f, p)
            }
            KeyAction.ENTER -> {
                path.reset()
                path.moveTo(0.9f, -0.7f); path.lineTo(0.9f, 0.2f); path.lineTo(-0.8f, 0.2f)
                path.moveTo(-0.25f, -0.35f); path.lineTo(-0.8f, 0.2f); path.lineTo(-0.25f, 0.75f)
                c.drawPath(path, p)
            }
            KeyAction.EMOJI -> {
                c.drawCircle(0f, 0f, 0.92f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(-0.33f, -0.25f, 0.1f, p)
                c.drawCircle(0.33f, -0.25f, 0.1f, p)
                p.style = Paint.Style.STROKE
                rect.set(-0.5f, -0.2f, 0.5f, 0.55f)
                c.drawArc(rect, 20f, 140f, false, p)
            }
            KeyAction.VOICE -> {
                rect.set(-0.3f, -0.95f, 0.3f, 0.2f)
                c.drawRoundRect(rect, 0.3f, 0.3f, p)
                rect.set(-0.62f, -0.45f, 0.62f, 0.55f)
                c.drawArc(rect, 0f, 180f, false, p)
                c.drawLine(0f, 0.55f, 0f, 0.95f, p)
                c.drawLine(-0.35f, 0.95f, 0.35f, 0.95f, p)
            }
            KeyAction.LANG -> {
                c.drawCircle(0f, 0f, 0.92f, p)
                rect.set(-0.42f, -0.92f, 0.42f, 0.92f)
                c.drawOval(rect, p)
                c.drawLine(-0.92f, 0f, 0.92f, 0f, p)
            }
            KeyAction.CLIP -> {
                rect.set(-0.65f, -0.72f, 0.65f, 0.95f)
                c.drawRoundRect(rect, 0.16f, 0.16f, p)
                p.style = Paint.Style.FILL
                rect.set(-0.32f, -0.98f, 0.32f, -0.5f)
                c.drawRoundRect(rect, 0.14f, 0.14f, p)
                p.style = Paint.Style.STROKE
                c.drawLine(-0.33f, 0.05f, 0.33f, 0.05f, p)
                c.drawLine(-0.33f, 0.5f, 0.33f, 0.5f, p)
            }
            KeyAction.TOOLS -> {
                val a = 0.08f; val b = 0.85f
                for ((x0, y0) in listOf(-b to -b, a to -b, -b to a, a to a)) {
                    rect.set(x0, y0, x0 + 0.77f, y0 + 0.77f)
                    c.drawRoundRect(rect, 0.2f, 0.2f, p)
                }
            }
            else -> Unit
        }
        p.style = oldStyle
        p.strokeWidth = oldWidth
        c.restore()
    }
}

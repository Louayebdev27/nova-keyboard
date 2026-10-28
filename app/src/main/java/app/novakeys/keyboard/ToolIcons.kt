package app.novakeys.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class ToolIcon { BACK, CLOSE, PIN, BACKSPACE, NUMPAD, CALC, CLIP, STICKER, SELECT, CAMERA, HAND, SETTINGS, MORE }

/**
 * Line icons for the keyboard panels, in the same style as [KeyIcons] so panels and keys look like one
 * family and follow the theme colours. Drawn in a -1..1 box, no image files.
 */
object ToolIcons {
    private val path = Path()
    private val rect = RectF()

    fun draw(c: Canvas, icon: ToolIcon, cx: Float, cy: Float, size: Float, p: Paint, filled: Boolean = false) {
        // Icons that already exist on the keys are reused as they are.
        when (icon) {
            ToolIcon.BACKSPACE -> { KeyIcons.draw(c, KeyAction.BACKSPACE, cx, cy, size, p); return }
            ToolIcon.CLIP -> { KeyIcons.draw(c, KeyAction.CLIP, cx, cy, size, p); return }
            ToolIcon.MORE -> { KeyIcons.draw(c, KeyAction.TOOLS, cx, cy, size, p); return }
            else -> Unit
        }
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
            ToolIcon.BACK -> {
                path.reset()
                path.moveTo(0.4f, -0.8f); path.lineTo(-0.5f, 0f); path.lineTo(0.4f, 0.8f)
                c.drawPath(path, p)
            }
            ToolIcon.CLOSE -> {
                c.drawLine(-0.65f, -0.65f, 0.65f, 0.65f, p)
                c.drawLine(0.65f, -0.65f, -0.65f, 0.65f, p)
            }
            ToolIcon.PIN -> {
                c.drawLine(-0.5f, -0.85f, 0.5f, -0.85f, p)
                path.reset()
                path.moveTo(-0.35f, -0.85f); path.lineTo(-0.35f, -0.1f); path.lineTo(-0.65f, 0.25f)
                path.lineTo(0.65f, 0.25f); path.lineTo(0.35f, -0.1f); path.lineTo(0.35f, -0.85f)
                if (filled) p.style = Paint.Style.FILL_AND_STROKE
                c.drawPath(path, p)
                p.style = Paint.Style.STROKE
                c.drawLine(0f, 0.25f, 0f, 0.95f, p)
            }
            ToolIcon.NUMPAD -> {
                p.style = Paint.Style.FILL
                for (row in -1..1) for (col in -1..1) c.drawCircle(col * 0.62f, row * 0.62f, 0.17f, p)
            }
            ToolIcon.CALC -> {
                rect.set(-0.75f, -0.95f, 0.75f, 0.95f)
                c.drawRoundRect(rect, 0.22f, 0.22f, p)
                rect.set(-0.42f, -0.62f, 0.42f, -0.3f)
                c.drawRoundRect(rect, 0.08f, 0.08f, p)
                p.style = Paint.Style.FILL
                for (row in 0..1) for (col in 0..1) c.drawCircle(-0.3f + col * 0.6f, 0.15f + row * 0.5f, 0.1f, p)
            }
            ToolIcon.STICKER -> {
                rect.set(-0.88f, -0.88f, 0.88f, 0.88f)
                c.drawRoundRect(rect, 0.32f, 0.32f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(-0.3f, -0.2f, 0.1f, p)
                c.drawCircle(0.3f, -0.2f, 0.1f, p)
                p.style = Paint.Style.STROKE
                rect.set(-0.45f, -0.15f, 0.45f, 0.5f)
                c.drawArc(rect, 20f, 140f, false, p)
            }
            ToolIcon.SELECT -> {
                p.pathEffect = DashPathEffect(floatArrayOf(0.32f, 0.26f), 0f)
                rect.set(-0.88f, -0.68f, 0.88f, 0.68f)
                c.drawRoundRect(rect, 0.18f, 0.18f, p)
                p.pathEffect = null
                c.drawLine(0f, -0.32f, 0f, 0.32f, p)
            }
            ToolIcon.CAMERA -> {
                rect.set(-0.9f, -0.45f, 0.9f, 0.78f)
                c.drawRoundRect(rect, 0.22f, 0.22f, p)
                path.reset()
                path.moveTo(-0.38f, -0.45f); path.lineTo(-0.22f, -0.82f); path.lineTo(0.22f, -0.82f); path.lineTo(0.38f, -0.45f)
                c.drawPath(path, p)
                c.drawCircle(0f, 0.17f, 0.34f, p)
            }
            ToolIcon.HAND -> {
                rect.set(-0.9f, -0.9f, 0.05f, 0.9f)
                c.drawRoundRect(rect, 0.22f, 0.22f, p)
                c.drawLine(0.3f, 0f, 0.9f, 0f, p)
                path.reset()
                path.moveTo(0.62f, -0.28f); path.lineTo(0.9f, 0f); path.lineTo(0.62f, 0.28f)
                c.drawPath(path, p)
            }
            ToolIcon.SETTINGS -> {
                c.drawCircle(0f, 0f, 0.5f, p)
                c.drawCircle(0f, 0f, 0.16f, p)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
                    c.drawLine(ca * 0.62f, sa * 0.62f, ca * 0.92f, sa * 0.92f, p)
                }
            }
            else -> Unit
        }
        p.pathEffect = null
        p.style = oldStyle
        p.strokeWidth = oldWidth
        c.restore()
    }
}

/** A plain View that paints one [ToolIcon] centred in its bounds. */
class IconView(context: Context, var icon: ToolIcon, tint: Int, private val scale: Float = 0.5f) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var filled = false
        set(v) { field = v; invalidate() }
    var tint: Int = tint
        set(v) { field = v; invalidate() }

    override fun onDraw(c: Canvas) {
        paint.color = tint
        ToolIcons.draw(c, icon, width / 2f, height / 2f, min(width, height) * scale, paint, filled)
    }
}

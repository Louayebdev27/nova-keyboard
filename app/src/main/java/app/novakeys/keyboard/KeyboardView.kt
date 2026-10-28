package app.novakeys.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import app.novakeys.theme.KeyboardTheme
import app.novakeys.theme.Themes
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a [KeyboardLayout] with a [KeyboardTheme] and turns touches into key events.
 * Supports fast two-finger typing, long-press alternatives, backspace repeat and
 * swiping on the space bar to move the caret.
 *
 * The parent must have clipChildren=false so the key preview can rise above this view.
 */
class KeyboardView(context: Context) : View(context) {

    var listener: KeyboardActionListener? = null
    var feedback: KeyFeedback? = null
    var showPreview = true
    var longPressMs = 350L

    var heightScale = 1f
        set(v) { field = v; requestLayout() }

    /** 0 = off, 1 = on for the next letter, 2 = caps lock. */
    var shiftState = 0
        set(v) { if (field != v) { field = v; invalidate() } }

    private var layout = KeyboardLayout("empty", emptyList())
    private var theme: KeyboardTheme = Themes.default
    private var backgroundBitmap: Bitmap? = null

    private class KeyRect(val key: Key, val vis: RectF, val hit: RectF, val row: Int)

    private class Ptr(var kr: KeyRect, val downX: Float, val downY: Float) {
        var x = downX
        var anchorX = downX
        var committed = false
        var popup = false
        var popupIndex = 0
        var longDone = false
        var cursorMode = false
        var repeatCount = 0
        var runnable: Runnable? = null
    }

    private val keyRects = ArrayList<KeyRect>()
    private val pointers = SparseArray<Ptr>()
    private val handler = Handler(Looper.getMainLooper())

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val rectF = RectF()
    private val srcRect = Rect()
    private val dstRect = Rect()

    // ---- public API -------------------------------------------------------------------------

    fun setLayout(l: KeyboardLayout) {
        cancelAll()
        layout = l
        requestLayout()
        computeKeys(width)
        invalidate()
    }

    fun setTheme(t: KeyboardTheme, background: Bitmap?) {
        theme = t
        backgroundBitmap = background
        textPaint.typeface = Typeface.create(t.fontFamily, if (t.boldText) Typeface.BOLD else Typeface.NORMAL)
        hintPaint.typeface = textPaint.typeface
        requestLayout()
        computeKeys(width)
        invalidate()
    }

    fun cancelAll() {
        for (i in 0 until pointers.size()) pointers.valueAt(i).runnable?.let { handler.removeCallbacks(it) }
        pointers.clear()
        invalidate()
    }

    // ---- geometry ---------------------------------------------------------------------------

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity
    private fun keyHeight() = dp(46f) * heightScale
    private fun vPad() = dp(6f)

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val w = MeasureSpec.getSize(widthSpec)
        val rows = max(layout.rows.size, 1)
        val gap = dp(theme.keyGapDp)
        val h = vPad() * 2 + rows * keyHeight() + (rows - 1) * gap
        setMeasuredDimension(w, h.toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        computeKeys(w)
    }

    private fun computeKeys(w: Int) {
        keyRects.clear()
        if (w <= 0 || layout.rows.isEmpty()) return
        val gap = dp(theme.keyGapDp)
        val side = max(gap, dp(3f))
        val keyH = keyHeight()
        layout.rows.forEachIndexed { ri, row ->
            val n = row.keys.size
            if (n == 0) return@forEachIndexed
            val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat() + 2 * row.inset
            val unit = (w - 2 * side - gap * (n - 1)) / totalWeight
            var x = side + row.inset * unit
            val y = vPad() + ri * (keyH + gap)
            for (k in row.keys) {
                val kw = k.weight * unit
                keyRects += KeyRect(
                    k,
                    RectF(x, y, x + kw, y + keyH),
                    RectF(x - gap / 2, y - gap / 2, x + kw + gap / 2, y + keyH + gap / 2),
                    ri,
                )
                x += kw + gap
            }
        }
    }

    private fun findKey(x: Float, y: Float): KeyRect? {
        if (keyRects.isEmpty()) return null
        val step = keyHeight() + dp(theme.keyGapDp)
        val row = ((y - vPad() + dp(theme.keyGapDp) / 2) / step).toInt().coerceIn(0, layout.rows.size - 1)
        var best: KeyRect? = null
        var bestDist = Float.MAX_VALUE
        for (kr in keyRects) {
            if (kr.row != row) continue
            val d = when {
                x < kr.hit.left -> kr.hit.left - x
                x > kr.hit.right -> x - kr.hit.right
                else -> 0f
            }
            if (d < bestDist) { bestDist = d; best = kr }
        }
        return best
    }

    // ---- drawing ----------------------------------------------------------------------------

    private fun withAlpha(color: Int, a: Float): Int =
        (color and 0x00FFFFFF) or ((Color.alpha(color) * a).toInt().coerceIn(0, 255) shl 24)

    private fun shade(c: Int, f: Float): Int =
        Color.argb(Color.alpha(c), (Color.red(c) * f).toInt(), (Color.green(c) * f).toInt(), (Color.blue(c) * f).toInt())

    private fun caseMap(s: String): String =
        if (shiftState != 0 && layout.shiftable) (if (s == "ß") "ẞ" else s.uppercase(Locale.ROOT)) else s

    override fun onDraw(c: Canvas) {
        c.drawColor(theme.background)
        backgroundBitmap?.let { bmp -> drawBackgroundImage(c, bmp) }

        val pressedKeys = HashSet<KeyRect>()
        for (i in 0 until pointers.size()) pressedKeys += pointers.valueAt(i).kr
        for (kr in keyRects) drawKey(c, kr, kr in pressedKeys)
        for (i in 0 until pointers.size()) drawOverlay(c, pointers.valueAt(i))
    }

    private fun drawBackgroundImage(c: Canvas, bmp: Bitmap) {
        // centre-crop to the view
        val vw = width.toFloat(); val vh = height.toFloat()
        val scale = max(vw / bmp.width, vh / bmp.height)
        val sw = (vw / scale).toInt(); val sh = (vh / scale).toInt()
        val sx = (bmp.width - sw) / 2; val sy = (bmp.height - sh) / 2
        srcRect.set(sx, sy, sx + sw, sy + sh)
        dstRect.set(0, 0, width, height)
        c.drawBitmap(bmp, srcRect, dstRect, bgPaint)
        if (theme.backgroundDim > 0f) {
            fillPaint.color = Color.argb((theme.backgroundDim * 255).toInt().coerceIn(0, 255), 0, 0, 0)
            c.drawRect(0f, 0f, vw, vh, fillPaint)
        }
    }

    private fun drawKey(c: Canvas, kr: KeyRect, pressed: Boolean) {
        val key = kr.key
        val r = dp(theme.keyRadiusDp)
        val shiftOn = key.action == KeyAction.SHIFT && shiftState != 0
        // Enter, "=" and an active Shift all light up in the accent colour.
        val accent = key.action == KeyAction.ENTER || key.action == KeyAction.EQUALS || shiftOn
        val base = when {
            accent -> theme.accentColor
            key.isSpecial && key.action != KeyAction.SPACE -> theme.specialKeyColor
            else -> theme.keyColor
        }
        val fill = when {
            !pressed -> base
            accent -> shade(base, 0.82f)
            else -> theme.keyPressedColor
        }
        if (theme.keyShadow && !pressed) {
            fillPaint.color = 0x33000000
            rectF.set(kr.vis.left, kr.vis.top + dp(1.2f), kr.vis.right, kr.vis.bottom + dp(1.2f))
            c.drawRoundRect(rectF, r, r, fillPaint)
        }
        fillPaint.color = withAlpha(fill, theme.keyAlpha)
        c.drawRoundRect(kr.vis, r, r, fillPaint)
        if (theme.borderWidthDp > 0f) {
            strokePaint.color = theme.borderColor
            strokePaint.strokeWidth = dp(theme.borderWidthDp)
            c.drawRoundRect(kr.vis, r, r, strokePaint)
        }

        val cx = kr.vis.centerX()
        val cy = kr.vis.centerY()
        val ink = when {
            accent -> theme.accentTextColor
            key.isSpecial -> theme.specialTextColor
            else -> theme.textColor
        }
        val iconSize = min(kr.vis.height() * 0.42f, kr.vis.width() * 0.6f)
        iconPaint.color = ink

        when (key.action) {
            KeyAction.SHIFT -> {
                KeyIcons.draw(c, KeyAction.SHIFT, cx, cy, iconSize, iconPaint, active = shiftState != 0, locked = shiftState == 2)
            }
            KeyAction.BACKSPACE, KeyAction.EMOJI, KeyAction.VOICE, KeyAction.LANG, KeyAction.CLIP, KeyAction.TOOLS ->
                KeyIcons.draw(c, key.action, cx, cy, iconSize, iconPaint)
            KeyAction.ENTER ->
                if (key.label == "enter") KeyIcons.draw(c, KeyAction.ENTER, cx, cy, iconSize, iconPaint)
                else drawText(c, key.label, cx, cy, sp(14f), ink, bold = true)
            KeyAction.SPACE ->
                if (key.label.isNotEmpty()) drawText(c, key.label, cx, cy, sp(12f), theme.hintColor)
            KeyAction.CHAR -> {
                val label = caseMap(key.label)
                val size = if (label.codePointCount(0, label.length) > 1) sp(15f) else sp(theme.textSizeSp)
                drawText(c, label, cx, cy, size, ink)
                key.hint?.let {
                    hintPaint.color = theme.hintColor
                    hintPaint.textSize = sp(10f)
                    c.drawText(it, kr.vis.right - dp(5f), kr.vis.top + dp(13f), hintPaint)
                }
            }
            else -> drawText(c, key.label, cx, cy, sp(15f), ink)
        }
    }

    private fun drawText(c: Canvas, text: String, cx: Float, cy: Float, size: Float, color: Int, bold: Boolean = false) {
        textPaint.color = color
        textPaint.textSize = size
        textPaint.isFakeBoldText = bold
        val y = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        c.drawText(text, cx, y, textPaint)
        textPaint.isFakeBoldText = false
    }

    private fun popupOptions(p: Ptr): List<String> = p.kr.key.popups.map { caseMap(it) }

    private val popupItemWidth get() = dp(40f)

    private fun popupLeft(p: Ptr): Float {
        val total = popupOptions(p).size * popupItemWidth
        return (p.kr.vis.centerX() - total / 2f).coerceIn(dp(4f), max(dp(4f), width - total - dp(4f)))
    }

    private fun popupIndexFor(p: Ptr, x: Float): Int {
        val n = popupOptions(p).size
        if (n == 0) return 0
        return ((x - popupLeft(p)) / popupItemWidth).toInt().coerceIn(0, n - 1)
    }

    private fun drawOverlay(c: Canvas, p: Ptr) {
        val kr = p.kr
        if (p.popup) {
            val opts = popupOptions(p)
            val itemH = keyHeight() * 1.1f
            val left = popupLeft(p)
            val top = kr.vis.top - itemH - dp(8f)
            val r = dp(max(theme.keyRadiusDp, 10f))
            fillPaint.color = 0x55000000
            rectF.set(left - dp(2f), top + dp(2f), left + opts.size * popupItemWidth + dp(2f), top + itemH + dp(4f))
            c.drawRoundRect(rectF, r, r, fillPaint)
            fillPaint.color = theme.specialKeyColor or (0xFF shl 24)
            rectF.set(left, top, left + opts.size * popupItemWidth, top + itemH)
            c.drawRoundRect(rectF, r, r, fillPaint)
            opts.forEachIndexed { i, s ->
                val sel = i == p.popupIndex
                if (sel) {
                    fillPaint.color = theme.accentColor
                    rectF.set(left + i * popupItemWidth + dp(3f), top + dp(3f), left + (i + 1) * popupItemWidth - dp(3f), top + itemH - dp(3f))
                    c.drawRoundRect(rectF, r * 0.8f, r * 0.8f, fillPaint)
                }
                drawText(c, s, left + (i + 0.5f) * popupItemWidth, top + itemH / 2f, sp(22f),
                    if (sel) theme.accentTextColor else theme.textColor)
            }
        } else if (showPreview && kr.key.action == KeyAction.CHAR && !p.committed) {
            val bw = max(kr.vis.width() * 1.3f, dp(48f))
            val bh = kr.vis.height() * 1.25f
            val left = (kr.vis.centerX() - bw / 2f).coerceIn(dp(2f), max(dp(2f), width - bw - dp(2f)))
            val top = kr.vis.top - bh - dp(4f)
            val r = dp(max(theme.keyRadiusDp, 10f))
            fillPaint.color = 0x55000000
            rectF.set(left, top + dp(2f), left + bw, top + bh + dp(2f))
            c.drawRoundRect(rectF, r, r, fillPaint)
            fillPaint.color = theme.keyPressedColor or (0xFF shl 24)
            rectF.set(left, top, left + bw, top + bh)
            c.drawRoundRect(rectF, r, r, fillPaint)
            drawText(c, caseMap(kr.key.label), left + bw / 2f, top + bh / 2f, sp(theme.textSizeSp * 1.5f), theme.textColor)
        }
    }

    // ---- touch ------------------------------------------------------------------------------

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                down(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until e.pointerCount) move(e.getPointerId(i), e.getX(i), e.getY(i))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> up(e.getPointerId(e.actionIndex))
            MotionEvent.ACTION_CANCEL -> cancelAll()
        }
        return true
    }

    private fun commit(key: Key) {
        when (key.action) {
            KeyAction.CHAR -> listener?.onText(caseMap(key.output))
            KeyAction.BACKSPACE -> Unit // already handled on press
            else -> listener?.onKey(key)
        }
    }

    private fun down(id: Int, x: Float, y: Float) {
        val kr = findKey(x, y) ?: return
        // Rollover: a second finger lands while the first is still down, so type the first key now.
        for (i in 0 until pointers.size()) {
            val o = pointers.valueAt(i)
            if (!o.committed && !o.popup && !o.longDone && !o.cursorMode && o.kr.key.action == KeyAction.CHAR) {
                commit(o.kr.key)
                o.committed = true
                o.runnable?.let { handler.removeCallbacks(it) }
            }
        }
        val p = Ptr(kr, x, y)
        pointers.put(id, p)
        feedback?.onPress(kr.key)

        when (kr.key.action) {
            KeyAction.BACKSPACE -> {
                listener?.onBackspace()
                p.committed = true
                scheduleBackspaceRepeat(id, p)
            }
            KeyAction.SPACE -> schedule(id, p, longPressMs * 2) {
                p.longDone = true
                listener?.onSpaceLongPress()
            }
            KeyAction.CHAR -> if (kr.key.popups.isNotEmpty()) schedule(id, p, longPressMs) {
                p.popup = true
                p.popupIndex = popupIndexFor(p, p.x)
                invalidate()
            }
            else -> Unit
        }
        invalidate()
    }

    private fun schedule(id: Int, p: Ptr, delay: Long, action: () -> Unit) {
        val r = Runnable { if (pointers.get(id) === p) action() }
        p.runnable = r
        handler.postDelayed(r, delay)
    }

    private fun scheduleBackspaceRepeat(id: Int, p: Ptr) {
        val r = object : Runnable {
            override fun run() {
                if (pointers.get(id) !== p) return
                listener?.onBackspace()
                p.repeatCount++
                handler.postDelayed(this, if (p.repeatCount > 12) 28L else 55L)
            }
        }
        p.runnable = r
        handler.postDelayed(r, 400L)
    }

    private fun move(id: Int, x: Float, y: Float) {
        val p = pointers.get(id) ?: return
        p.x = x
        if (p.popup) {
            p.popupIndex = popupIndexFor(p, x)
            invalidate()
            return
        }
        when (p.kr.key.action) {
            KeyAction.SPACE -> {
                if (!p.cursorMode && abs(x - p.downX) > dp(24f)) {
                    p.cursorMode = true
                    p.runnable?.let { handler.removeCallbacks(it) }
                }
                if (p.cursorMode) {
                    val step = dp(14f)
                    val n = ((x - p.anchorX) / step).toInt()
                    if (n != 0) {
                        listener?.onCursorMove(n)
                        p.anchorX += n * step
                    }
                }
            }
            KeyAction.BACKSPACE -> Unit
            else -> {
                val kr = findKey(x, y)
                if (kr != null && kr !== p.kr && !p.committed) {
                    p.runnable?.let { handler.removeCallbacks(it) }
                    p.kr = kr
                    invalidate()
                }
            }
        }
    }

    private fun up(id: Int) {
        val p = pointers.get(id) ?: return
        p.runnable?.let { handler.removeCallbacks(it) }
        pointers.remove(id)
        when {
            p.popup -> {
                val opts = popupOptions(p)
                if (opts.isNotEmpty()) listener?.onText(opts[p.popupIndex.coerceIn(0, opts.lastIndex)])
            }
            p.committed || p.longDone || p.cursorMode -> Unit
            else -> commit(p.kr.key)
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        cancelAll()
        super.onDetachedFromWindow()
    }
}

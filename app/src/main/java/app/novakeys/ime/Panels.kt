package app.novakeys.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.novakeys.R
import app.novakeys.data.ClipboardStore
import app.novakeys.data.EmojiData
import app.novakeys.data.StickerStore
import app.novakeys.feature.StyledText
import app.novakeys.keyboard.IconView
import app.novakeys.keyboard.ToolIcon
import app.novakeys.theme.KeyboardTheme
import java.io.File

/** What the panels can ask the keyboard service to do. */
interface PanelHost {
    fun commit(text: String)
    fun backspace()
    fun closePanel()
    fun showLayout(id: String)
    fun commitSticker(file: File)
    fun openSettings()
    fun openOcr()
    fun toggleStyle(styleId: String)
    fun activeStyle(): String?
    fun toggleOneHanded()
    /** 0 = off, 1 = left hand, 2 = right hand. */
    fun oneHandedState(): Int
    fun selectAll()
}

/** Builds the full-height panels that replace the key area. Plain views, themed at build time. */
class Panels(private val ctx: Context, private val theme: KeyboardTheme, private val host: PanelHost, private val height: Int) {

    private val d = ctx.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    private fun rounded(color: Int, radiusDp: Float = 12f) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusDp * d
    }

    private fun root(): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(theme.background)
        layoutParams = ViewGroup.LayoutParams(-1, height)
    }

    private fun text(s: String, sizeSp: Float, color: Int = theme.textColor, bold: Boolean = false) = TextView(ctx).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    /** Dims a view while it is held down, so every tappable thing answers the finger. */
    @SuppressLint("ClickableViewAccessibility")
    private fun <T : View> T.pressable(onClick: () -> Unit): T {
        setOnClickListener { onClick() }
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.alpha = 0.55f
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.alpha = 1f
            }
            false
        }
        return this
    }

    private fun chip(label: String, selected: Boolean, onClick: () -> Unit) = text(label, 15f,
        if (selected) theme.accentTextColor else theme.specialTextColor).apply {
        setPadding(dp(14), dp(7), dp(14), dp(7))
        background = rounded(if (selected) theme.accentColor else theme.keyColor, 18f)
        pressable(onClick)
    }

    /** A round tappable icon with a key-coloured background. */
    private fun iconButton(icon: ToolIcon, description: String, sizeDp: Int = 40, onClick: () -> Unit): View =
        IconView(ctx, icon, theme.specialTextColor).apply {
            background = rounded(theme.keyColor, sizeDp / 2f)
            contentDescription = description
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
            pressable(onClick)
        }

    private fun pill(label: String, onClick: () -> Unit) = text(label, 14f, theme.specialTextColor).apply {
        setPadding(dp(14), dp(8), dp(14), dp(8))
        background = rounded(theme.keyColor, 18f)
        pressable(onClick)
    }

    /** Top bar: a back button, a title, and an optional trailing control. */
    private fun header(title: String, trailing: View? = null): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), dp(4), dp(8), dp(4))
        addView(iconButton(ToolIcon.BACK, "Back to keyboard") { host.closePanel() })
        addView(text(title, 16f, theme.textColor, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (trailing != null) addView(trailing)
        else addView(View(ctx), LinearLayout.LayoutParams(dp(40), 1))
    }

    private fun emptyState(icon: ToolIcon, message: String, hint: String? = null): View = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(30), dp(28), dp(30), 0)
        addView(IconView(ctx, icon, theme.hintColor, 0.6f), LinearLayout.LayoutParams(dp(56), dp(56)))
        addView(text(message, 15f, theme.specialTextColor).apply { setPadding(0, dp(8), 0, 0) })
        if (hint != null) addView(text(hint, 13f, theme.hintColor).apply { setPadding(0, dp(4), 0, 0) })
    }

    // ---- Emoji -----------------------------------------------------------------------------

    fun emoji(): View {
        val r = root()
        val grid = GridLayout(ctx).apply { columnCount = 8 }
        val scroll = ScrollView(ctx).apply { addView(grid) }
        val tabs = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(8), 0, dp(8), dp(4)) }
        val tabScroll = HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(tabs) }

        fun fill(list: List<String>) {
            grid.removeAllViews()
            val cell = (ctx.resources.displayMetrics.widthPixels / 8)
            list.forEach { e ->
                grid.addView(TextView(ctx).apply {
                    text = e
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                    gravity = Gravity.CENTER
                    pressable {
                        host.commit(e)
                        EmojiData.pushRecent(ctx, e)
                    }
                }, GridLayout.LayoutParams().apply { width = cell; height = dp(46) })
            }
            scroll.scrollTo(0, 0)
        }

        val recents = EmojiData.recents(ctx)
        val entries = (if (recents.isNotEmpty()) listOf("🕘" to recents) else emptyList()) +
            EmojiData.categories.map { it.icon to it.emojis }
        var selected = 0
        fun renderTabs() {
            tabs.removeAllViews()
            entries.forEachIndexed { i, (icon, list) ->
                tabs.addView(text(icon, 20f).apply {
                    setPadding(dp(14), dp(6), dp(14), dp(6))
                    alpha = if (i == selected) 1f else 0.5f
                    if (i == selected) background = rounded(theme.keyColor, 14f)
                    setOnClickListener { selected = i; renderTabs(); fill(list) }
                })
            }
        }
        r.addView(header("Emoji", iconButton(ToolIcon.BACKSPACE, "Delete") { host.backspace() }))
        r.addView(tabScroll)
        r.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        renderTabs()
        fill(entries[0].second)
        return r
    }

    // ---- Clipboard -------------------------------------------------------------------------

    fun clipboard(store: ClipboardStore): View {
        val r = root()
        val list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(4), dp(10), dp(10)) }
        val scroll = ScrollView(ctx).apply { addView(list) }

        fun render() {
            list.removeAllViews()
            val items = store.all()
            if (items.isEmpty()) {
                list.addView(emptyState(ToolIcon.CLIP, ctx.getString(R.string.clip_empty), "Copy some text and it will show up here"))
                return
            }
            items.forEach { item ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = rounded(theme.keyColor, 12f)
                    setPadding(dp(14), dp(4), dp(4), dp(4))
                    minimumHeight = dp(48)
                    pressable { host.commit(item.text) }
                }
                row.addView(text(item.text.replace('\n', ' '), 15f).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(IconView(ctx, ToolIcon.PIN, if (item.pinned) theme.accentColor else theme.hintColor, 0.42f).apply {
                    filled = item.pinned
                    contentDescription = if (item.pinned) "Unpin" else "Pin"
                    pressable { store.togglePin(item); render() }
                }, LinearLayout.LayoutParams(dp(44), dp(44)))
                row.addView(IconView(ctx, ToolIcon.CLOSE, theme.hintColor, 0.36f).apply {
                    contentDescription = "Remove"
                    pressable { store.remove(item); render() }
                }, LinearLayout.LayoutParams(dp(44), dp(44)))
                list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
            }
        }
        r.addView(header("Clipboard", pill("Clear") { store.clearUnpinned(); render() }))
        r.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        render()
        return r
    }

    // ---- Stickers --------------------------------------------------------------------------

    fun stickers(store: StickerStore): View {
        val r = root()
        val files = store.all()
        r.addView(header("Stickers"))
        if (files.isEmpty()) {
            r.addView(emptyState(ToolIcon.STICKER, "No stickers yet", "Add images in the Nova Keyboard app"))
            return r
        }
        val grid = GridLayout(ctx).apply { columnCount = 4 }
        val cell = ctx.resources.displayMetrics.widthPixels / 4
        files.forEach { f ->
            grid.addView(ImageView(ctx).apply {
                setPadding(dp(8), dp(8), dp(8), dp(8))
                scaleType = ImageView.ScaleType.FIT_CENTER
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                setImageBitmap(BitmapFactory.decodeFile(f.path, opts))
                pressable { host.commitSticker(f) }
            }, GridLayout.LayoutParams().apply { width = cell; height = cell })
        }
        r.addView(ScrollView(ctx).apply { addView(grid) }, LinearLayout.LayoutParams(-1, 0, 1f))
        return r
    }

    // ---- Tools -----------------------------------------------------------------------------

    fun tools(): View {
        val r = root()
        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(4), dp(12), dp(12)) }

        fun tile(icon: ToolIcon, label: String, sub: String?, onClick: () -> Unit) = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(theme.keyColor, 14f)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            addView(IconView(ctx, icon, theme.textColor, 0.6f), LinearLayout.LayoutParams(dp(34), dp(34)))
            addView(text(label, 12f, theme.specialTextColor).apply { maxLines = 1 })
            if (sub != null) addView(text(sub, 11f, theme.accentColor).apply { maxLines = 1 })
            contentDescription = label
            pressable(onClick)
        }

        class Tool(val icon: ToolIcon, val label: String, val sub: String? = null, val run: () -> Unit)
        val oneHandedSub = when (host.oneHandedState()) { 1 -> "Left"; 2 -> "Right"; else -> "Off" }
        val actions = listOf(
            Tool(ToolIcon.NUMPAD, "Numpad") { host.showLayout("numpad") },
            Tool(ToolIcon.CALC, "Calculator") { host.showLayout("calc") },
            Tool(ToolIcon.CLIP, "Clipboard") { host.showLayout("panel:clip") },
            Tool(ToolIcon.STICKER, "Stickers") { host.showLayout("panel:stickers") },
            Tool(ToolIcon.SELECT, "Select all") { host.selectAll(); host.closePanel() },
            Tool(ToolIcon.CAMERA, "Scan text") { host.openOcr() },
            Tool(ToolIcon.HAND, "One-handed", oneHandedSub) { host.toggleOneHanded(); host.closePanel() },
            Tool(ToolIcon.SETTINGS, "Settings") { host.openSettings() },
        )
        val grid = GridLayout(ctx).apply { columnCount = 4 }
        val cw = (ctx.resources.displayMetrics.widthPixels - dp(24)) / 4
        actions.forEach { t ->
            grid.addView(tile(t.icon, t.label, t.sub, t.run), GridLayout.LayoutParams().apply {
                width = cw - dp(6); height = dp(78); setMargins(dp(3), dp(3), dp(3), dp(3))
            })
        }
        body.addView(grid)

        body.addView(text("TEXT STYLES", 12f, theme.hintColor, true).apply {
            gravity = Gravity.START; setPadding(dp(6), dp(14), 0, dp(2)); letterSpacing = 0.08f
        })
        body.addView(text("Pick a style, go back, then type. Select text first to restyle it.", 12f, theme.hintColor).apply {
            gravity = Gravity.START; setPadding(dp(6), 0, dp(6), dp(8))
        })
        val styleFlow = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        var rowLayout = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        var used = 0
        val maxW = ctx.resources.displayMetrics.widthPixels - dp(24)
        fun render() {
            styleFlow.removeAllViews()
            rowLayout = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            styleFlow.addView(rowLayout)
            used = 0
            StyledText.styles.forEach { s ->
                val v = chip(s.label, host.activeStyle() == s.id) { host.toggleStyle(s.id); render() }
                v.measure(0, 0)
                val w = v.measuredWidth + dp(8)
                if (used + w > maxW) {
                    rowLayout = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                    styleFlow.addView(rowLayout, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
                    used = 0
                }
                rowLayout.addView(v, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
                used += w
            }
        }
        render()
        body.addView(styleFlow)

        r.addView(header("Tools"))
        r.addView(ScrollView(ctx).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        return r
    }
}

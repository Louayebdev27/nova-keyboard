package app.novakeys.ime

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

    private fun chip(label: String, selected: Boolean, onClick: () -> Unit) = text(label, 15f,
        if (selected) theme.accentTextColor else theme.specialTextColor).apply {
        setPadding(dp(14), dp(7), dp(14), dp(7))
        background = rounded(if (selected) theme.accentColor else theme.keyColor, 18f)
        setOnClickListener { onClick() }
    }

    /** Top bar with a back button, a title and an optional trailing button. */
    private fun header(title: String, trailing: View? = null): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), dp(4), dp(8), dp(4))
        addView(text("ABC", 15f, theme.accentColor, true).apply {
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(theme.keyColor, 14f)
            setOnClickListener { host.closePanel() }
        })
        addView(text(title, 16f, theme.textColor, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (trailing != null) addView(trailing)
        else addView(View(ctx), LinearLayout.LayoutParams(dp(56), 1))
    }

    // ---- Emoji -----------------------------------------------------------------------------

    fun emoji(): View {
        val r = root()
        val grid = GridLayout(ctx).apply { columnCount = 8 }
        val scroll = ScrollView(ctx).apply { addView(grid) }
        val tabs = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val tabScroll = HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(tabs) }

        fun fill(list: List<String>) {
            grid.removeAllViews()
            val cell = (ctx.resources.displayMetrics.widthPixels / 8)
            list.forEach { e ->
                grid.addView(TextView(ctx).apply {
                    text = e
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                    gravity = Gravity.CENTER
                    setOnClickListener {
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
        val del = text("⌫", 18f, theme.specialTextColor).apply {
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = rounded(theme.keyColor, 14f)
            setOnClickListener { host.backspace() }
        }
        r.addView(header("Emoji", del))
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
                list.addView(text(ctx.getString(R.string.clip_empty), 15f, theme.hintColor).apply { setPadding(0, dp(40), 0, 0) })
                return
            }
            items.forEach { item ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = rounded(theme.keyColor, 12f)
                    setPadding(dp(14), dp(10), dp(6), dp(10))
                    setOnClickListener { host.commit(item.text) }
                }
                row.addView(text(item.text.replace('\n', ' '), 15f).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(text(if (item.pinned) "📌" else "📍", 16f, theme.hintColor).apply {
                    alpha = if (item.pinned) 1f else 0.4f
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    setOnClickListener { store.togglePin(item); render() }
                })
                row.addView(text("✕", 15f, theme.hintColor).apply {
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    setOnClickListener { store.remove(item); render() }
                })
                list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
            }
        }
        val clear = text("Clear", 14f, theme.specialTextColor).apply {
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = rounded(theme.keyColor, 14f)
            setOnClickListener { store.clearUnpinned(); render() }
        }
        r.addView(header("Clipboard", clear))
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
            r.addView(text(ctx.getString(R.string.sticker_empty), 15f, theme.hintColor).apply { setPadding(dp(30), dp(40), dp(30), 0) })
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
                setOnClickListener { host.commitSticker(f) }
            }, GridLayout.LayoutParams().apply { width = cell; height = cell })
        }
        r.addView(ScrollView(ctx).apply { addView(grid) }, LinearLayout.LayoutParams(-1, 0, 1f))
        return r
    }

    // ---- Tools -----------------------------------------------------------------------------

    fun tools(): View {
        val r = root()
        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(4), dp(12), dp(12)) }

        fun tile(icon: String, label: String, onClick: () -> Unit) = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(theme.keyColor, 14f)
            setPadding(dp(4), dp(10), dp(4), dp(10))
            addView(text(icon, 22f))
            addView(text(label, 12f, theme.specialTextColor))
            setOnClickListener { onClick() }
        }

        class Tool(val icon: String, val label: String, val run: () -> Unit)
        val actions = listOf(
            Tool("🔢", "Numpad") { host.showLayout("numpad") },
            Tool("🧮", "Calculator") { host.showLayout("calc") },
            Tool("📋", "Clipboard") { host.showLayout("panel:clip") },
            Tool("🖼️", "Stickers") { host.showLayout("panel:stickers") },
            Tool("🔠", "Select all") { host.selectAll(); host.closePanel() },
            Tool("📷", "Text from image") { host.openOcr() },
            Tool("🤚", "One-handed") { host.toggleOneHanded(); host.closePanel() },
            Tool("⚙️", "Settings") { host.openSettings() },
        )
        val grid = GridLayout(ctx).apply { columnCount = 4 }
        val cw = (ctx.resources.displayMetrics.widthPixels - dp(24)) / 4
        actions.forEach { t ->
            val (i, l, a) = Triple(t.icon, t.label, t.run)
            grid.addView(tile(i, l, a), GridLayout.LayoutParams().apply {
                width = cw - dp(6); height = dp(72); setMargins(dp(3), dp(3), dp(3), dp(3))
            })
        }
        body.addView(grid)

        body.addView(text("Text styles", 13f, theme.hintColor).apply {
            gravity = Gravity.START; setPadding(dp(4), dp(12), 0, dp(6))
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

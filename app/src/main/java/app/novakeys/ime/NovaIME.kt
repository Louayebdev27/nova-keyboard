package app.novakeys.ime

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import app.novakeys.R
import app.novakeys.data.ClipboardStore
import app.novakeys.data.Predictors
import app.novakeys.data.Prefs
import app.novakeys.data.ShortcutStore
import app.novakeys.data.StickerStore
import app.novakeys.feature.Calc
import app.novakeys.feature.KeyFeedbackImpl
import app.novakeys.feature.OcrActivity
import app.novakeys.feature.StyledText
import app.novakeys.feature.VoiceActivity
import app.novakeys.keyboard.Key
import app.novakeys.keyboard.KeyAction
import app.novakeys.keyboard.KeyboardActionListener
import app.novakeys.keyboard.KeyboardView
import app.novakeys.keyboard.Layouts
import app.novakeys.theme.KeyboardTheme
import app.novakeys.theme.ThemeStore
import app.novakeys.ui.MainActivity
import java.io.File

class NovaIME : InputMethodService(), KeyboardActionListener, PanelHost {

    private lateinit var prefs: Prefs
    private lateinit var themes: ThemeStore
    private lateinit var shortcuts: ShortcutStore
    private lateinit var clipStore: ClipboardStore
    private lateinit var stickerStore: StickerStore
    private lateinit var feedback: KeyFeedbackImpl
    private lateinit var clipboardManager: ClipboardManager

    private var root: LinearLayout? = null
    private var content: FrameLayout? = null
    private var kbd: KeyboardView? = null
    private var bar: LinearLayout? = null
    private val slots = ArrayList<TextView>()
    private var toolsButton: TextView? = null
    private var panel: View? = null
    private var theme: KeyboardTheme? = null
    private var bgCacheKey: String? = null
    private var bgBitmap: Bitmap? = null

    // keyboard state
    private var mode = "letters"            // letters | symbols | symbols2 | numpad | calc
    private var startMode = "letters"
    private var lang = "en"
    private var shift = 0
    private var lastShiftTap = 0L
    private var lastSpaceAt = 0L
    private var styleId: String? = null
    private var noSuggest = false
    private var noLearn = false
    private var quickChips: List<String> = emptyList()
    private var enterLabel: String? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (!prefs.clipboardHistory || noLearn) return@OnPrimaryClipChangedListener
        runCatching {
            val clip = clipboardManager.primaryClip ?: return@runCatching
            if (clip.itemCount > 0) clip.getItemAt(0).text?.toString()?.let { clipStore.add(it) }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ---- lifecycle --------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs.get(this)
        themes = ThemeStore(this)
        shortcuts = ShortcutStore(this)
        clipStore = ClipboardStore(this)
        stickerStore = StickerStore(this)
        feedback = KeyFeedbackImpl(this)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipListener)
        VoiceActivity.onResult = { text -> commit(text) }
    }

    override fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        VoiceActivity.onResult = null
        Predictors.saveAll()
        feedback.release()
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        val suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tools = TextView(this).apply {
            text = "⋯"
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setOnClickListener { showPanel(Panels(this@NovaIME, currentTheme(), this@NovaIME, panelHeight()).tools()) }
        }
        suggestionBar.addView(tools, LinearLayout.LayoutParams(dp(48), dp(42)))
        slots.clear()
        repeat(3) {
            val tv = TextView(this).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(dp(6), 0, dp(6), 0)
            }
            slots += tv
            suggestionBar.addView(tv, LinearLayout.LayoutParams(0, dp(42), 1f))
        }
        val frame = FrameLayout(this).apply { clipChildren = false; clipToPadding = false }
        val keyboard = KeyboardView(this).apply {
            listener = this@NovaIME
            feedback = this@NovaIME.feedback
        }
        frame.addView(keyboard, FrameLayout.LayoutParams(-1, -2))
        rootLayout.addView(suggestionBar, LinearLayout.LayoutParams(-1, dp(42)))
        rootLayout.addView(frame, LinearLayout.LayoutParams(-1, -2))

        root = rootLayout; bar = suggestionBar; content = frame; kbd = keyboard; toolsButton = tools
        applyTheme()
        applySettingsToView()
        rebuildLayout()
        return rootLayout
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        classifyField(info)
        if (!restarting) {
            lang = prefs.currentLanguage.takeIf { it in prefs.languages } ?: prefs.languages.firstOrNull() ?: "en"
            styleId = null
            closePanel()
        }
        applyTheme()
        applySettingsToView()
        rebuildLayout()
        autoCapitalize()
        updateSuggestions()
    }

    override fun onFinishInput() {
        Predictors.saveAll()
        super.onFinishInput()
    }

    private fun classifyField(info: EditorInfo) {
        val type = info.inputType
        val cls = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        startMode = when (cls) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> "numpad"
            else -> "letters"
        }
        mode = startMode
        val password = (cls == InputType.TYPE_CLASS_TEXT && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        noLearn = password
        noSuggest = password || cls != InputType.TYPE_CLASS_TEXT ||
            (type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0 ||
            variation == InputType.TYPE_TEXT_VARIATION_URI ||
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        quickChips = when (variation) {
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ->
                if (cls == InputType.TYPE_CLASS_TEXT) listOf("@", ".com", ".net") else emptyList()
            InputType.TYPE_TEXT_VARIATION_URI -> listOf("www.", ".com", "/")
            else -> emptyList()
        }
        enterLabel = if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) null
        else when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            else -> null
        }
    }

    // ---- theme and layout -------------------------------------------------------------------

    private fun currentTheme(): KeyboardTheme = theme ?: themes.active().also { theme = it }

    private fun applyTheme() {
        val t = themes.active()
        theme = t
        val file = themes.backgroundFile(t)
        val key = file?.let { it.path + it.lastModified() }
        if (key != bgCacheKey) {
            bgBitmap?.recycle()
            bgBitmap = file?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }
            bgCacheKey = key
        }
        kbd?.setTheme(t, bgBitmap)
        bar?.setBackgroundColor(t.background)
        root?.setBackgroundColor(t.background)
        toolsButton?.setTextColor(t.specialTextColor)
        slots.forEach { it.setTextColor(t.textColor) }
    }

    private fun applySettingsToView() {
        kbd?.apply {
            heightScale = prefs.keyHeightScale
            showPreview = prefs.keyPreview
            longPressMs = prefs.longPressMs.toLong()
        }
        val side = when (prefs.oneHanded) {
            1 -> resources.displayMetrics.widthPixels * 15 / 100 to 0
            2 -> 0 to resources.displayMetrics.widthPixels * 15 / 100
            else -> 0 to 0
        }
        // oneHanded 1 = left hand: keyboard hugs the left edge, so extra space goes on the right.
        content?.setPadding(side.second, 0, side.first, 0)
        bar?.visibility = if (prefs.suggestionsEnabled || quickChips.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun rebuildLayout() {
        val view = kbd ?: return
        val bottom = prefs.bottomRow
        val l = when (mode) {
            "symbols" -> Layouts.symbols(1, prefs.numberRow, bottom, enterLabel)
            "symbols2" -> Layouts.symbols(2, prefs.numberRow, bottom, enterLabel)
            "numpad" -> Layouts.numpad(enterLabel)
            "calc" -> Layouts.calculator()
            else -> Layouts.letters(lang, prefs.numberRow, bottom, enterLabel)
        }
        view.setLayout(l)
        view.shiftState = shift
    }

    private fun panelHeight(): Int {
        val h = kbd?.height ?: 0
        return if (h > 0) h else dp(250)
    }

    private fun showPanel(v: View) {
        closePanel()
        panel = v
        content?.addView(v, FrameLayout.LayoutParams(-1, panelHeight()))
        kbd?.visibility = View.INVISIBLE
    }

    override fun closePanel() {
        panel?.let { content?.removeView(it) }
        panel = null
        kbd?.visibility = View.VISIBLE
    }

    // ---- KeyboardActionListener -------------------------------------------------------------

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        val out = styleId?.let { StyledText.apply(it, text) } ?: text
        val c = text.firstOrNull()
        if (text.length == 1 && c != null && !TextUtil.isWordChar(c) && !c.isDigit()) {
            learnCurrentWord(ic) // punctuation ends a word
        }
        ic.commitText(out, 1)
        lastSpaceAt = 0L
        if (shift == 1) { shift = 0; kbd?.shiftState = 0 }
        autoCapitalize()
        updateSuggestions()
    }

    override fun onKey(key: Key) {
        when (key.action) {
            KeyAction.SHIFT -> toggleShift()
            KeyAction.ENTER -> handleEnter()
            KeyAction.SPACE -> handleSpace()
            KeyAction.SWITCH -> {
                mode = when (key.target) { "symbols" -> "symbols"; "symbols2" -> "symbols2"; else -> "letters" }
                rebuildLayout()
            }
            KeyAction.EMOJI -> showPanel(Panels(this, currentTheme(), this, panelHeight()).emoji())
            KeyAction.CLIP -> showPanel(Panels(this, currentTheme(), this, panelHeight()).clipboard(clipStore))
            KeyAction.TOOLS -> showPanel(Panels(this, currentTheme(), this, panelHeight()).tools())
            KeyAction.VOICE -> startVoice()
            KeyAction.LANG -> cycleLanguage()
            KeyAction.EQUALS -> calculate()
            else -> Unit
        }
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) ic.commitText("", 1) else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        lastSpaceAt = 0L
        autoCapitalize()
        updateSuggestions()
    }

    override fun onCursorMove(delta: Int) {
        val code = if (delta > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(kotlin.math.abs(delta)) { sendDownUpKeyEvents(code) }
        updateSuggestions()
    }

    override fun onSpaceLongPress() {
        if (prefs.languages.size > 1) cycleLanguage()
        else (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    // ---- editing helpers --------------------------------------------------------------------

    private fun before(n: Int = 60): String =
        currentInputConnection?.getTextBeforeCursor(n, 0)?.toString() ?: ""

    private fun toggleShift() {
        val now = SystemClock.uptimeMillis()
        shift = when {
            shift == 2 -> 0
            now - lastShiftTap < 350 && shift == 1 -> 2
            shift == 1 -> 0
            else -> 1
        }
        lastShiftTap = now
        kbd?.shiftState = shift
    }

    private fun autoCapitalize() {
        if (mode != "letters" || !prefs.autoCapitalize || shift == 2) return
        val ei = currentInputEditorInfo ?: return
        if (ei.inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return
        val caps = currentInputConnection?.getCursorCapsMode(ei.inputType) ?: 0
        val want = if (caps != 0) 1 else 0
        if (want != shift) { shift = want; kbd?.shiftState = shift }
    }

    private fun handleEnter() {
        val ei = currentInputEditorInfo
        val ic = currentInputConnection ?: return
        val action = (ei?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noEnterAction = ((ei?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            learnCurrentWord(ic)
            ic.performEditorAction(action)
        } else {
            learnCurrentWord(ic)
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
        lastSpaceAt = 0L
        autoCapitalize()
        updateSuggestions()
    }

    private fun handleSpace() {
        val ic = currentInputConnection ?: return
        val text = before()
        val now = SystemClock.uptimeMillis()
        if (prefs.doubleSpacePeriod && now - lastSpaceAt < 450 && TextUtil.shouldDoubleSpacePeriod(text)) {
            ic.deleteSurroundingText(1, 0)
            ic.commitText(". ", 1)
            lastSpaceAt = 0L
            autoCapitalize()
            updateSuggestions()
            return
        }
        val word = TextUtil.currentWord(text)
        val expansion = if (word.isNotEmpty()) shortcuts.expand(word) else null
        if (expansion != null) {
            ic.deleteSurroundingText(word.length, 0)
            ic.commitText("$expansion ", 1)
        } else {
            learnCurrentWord(ic, text)
            ic.commitText(" ", 1)
        }
        lastSpaceAt = now
        if (shift == 1) { shift = 0; kbd?.shiftState = 0 }
        autoCapitalize()
        updateSuggestions()
    }

    private fun learnCurrentWord(ic: android.view.inputmethod.InputConnection, text: String = before()) {
        if (noLearn || !prefs.learnWords) return
        val word = TextUtil.currentWord(text)
        if (word.length < 2) return
        Predictors.get(this, lang).learn(TextUtil.previousWord(text), word)
    }

    private fun calculate() {
        val ic = currentInputConnection ?: return
        val expr = Calc.trailingExpression(before(80))
        val result = Calc.evaluate(expr) ?: return
        ic.commitText("=$result", 1)
    }

    private fun cycleLanguage() {
        val list = prefs.languages
        if (list.isEmpty()) return
        lang = list[(list.indexOf(lang) + 1).mod(list.size)]
        prefs.currentLanguage = lang
        mode = "letters"
        rebuildLayout()
        updateSuggestions()
        Toast.makeText(this, Layouts.languageNames[lang] ?: lang, Toast.LENGTH_SHORT).show()
    }

    private fun startVoice() {
        val tag = when (lang) { "fr" -> "fr-FR"; "ar" -> "ar"; else -> "en-US" }
        startActivity(Intent(this, VoiceActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(VoiceActivity.EXTRA_LANG, tag))
    }

    // ---- suggestions ------------------------------------------------------------------------

    private fun updateSuggestions() {
        if (slots.isEmpty()) return
        val list: List<String> = when {
            quickChips.isNotEmpty() -> quickChips
            noSuggest || !prefs.suggestionsEnabled -> emptyList()
            else -> {
                val text = before()
                val word = TextUtil.currentWord(text)
                val p = Predictors.get(this, lang)
                if (word.isNotEmpty()) p.complete(word, 3)
                else p.nextWords(TextUtil.lastWordForNext(text), 3)
            }
        }
        slots.forEachIndexed { i, tv ->
            val s = list.getOrNull(i)
            tv.text = s ?: ""
            tv.setOnClickListener(if (s == null) null else View.OnClickListener { pickSuggestion(s) })
        }
    }

    private fun pickSuggestion(s: String) {
        val ic = currentInputConnection ?: return
        if (quickChips.isNotEmpty()) { ic.commitText(s, 1); return }
        val text = before()
        val word = TextUtil.currentWord(text)
        if (word.isNotEmpty()) ic.deleteSurroundingText(word.length, 0)
        ic.commitText("$s ", 1)
        if (!noLearn && prefs.learnWords) Predictors.get(this, lang).learn(TextUtil.previousWord(text), s)
        lastSpaceAt = SystemClock.uptimeMillis()
        autoCapitalize()
        updateSuggestions()
    }

    // ---- PanelHost --------------------------------------------------------------------------

    override fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
        lastSpaceAt = 0L
        updateSuggestions()
    }

    override fun backspace() { onBackspace() }

    override fun showLayout(id: String) {
        closePanel()
        when {
            id == "panel:clip" -> showPanel(Panels(this, currentTheme(), this, panelHeight()).clipboard(clipStore))
            id == "panel:stickers" -> showPanel(Panels(this, currentTheme(), this, panelHeight()).stickers(stickerStore))
            else -> { mode = id; rebuildLayout() }
        }
    }

    override fun commitSticker(file: File) {
        val ei = currentInputEditorInfo ?: return
        val ic = currentInputConnection ?: return
        val supported = EditorInfoCompat.getContentMimeTypes(ei).any { ClipDescription.compareMimeTypes("image/png", it) }
        if (!supported) {
            Toast.makeText(this, R.string.sticker_unsupported, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val info = InputContentInfoCompat(uri, ClipDescription("sticker", arrayOf("image/png")), null)
        var flags = 0
        if (Build.VERSION.SDK_INT >= 25) flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        else grantUriPermission(ei.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        InputConnectionCompat.commitContent(ic, ei, info, flags, null)
        closePanel()
    }

    override fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun openOcr() {
        startActivity(Intent(this, OcrActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun toggleStyle(styleId: String) {
        val ic = currentInputConnection
        val sel = ic?.getSelectedText(0)
        if (ic != null && !sel.isNullOrEmpty()) {
            ic.commitText(StyledText.apply(styleId, sel.toString()), 1)
            return
        }
        this.styleId = if (this.styleId == styleId) null else styleId
    }

    override fun activeStyle(): String? = styleId

    override fun toggleOneHanded() {
        prefs.oneHanded = when (prefs.oneHanded) { 0 -> 2; 2 -> 1; else -> 0 }
        applySettingsToView()
        rebuildLayout()
    }

    override fun selectAll() {
        currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
    }
}

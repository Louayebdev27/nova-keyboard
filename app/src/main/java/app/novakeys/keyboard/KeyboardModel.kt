package app.novakeys.keyboard

enum class KeyAction {
    CHAR, SHIFT, BACKSPACE, ENTER, SPACE,
    SWITCH,   // change layout page, see Key.target
    EMOJI, VOICE, LANG, CLIP, TOOLS,
    EQUALS,   // calculator "="
}

data class Key(
    val label: String,
    val action: KeyAction = KeyAction.CHAR,
    val output: String = label,
    val weight: Float = 1f,
    val popups: List<String> = emptyList(),
    val hint: String? = null,
    val target: String? = null,
) {
    val isSpecial: Boolean get() = action != KeyAction.CHAR
}

data class Row(val keys: List<Key>, val inset: Float = 0f)

data class KeyboardLayout(
    val id: String,
    val rows: List<Row>,
    val rtl: Boolean = false,
    /** Whether the Shift state changes the case of the labels (Latin scripts). */
    val shiftable: Boolean = true,
)

/** Where the key press ended up, used by the view to talk to the service. */
interface KeyboardActionListener {
    fun onText(text: String)
    fun onKey(key: Key)
    fun onBackspace()
    /** Positive moves the caret right, negative left, in characters. */
    fun onCursorMove(delta: Int)
    fun onSpaceLongPress()
}

/** Plays sound / vibration for presses. Implemented in the feature package. */
interface KeyFeedback {
    fun onPress(key: Key)
}

package app.novakeys.keyboard

/** Lists the keys that can sit on the bottom row. The user picks and orders them in settings. */
object BottomKeys {
    val all = listOf("symbols", "emoji", "comma", "space", "period", "enter", "voice", "lang", "clip", "tools")

    fun label(id: String): String = when (id) {
        "symbols" -> "Symbols / ABC"
        "emoji" -> "Emoji"
        "comma" -> "Comma"
        "space" -> "Space"
        "period" -> "Period"
        "enter" -> "Enter"
        "voice" -> "Voice"
        "lang" -> "Language"
        "clip" -> "Clipboard"
        "tools" -> "Tools"
        else -> id
    }
}

object Layouts {

    val languageNames = mapOf("en" to "English", "fr" to "Français", "ar" to "العربية")

    private fun ch(c: String, popups: String = "", hint: String? = null, weight: Float = 1f) =
        Key(label = c, popups = popups.map { it.toString() }, hint = hint, weight = weight)

    private fun chars(s: String, popups: Map<Char, String> = emptyMap()): List<Key> =
        s.map { ch(it.toString(), popups[it] ?: "") }

    // ---- Latin popups (long-press alternatives) ----------------------------------------------

    private val enPopups = mapOf(
        'a' to "àáâäæãåā", 'e' to "èéêëēėę", 'i' to "îïíīįì", 'o' to "ôöòóœøōõ",
        'u' to "ûüùúū", 'c' to "çćč", 'n' to "ñń", 's' to "ßśš", 'y' to "ÿý",
        'z' to "žźż", 'l' to "ł", 'd' to "ð",
    )
    private val frPopups = mapOf(
        'a' to "àâæáä", 'e' to "éèêë€", 'u' to "ùûüú", 'i' to "îïí", 'o' to "ôœöó",
        'c' to "ç", 'y' to "ÿ", 'n' to "ñ",
    )
    private val arPopups = mapOf(
        'ا' to "أإآٱ", 'ي' to "ئى", 'و' to "ؤ", 'ه' to "ة", 'ت' to "ث", 'ك' to "گ",
        'ب' to "پ", 'ف' to "ڤ", 'ج' to "چ", 'ز' to "ژ",
    )

    private val superscripts = mapOf(
        '1' to "¹½⅓¼", '2' to "²⅔", '3' to "³¾", '4' to "⁴", '5' to "⁵",
        '6' to "⁶", '7' to "⁷", '8' to "⁸", '9' to "⁹", '0' to "⁰°∅",
    )
    private val arabicIndic = mapOf(
        '1' to "١", '2' to "٢", '3' to "٣", '4' to "٤", '5' to "٥",
        '6' to "٦", '7' to "٧", '8' to "٨", '9' to "٩", '0' to "٠",
    )

    private fun numberRow(lang: String): Row {
        val map = if (lang == "ar") arabicIndic else superscripts
        return Row("1234567890".map { ch(it.toString(), map[it] ?: "") })
    }

    // ---- Letters ---------------------------------------------------------------------------

    private val shiftKey = Key("shift", KeyAction.SHIFT, weight = 1.5f)
    private val backspaceKey = Key("bksp", KeyAction.BACKSPACE, weight = 1.5f)

    fun letters(lang: String, withNumberRow: Boolean, bottom: List<String>, enterLabel: String?): KeyboardLayout {
        val rows = mutableListOf<Row>()
        if (withNumberRow) rows += numberRow(lang)
        when (lang) {
            "fr" -> {
                rows += Row(chars("azertyuiop", frPopups))
                rows += Row(chars("qsdfghjklm", frPopups))
                rows += Row(listOf(shiftKey) + chars("wxcvbn", frPopups) + ch("'", "’`") + backspaceKey.copy(weight = 1.3f))
            }
            "ar" -> {
                rows += Row(chars("ضصثقفغعهخحج", arPopups))
                rows += Row(chars("شسيبلاتنمكط", arPopups))
                rows += Row(chars("ذدزروةىؤءئظ", arPopups) + backspaceKey)
            }
            else -> {
                rows += Row(chars("qwertyuiop", enPopups))
                rows += Row(chars("asdfghjkl", enPopups), inset = 0.5f)
                rows += Row(listOf(shiftKey) + chars("zxcvbnm", enPopups) + backspaceKey)
            }
        }
        rows += bottomRow(bottom, onSymbols = false, lang = lang, enterLabel = enterLabel)
        return KeyboardLayout("letters-$lang", rows, rtl = lang == "ar", shiftable = lang != "ar")
    }

    // ---- Symbols ---------------------------------------------------------------------------

    private val symbolPopups = mapOf(
        '-' to "–—•", '.' to "…", '!' to "¡", '?' to "¿", '$' to "€£¥¢₿",
        '\'' to "‘’‚", '"' to "“”„«»", '%' to "‰", '=' to "≈≠±", '+' to "±",
    )

    fun symbols(page: Int, withDigits: Boolean, bottom: List<String>, enterLabel: String?): KeyboardLayout {
        val rows = mutableListOf<Row>()
        if (withDigits) rows += Row("1234567890".map { ch(it.toString(), superscripts[it] ?: "") })
        // Three symbol rows, so the keyboard keeps the same height as the letters page.
        if (page == 1) {
            rows += Row(chars("@#$%&-+()/", symbolPopups))
            rows += Row(chars("*\"':;!?_=|", symbolPopups))
            rows += Row(listOf(Key("=\\<", KeyAction.SWITCH, target = "symbols2", weight = 1.5f)) +
                chars("~`^<>{}", symbolPopups) + backspaceKey)
        } else {
            rows += Row(chars("~`|•√π÷×¶∆", symbolPopups))
            rows += Row(chars("£¢€¥°^[]{}", symbolPopups))
            rows += Row(listOf(Key("?123", KeyAction.SWITCH, target = "symbols", weight = 1.5f)) +
                chars("<>=§©®™", symbolPopups) + backspaceKey)
        }
        rows += bottomRow(bottom, onSymbols = true, lang = "en", enterLabel = enterLabel)
        return KeyboardLayout("symbols$page", rows, shiftable = false)
    }

    // ---- Numpad and calculator -------------------------------------------------------------

    fun numpad(enterLabel: String?): KeyboardLayout {
        fun num(c: String) = Key(c, weight = 1f)
        val rows = listOf(
            Row(listOf(num("1"), num("2"), num("3"), Key("bksp", KeyAction.BACKSPACE))),
            Row(listOf(num("4"), num("5"), num("6"), num(","))),
            Row(listOf(num("7"), num("8"), num("9"), num("."))),
            Row(listOf(
                Key("ABC", KeyAction.SWITCH, target = "letters"),
                Key("+"), num("0"),
                Key(enterLabel ?: "enter", KeyAction.ENTER),
            )),
        )
        return KeyboardLayout("numpad", rows, shiftable = false)
    }

    fun calculator(): KeyboardLayout {
        fun k(c: String) = Key(c)
        val rows = listOf(
            Row(listOf(k("("), k(")"), k("%"), Key("÷", output = "÷"))),
            Row(listOf(k("7"), k("8"), k("9"), Key("×", output = "×"))),
            Row(listOf(k("4"), k("5"), k("6"), Key("−", output = "−"))),
            Row(listOf(k("1"), k("2"), k("3"), k("+"))),
            Row(listOf(
                Key("ABC", KeyAction.SWITCH, target = "letters"),
                k("0"), k("."), Key("=", KeyAction.EQUALS),
                Key("bksp", KeyAction.BACKSPACE),
            )),
        )
        return KeyboardLayout("calc", rows, shiftable = false)
    }

    // ---- Bottom row (user configurable) -----------------------------------------------------

    fun bottomRow(ids: List<String>, onSymbols: Boolean, lang: String, enterLabel: String?): Row {
        val order = if ("space" in ids) ids else ids + "space"
        val keys = order.mapNotNull { id ->
            when (id) {
                "symbols" ->
                    if (onSymbols) Key("ABC", KeyAction.SWITCH, target = "letters", weight = 1.4f)
                    else Key("?123", KeyAction.SWITCH, target = "symbols", weight = 1.4f)
                "emoji" -> Key("emoji", KeyAction.EMOJI)
                "comma" -> Key(",", popups = listOf("،", ";", "\"", "'"))
                "space" -> Key(languageNames[lang] ?: "", KeyAction.SPACE, output = " ", weight = 4f)
                "period" -> Key(".", popups = listOf("…", "!", "?", ":", ";", "؟"))
                "enter" -> Key(enterLabel ?: "enter", KeyAction.ENTER, weight = 1.5f)
                "voice" -> Key("voice", KeyAction.VOICE)
                "lang" -> Key("lang", KeyAction.LANG)
                "clip" -> Key("clip", KeyAction.CLIP)
                "tools" -> Key("tools", KeyAction.TOOLS)
                else -> null
            }
        }
        return Row(keys)
    }
}

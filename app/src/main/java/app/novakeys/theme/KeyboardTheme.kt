package app.novakeys.theme

import org.json.JSONObject

/**
 * Everything the keyboard view needs to paint itself. Colours are ARGB ints.
 * Fonts are system families only, so no font files need to be shipped.
 */
data class KeyboardTheme(
    val id: String,
    val name: String,
    val background: Int,
    val keyColor: Int,
    val keyPressedColor: Int,
    val specialKeyColor: Int,
    val accentColor: Int,
    val textColor: Int,
    val specialTextColor: Int,
    val accentTextColor: Int,
    val hintColor: Int,
    val borderColor: Int = 0,
    val borderWidthDp: Float = 0f,
    val keyRadiusDp: Float = 10f,
    val keyGapDp: Float = 5f,
    val keyShadow: Boolean = true,
    val keyAlpha: Float = 1f,
    val fontFamily: String = "sans-serif",
    val boldText: Boolean = false,
    val textSizeSp: Float = 20f,
    val backgroundImage: String? = null,
    val backgroundDim: Float = 0.25f,
    val builtIn: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("background", background); put("keyColor", keyColor)
        put("keyPressedColor", keyPressedColor); put("specialKeyColor", specialKeyColor)
        put("accentColor", accentColor); put("textColor", textColor)
        put("specialTextColor", specialTextColor); put("accentTextColor", accentTextColor)
        put("hintColor", hintColor); put("borderColor", borderColor)
        put("borderWidthDp", borderWidthDp.toDouble()); put("keyRadiusDp", keyRadiusDp.toDouble())
        put("keyGapDp", keyGapDp.toDouble()); put("keyShadow", keyShadow)
        put("keyAlpha", keyAlpha.toDouble()); put("fontFamily", fontFamily)
        put("boldText", boldText); put("textSizeSp", textSizeSp.toDouble())
        put("backgroundImage", backgroundImage ?: JSONObject.NULL)
        put("backgroundDim", backgroundDim.toDouble())
    }

    companion object {
        fun fromJson(o: JSONObject, base: KeyboardTheme = Themes.default): KeyboardTheme = KeyboardTheme(
            id = o.optString("id", base.id),
            name = o.optString("name", base.name),
            background = o.optInt("background", base.background),
            keyColor = o.optInt("keyColor", base.keyColor),
            keyPressedColor = o.optInt("keyPressedColor", base.keyPressedColor),
            specialKeyColor = o.optInt("specialKeyColor", base.specialKeyColor),
            accentColor = o.optInt("accentColor", base.accentColor),
            textColor = o.optInt("textColor", base.textColor),
            specialTextColor = o.optInt("specialTextColor", base.specialTextColor),
            accentTextColor = o.optInt("accentTextColor", base.accentTextColor),
            hintColor = o.optInt("hintColor", base.hintColor),
            borderColor = o.optInt("borderColor", base.borderColor),
            borderWidthDp = o.optDouble("borderWidthDp", base.borderWidthDp.toDouble()).toFloat(),
            keyRadiusDp = o.optDouble("keyRadiusDp", base.keyRadiusDp.toDouble()).toFloat(),
            keyGapDp = o.optDouble("keyGapDp", base.keyGapDp.toDouble()).toFloat(),
            keyShadow = o.optBoolean("keyShadow", base.keyShadow),
            keyAlpha = o.optDouble("keyAlpha", base.keyAlpha.toDouble()).toFloat(),
            fontFamily = o.optString("fontFamily", base.fontFamily),
            boldText = o.optBoolean("boldText", base.boldText),
            textSizeSp = o.optDouble("textSizeSp", base.textSizeSp.toDouble()).toFloat(),
            backgroundImage = if (o.isNull("backgroundImage")) null else o.optString("backgroundImage"),
            backgroundDim = o.optDouble("backgroundDim", base.backgroundDim.toDouble()).toFloat(),
        )
    }
}

/** Original colour palettes made for this project. */
object Themes {
    private fun c(rgb: Long): Int = (0xFF000000L or rgb).toInt()

    val midnight = KeyboardTheme(
        id = "midnight", name = "Midnight", builtIn = true,
        background = c(0x12141C), keyColor = c(0x232736), keyPressedColor = c(0x383E55),
        specialKeyColor = c(0x1A1D29), accentColor = c(0x7C83FF),
        textColor = c(0xE9EBF5), specialTextColor = c(0xA9AEC6), accentTextColor = c(0x0F1020),
        hintColor = c(0x6F7592),
    )
    val snow = KeyboardTheme(
        id = "snow", name = "Snow", builtIn = true,
        background = c(0xE7EAF1), keyColor = c(0xFFFFFF), keyPressedColor = c(0xD4D9E6),
        specialKeyColor = c(0xCDD2DF), accentColor = c(0x4F5BFF),
        textColor = c(0x1B1E2B), specialTextColor = c(0x434A63), accentTextColor = c(0xFFFFFF),
        hintColor = c(0x8A90A8),
    )
    val amoled = KeyboardTheme(
        id = "amoled", name = "AMOLED", builtIn = true,
        background = c(0x000000), keyColor = c(0x14141A), keyPressedColor = c(0x2A2A36),
        specialKeyColor = c(0x0B0B10), accentColor = c(0x00E0A4),
        textColor = c(0xF2F2F7), specialTextColor = c(0x9A9AAD), accentTextColor = c(0x00201A),
        hintColor = c(0x5E5E70), keyShadow = false, keyRadiusDp = 14f,
    )
    val aurora = KeyboardTheme(
        id = "aurora", name = "Aurora", builtIn = true,
        background = c(0x1B1033), keyColor = c(0x2F1D55), keyPressedColor = c(0x4B2F86),
        specialKeyColor = c(0x24153F), accentColor = c(0xFF5FC8),
        textColor = c(0xF5ECFF), specialTextColor = c(0xC3A9F0), accentTextColor = c(0x2A0A22),
        hintColor = c(0x8B72BD), keyRadiusDp = 12f,
    )
    val sunset = KeyboardTheme(
        id = "sunset", name = "Sunset", builtIn = true,
        background = c(0x2B1620), keyColor = c(0x45232F), keyPressedColor = c(0x6B3545),
        specialKeyColor = c(0x361B26), accentColor = c(0xFF9A5C),
        textColor = c(0xFFEFE6), specialTextColor = c(0xE8B5A0), accentTextColor = c(0x2B1206),
        hintColor = c(0xA8756A),
    )
    val mint = KeyboardTheme(
        id = "mint", name = "Mint", builtIn = true,
        background = c(0xDDEFE8), keyColor = c(0xF7FFFB), keyPressedColor = c(0xB9DDD0),
        specialKeyColor = c(0xBFDCD0), accentColor = c(0x14A37F),
        textColor = c(0x10302A), specialTextColor = c(0x2E5C50), accentTextColor = c(0xFFFFFF),
        hintColor = c(0x6E9A8C), keyRadiusDp = 16f,
    )
    val rose = KeyboardTheme(
        id = "rose", name = "Rosé", builtIn = true,
        background = c(0xF6E4E8), keyColor = c(0xFFF8F9), keyPressedColor = c(0xEBC4CE),
        specialKeyColor = c(0xEBCDD5), accentColor = c(0xE0486F),
        textColor = c(0x3A1822), specialTextColor = c(0x7A3F50), accentTextColor = c(0xFFFFFF),
        hintColor = c(0xB07A88), keyRadiusDp = 18f,
    )
    val slate = KeyboardTheme(
        id = "slate", name = "Slate", builtIn = true,
        background = c(0x2A2F38), keyColor = c(0x3A404C), keyPressedColor = c(0x535B6B),
        specialKeyColor = c(0x31363F), accentColor = c(0xFFC857),
        textColor = c(0xF1F3F7), specialTextColor = c(0xB5BBC9), accentTextColor = c(0x2A1F00),
        hintColor = c(0x7F8798), keyRadiusDp = 6f, keyGapDp = 4f,
    )
    val ocean = KeyboardTheme(
        id = "ocean", name = "Ocean", builtIn = true,
        background = c(0x0B2239), keyColor = c(0x14385A), keyPressedColor = c(0x1F5C93),
        specialKeyColor = c(0x0F2D49), accentColor = c(0x3DD6F5),
        textColor = c(0xE6F6FF), specialTextColor = c(0x96C4E0), accentTextColor = c(0x00232B),
        hintColor = c(0x5F8CAB), borderColor = c(0x1F5C93), borderWidthDp = 0.5f,
    )

    val default = midnight
    val presets: List<KeyboardTheme> = listOf(midnight, snow, amoled, aurora, sunset, mint, rose, slate, ocean)
}

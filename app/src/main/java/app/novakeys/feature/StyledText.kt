package app.novakeys.feature

/**
 * Converts plain text into decorative Unicode "fonts" (bold, italic, script, etc.).
 * These are real characters, so they work in any app without needing a font installed.
 */
object StyledText {

    data class Style(val id: String, val label: String, val convert: (String) -> String)

    private fun mapped(upper: Int, lower: Int, digit: Int? = null, exceptions: Map<Char, Int> = emptyMap()): (String) -> String = { s ->
        val sb = StringBuilder()
        for (ch in s) {
            when {
                exceptions.containsKey(ch) -> sb.appendCodePoint(exceptions.getValue(ch))
                ch in 'A'..'Z' && upper != 0 -> sb.appendCodePoint(upper + (ch - 'A'))
                ch in 'a'..'z' && lower != 0 -> sb.appendCodePoint(lower + (ch - 'a'))
                ch in '0'..'9' && digit != null -> sb.appendCodePoint(digit + (ch - '0'))
                else -> sb.append(ch)
            }
        }
        sb.toString()
    }

    private val circled: (String) -> String = { s ->
        buildString {
            for (ch in s) when (ch) {
                in 'A'..'Z' -> appendCodePoint(0x24B6 + (ch - 'A'))
                in 'a'..'z' -> appendCodePoint(0x24D0 + (ch - 'a'))
                '0' -> appendCodePoint(0x24EA)
                in '1'..'9' -> appendCodePoint(0x2460 + (ch - '1'))
                else -> append(ch)
            }
        }
    }

    private val fullWidth: (String) -> String = { s ->
        buildString {
            for (ch in s) when {
                ch == ' ' -> append('　')
                ch in '!'..'~' -> appendCodePoint(0xFF01 + (ch - '!'))
                else -> append(ch)
            }
        }
    }

    private val smallCaps: (String) -> String = { s ->
        val map = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ"
        buildString { for (ch in s) if (ch in 'a'..'z') append(map[ch - 'a']) else append(ch) }
    }

    private val upsideDown: (String) -> String = { s ->
        val from = "abcdefghijklmnopqrstuvwxyz.,'!?()[]{}_&;"
        val to = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz˙'‘¡¿)(][}{‾⅋؛"
        buildString { for (ch in s.reversed()) { val i = from.indexOf(ch.lowercaseChar()); append(if (i >= 0) to[i] else ch) } }
    }

    private val squared: (String) -> String = { s ->
        buildString {
            for (ch in s) when (ch) {
                in 'A'..'Z' -> appendCodePoint(0x1F130 + (ch - 'A'))
                in 'a'..'z' -> appendCodePoint(0x1F130 + (ch - 'a'))
                else -> append(ch)
            }
        }
    }

    private val strike: (String) -> String = { s -> s.map { if (it.isWhitespace()) "$it" else "$it̶" }.joinToString("") }
    private val underline: (String) -> String = { s -> s.map { if (it.isWhitespace()) "$it" else "$it̲" }.joinToString("") }
    private val spaced: (String) -> String = { s -> s.toList().joinToString(" ") }

    val styles: List<Style> = listOf(
        Style("bold", "𝐁𝐨𝐥𝐝", mapped(0x1D400, 0x1D41A, 0x1D7CE)),
        Style("italic", "𝐼𝑡𝑎𝑙𝑖𝑐", mapped(0x1D434, 0x1D44E, null, mapOf('h' to 0x210E))),
        Style("bolditalic", "𝑩𝒐𝒍𝒅 𝑰𝒕𝒂𝒍𝒊𝒄", mapped(0x1D468, 0x1D482)),
        Style("script", "𝒮𝒸𝓇𝒾𝓅𝓉", mapped(0x1D49C, 0x1D4B6, null,
            mapOf('B' to 0x212C, 'E' to 0x2130, 'F' to 0x2131, 'H' to 0x210B, 'I' to 0x2110, 'L' to 0x2112,
                'M' to 0x2133, 'R' to 0x211B, 'e' to 0x212F, 'g' to 0x210A, 'o' to 0x2134))),
        Style("fraktur", "𝔉𝔯𝔞𝔨𝔱𝔲𝔯", mapped(0x1D504, 0x1D51E, null,
            mapOf('C' to 0x212D, 'H' to 0x210C, 'I' to 0x2111, 'R' to 0x211C, 'Z' to 0x2128))),
        Style("double", "𝔻𝕠𝕦𝕓𝕝𝕖", mapped(0x1D538, 0x1D552, 0x1D7D8,
            mapOf('C' to 0x2102, 'H' to 0x210D, 'N' to 0x2115, 'P' to 0x2119, 'Q' to 0x211A, 'R' to 0x211D, 'Z' to 0x2124))),
        Style("mono", "𝙼𝚘𝚗𝚘", mapped(0x1D670, 0x1D68A, 0x1D7F6)),
        Style("sans", "𝖲𝖺𝗇𝗌", mapped(0x1D5A0, 0x1D5BA, 0x1D7E2)),
        Style("sansbold", "𝗦𝗮𝗻𝘀 𝗕𝗼𝗹𝗱", mapped(0x1D5D4, 0x1D5EE, 0x1D7EC)),
        Style("circled", "Ⓒⓘⓡⓒⓛⓔⓓ", circled),
        Style("squared", "🅂🅀🅄🄰🅁🄴🄳", squared),
        Style("fullwidth", "Ｆｕｌｌ", fullWidth),
        Style("smallcaps", "sᴍᴀʟʟ ᴄᴀᴘs", smallCaps),
        Style("upside", "ǝpᴉsdn", upsideDown),
        Style("strike", "S̶t̶r̶i̶k̶e̶", strike),
        Style("underline", "U̲n̲d̲e̲r̲", underline),
        Style("spaced", "S p a c e d", spaced),
    )

    fun apply(id: String, text: String): String = styles.firstOrNull { it.id == id }?.convert?.invoke(text) ?: text
}

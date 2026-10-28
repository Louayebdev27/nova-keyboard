package app.novakeys.ime

/** Pure text helpers used by the keyboard service. No Android classes, so they can be unit tested. */
object TextUtil {

    fun isWordChar(c: Char): Boolean = c.isLetter() || c == '\'' || c == '’' || c == 'ـ'

    /** The word the caret is touching, taken from the text before the caret. */
    fun currentWord(before: CharSequence): String {
        var i = before.length
        while (i > 0 && isWordChar(before[i - 1])) i--
        return before.substring(i)
    }

    /** The word before the current one, or null when the current word starts the sentence. */
    fun previousWord(before: CharSequence): String? {
        var i = before.length
        while (i > 0 && isWordChar(before[i - 1])) i--
        // skip a single run of spaces
        var j = i
        while (j > 0 && before[j - 1] == ' ') j--
        if (j == i) return null            // no space between, so there is no previous word to link
        var k = j
        while (k > 0 && isWordChar(before[k - 1])) k--
        val w = before.substring(k, j)
        return w.ifEmpty { null }
    }

    /**
     * The word to base a next-word guess on: the last word before the caret once the caret has
     * moved past it (after a space). Null at the start of a sentence or text.
     */
    fun lastWordForNext(before: CharSequence): String? {
        var j = before.length
        if (j == 0 || before[j - 1] != ' ') return null
        while (j > 0 && before[j - 1] == ' ') j--
        if (j == 0 || isSentenceEnd(before[j - 1])) return null
        var k = j
        while (k > 0 && isWordChar(before[k - 1])) k--
        return before.substring(k, j).ifEmpty { null }
    }

    fun isSentenceEnd(c: Char) = c == '.' || c == '!' || c == '?' || c == '؟' || c == '…'

    /** True when a double space should become ". " (letter or digit, then one space). */
    fun shouldDoubleSpacePeriod(before: CharSequence): Boolean {
        if (before.length < 2) return false
        val last = before[before.length - 1]
        val prev = before[before.length - 2]
        return last == ' ' && (prev.isLetterOrDigit() || prev == ')' || prev == '"')
    }
}

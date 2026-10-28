package app.novakeys

import app.novakeys.feature.Calc
import app.novakeys.feature.StyledText
import app.novakeys.ime.TextUtil
import app.novakeys.keyboard.KeyAction
import app.novakeys.keyboard.Layouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcTest {
    @Test fun addsAndMultiplies() = assertEquals("14", Calc.evaluate("2+3×4"))
    @Test fun parentheses() = assertEquals("20", Calc.evaluate("(2+3)×4"))
    @Test fun divisionAndDecimals() = assertEquals("2.5", Calc.evaluate("5÷2"))
    @Test fun unaryMinus() = assertEquals("-1", Calc.evaluate("2-3"))
    @Test fun unicodeMinus() = assertEquals("7", Calc.evaluate("10−3"))
    @Test fun percent() = assertEquals("0.5", Calc.evaluate("50%"))
    @Test fun commaDecimal() = assertEquals("3", Calc.evaluate("1,5+1,5"))
    @Test fun divideByZeroIsNull() = assertNull(Calc.evaluate("1÷0"))
    @Test fun garbageIsNull() = assertNull(Calc.evaluate("2+"))
    @Test fun emptyIsNull() = assertNull(Calc.evaluate(""))
    @Test fun longDecimalIsRounded() = assertEquals("0.333333333333", Calc.evaluate("1÷3"))
    @Test fun trailingExpressionStopsAtLetters() = assertEquals("12+30", Calc.trailingExpression("total is 12+30"))
}

class TextUtilTest {
    @Test fun currentWord() = assertEquals("wor", TextUtil.currentWord("hello wor"))
    @Test fun currentWordEmptyAfterSpace() = assertEquals("", TextUtil.currentWord("hello "))
    @Test fun currentWordKeepsApostrophe() = assertEquals("don't", TextUtil.currentWord("I don't"))
    @Test fun currentWordArabic() = assertEquals("مرح", TextUtil.currentWord("قال مرح"))
    @Test fun previousWord() = assertEquals("hello", TextUtil.previousWord("say hello wor"))
    @Test fun previousWordNoneAtStart() = assertNull(TextUtil.previousWord("wor"))
    @Test fun nextWordBase() = assertEquals("hello", TextUtil.lastWordForNext("say hello "))
    @Test fun nextWordNullAfterPeriod() = assertNull(TextUtil.lastWordForNext("Done. "))
    @Test fun nextWordNullWithoutSpace() = assertNull(TextUtil.lastWordForNext("hello"))
    @Test fun doubleSpaceAfterWord() = assertTrue(TextUtil.shouldDoubleSpacePeriod("hello "))
    @Test fun doubleSpaceNotAfterPunctuation() = assertTrue(!TextUtil.shouldDoubleSpacePeriod("hello. "))
    @Test fun doubleSpaceNotOnSingleSpace() = assertTrue(!TextUtil.shouldDoubleSpacePeriod(" "))
}

class StyledTextTest {
    @Test fun boldLetters() = assertEquals("𝐇𝐢", StyledText.apply("bold", "Hi"))
    @Test fun boldDigits() = assertEquals("𝟏𝟐", StyledText.apply("bold", "12"))
    @Test fun scriptHasExceptionForLowerE() = assertEquals("ℯ", StyledText.apply("script", "e"))
    @Test fun fullwidthSpaceAndLetter() = assertEquals("Ａ　ｂ", StyledText.apply("fullwidth", "A b"))
    @Test fun smallCaps() = assertEquals("ʜᴇʟʟᴏ", StyledText.apply("smallcaps", "hello"))
    @Test fun nonLatinIsUntouched() = assertEquals("مرحبا", StyledText.apply("bold", "مرحبا"))
    @Test fun unknownStyleIsIdentity() = assertEquals("abc", StyledText.apply("nope", "abc"))
    @Test fun everyStyleHandlesMixedInput() {
        StyledText.styles.forEach { s -> assertTrue(s.id, s.convert("Hello, World 123!").isNotEmpty()) }
    }
}

class LayoutsTest {
    private val bottom = listOf("symbols", "emoji", "comma", "space", "period", "enter")

    @Test fun everyLetterLayoutIsWellFormed() {
        for (lang in listOf("en", "fr", "ar")) for (nums in listOf(true, false)) {
            val l = Layouts.letters(lang, nums, bottom, null)
            assertEquals(if (nums) 5 else 4, l.rows.size)
            l.rows.forEach { r ->
                assertTrue(r.keys.isNotEmpty())
                r.keys.forEach { k -> assertTrue(k.weight > 0f); assertTrue(k.label.isNotEmpty()) }
            }
            assertTrue("$lang has a space key", l.rows.last().keys.any { it.action == KeyAction.SPACE })
            assertTrue("$lang has backspace", l.rows.any { r -> r.keys.any { it.action == KeyAction.BACKSPACE } })
        }
    }

    @Test fun englishHasThirtyLetters() {
        val l = Layouts.letters("en", false, bottom, null)
        val letters = l.rows.take(3).flatMap { it.keys }.filter { it.action == KeyAction.CHAR }
        assertEquals(26, letters.size)
        assertEquals(26, letters.map { it.label }.toSet().size)
    }

    @Test fun frenchIsAzerty() {
        val first = Layouts.letters("fr", false, bottom, null).rows[0].keys.joinToString("") { it.label }
        assertEquals("azertyuiop", first)
    }

    @Test fun arabicIsRtlAndNotShiftable() {
        val l = Layouts.letters("ar", false, bottom, null)
        assertTrue(l.rtl); assertTrue(!l.shiftable)
    }

    @Test fun arabicHasNoDuplicateLetters() {
        val letters = Layouts.letters("ar", false, bottom, null).rows.take(3).flatMap { it.keys }
            .filter { it.action == KeyAction.CHAR }.map { it.label }
        assertEquals(letters.size, letters.toSet().size)
    }

    @Test fun bottomRowAlwaysHasSpace() {
        val row = Layouts.bottomRow(listOf("emoji", "enter"), false, "en", null)
        assertTrue(row.keys.any { it.action == KeyAction.SPACE })
    }

    @Test fun symbolsAndNumpadAndCalc() {
        assertTrue(Layouts.symbols(1, true, bottom, null).rows.size == 5)
        assertTrue(Layouts.symbols(2, false, bottom, null).rows.size == 4)
        assertEquals(4, Layouts.numpad(null).rows.size)
        assertEquals(5, Layouts.calculator().rows.size)
        assertTrue(Layouts.calculator().rows.flatMap { it.keys }.any { it.action == KeyAction.EQUALS })
    }

    @Test fun enterLabelIsUsed() {
        val k = Layouts.letters("en", false, bottom, "Send").rows.last().keys.first { it.action == KeyAction.ENTER }
        assertEquals("Send", k.label)
    }
}

package app.novakeys.feature

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Tiny arithmetic evaluator for the calculator keyboard.
 * Supports + - × ÷ * / ( ), decimals, unary minus, and a trailing % (meaning /100).
 */
object Calc {
    private val OPS = "0123456789+-−×÷*/().,% "

    /** Longest tail of [before] that looks like an expression, or empty. */
    fun trailingExpression(before: CharSequence): String {
        var i = before.length
        while (i > 0 && before[i - 1] in OPS) i--
        return before.substring(i).trimStart()
    }

    /** Returns the formatted result, or null if the expression is not valid. */
    fun evaluate(expr: String): String? {
        val src = expr.trim().trimEnd('=').replace(',', '.')
        if (src.isEmpty() || src.none { it.isDigit() }) return null
        return runCatching {
            val p = Parser(src)
            val v = p.parseExpr()
            p.skipWs()
            if (!p.done()) return null
            format(v)
        }.getOrNull()
    }

    private fun format(v: BigDecimal): String {
        val r = v.round(MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros()
        return r.toPlainString()
    }

    private class Parser(val s: String) {
        var i = 0
        fun done() = i >= s.length
        fun skipWs() { while (i < s.length && s[i] == ' ') i++ }

        fun parseExpr(): BigDecimal {
            var v = parseTerm()
            while (true) {
                skipWs()
                when {
                    i < s.length && s[i] == '+' -> { i++; v += parseTerm() }
                    i < s.length && (s[i] == '-' || s[i] == '−') -> { i++; v -= parseTerm() }
                    else -> return v
                }
            }
        }

        fun parseTerm(): BigDecimal {
            var v = parseFactor()
            while (true) {
                skipWs()
                when {
                    i < s.length && (s[i] == '×' || s[i] == '*') -> { i++; v *= parseFactor() }
                    i < s.length && (s[i] == '÷' || s[i] == '/') -> {
                        i++
                        val d = parseFactor()
                        if (d.signum() == 0) throw ArithmeticException("div0")
                        v = v.divide(d, MathContext(20, RoundingMode.HALF_UP))
                    }
                    else -> return v
                }
            }
        }

        fun parseFactor(): BigDecimal {
            skipWs()
            var v: BigDecimal
            if (i < s.length && (s[i] == '-' || s[i] == '−')) { i++; return parseFactor().negate() }
            if (i < s.length && s[i] == '+') { i++; return parseFactor() }
            if (i < s.length && s[i] == '(') {
                i++
                v = parseExpr()
                skipWs()
                if (i < s.length && s[i] == ')') i++ else throw IllegalArgumentException("paren")
            } else {
                val start = i
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                if (start == i) throw IllegalArgumentException("number")
                v = BigDecimal(s.substring(start, i))
            }
            skipWs()
            while (i < s.length && s[i] == '%') { i++; v = v.divide(BigDecimal(100)); skipWs() }
            return v
        }
    }
}

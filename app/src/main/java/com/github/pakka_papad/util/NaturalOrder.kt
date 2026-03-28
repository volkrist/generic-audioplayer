package com.github.pakka_papad.util

/**
 * Human-friendly ("natural") string order: numeric substrings compare as numbers,
 * so e.g. "Глава 2" sorts before "Глава 10". Text segments compare case-insensitively.
 */
object NaturalOrder {

    val stringComparator: Comparator<String> = Comparator { a, b -> compareNatural(a, b) }

    fun compareNatural(a: String?, b: String?): Int {
        val s1 = a.orEmpty()
        val s2 = b.orEmpty()
        var i = 0
        var j = 0
        while (i < s1.length && j < s2.length) {
            val c1 = s1[i]
            val c2 = s2[j]
            when {
                c1.isDigit() && c2.isDigit() -> {
                    val n1 = readNumber(s1, i)
                    val n2 = readNumber(s2, j)
                    val cmp = compareNumberTokens(n1.first, n2.first)
                    if (cmp != 0) return cmp
                    i = n1.second
                    j = n2.second
                }
                !c1.isDigit() && !c2.isDigit() -> {
                    val t1 = readTextToken(s1, i)
                    val t2 = readTextToken(s2, j)
                    val cmp = t1.first.compareTo(t2.first, ignoreCase = true)
                    if (cmp != 0) return cmp
                    i = t1.second
                    j = t2.second
                }
                else -> {
                    val cmp = c1.compareTo(c2)
                    if (cmp != 0) return cmp
                    i++
                    j++
                }
            }
        }
        return (s1.length - i) - (s2.length - j)
    }

    private fun readNumber(s: String, start: Int): Pair<String, Int> {
        var end = start
        while (end < s.length && s[end].isDigit()) end++
        return s.substring(start, end) to end
    }

    /**
     * Compare digit-only tokens as integers; avoids overflow by falling back to
     * length / lexicographic order for huge values.
     */
    private fun compareNumberTokens(a: String, b: String): Int {
        if (a.isEmpty() && b.isEmpty()) return 0
        val trimA = a.trimStart('0').ifEmpty { "0" }
        val trimB = b.trimStart('0').ifEmpty { "0" }
        val lenCmp = trimA.length.compareTo(trimB.length)
        if (lenCmp != 0) return lenCmp
        val longA = trimA.toLongOrNull()
        val longB = trimB.toLongOrNull()
        if (longA != null && longB != null) {
            return longA.compareTo(longB)
        }
        return trimA.compareTo(trimB)
    }

    private fun readTextToken(s: String, start: Int): Pair<String, Int> {
        var end = start
        while (end < s.length && !s[end].isDigit()) end++
        return s.substring(start, end) to end
    }
}

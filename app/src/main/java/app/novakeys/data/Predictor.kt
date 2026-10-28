package app.novakeys.data

import android.content.Context
import java.io.File
import java.util.PriorityQueue
import java.util.TreeMap

/**
 * Offline word suggestions. Two sources are combined:
 *  - a bundled word list (assets/words_<lang>.txt, one word per line, most frequent first), and
 *  - what the user types, learned on the device and kept in app storage only.
 *
 * To use a bigger dictionary, replace the asset file with any open-licensed frequency list in the same format.
 */
class Predictor(private val context: Context, val lang: String) {

    private val seed = TreeMap<String, Int>()
    private var seedTop: List<String> = emptyList()
    private val learned = TreeMap<String, Int>()
    private val bigrams = HashMap<String, HashMap<String, Int>>()
    private val file = File(context.filesDir, "predictor_$lang.txt")
    private var loaded = false
    private var dirty = 0

    companion object {
        const val LEARN_BOOST = 30
        const val MAX_LEARNED = 20000
    }

    @Synchronized
    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val words = context.assets.open("words_$lang.txt").bufferedReader().readLines()
                .map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val n = words.size.coerceAtLeast(1)
            words.forEachIndexed { i, w -> if (w !in seed) seed[w] = 1 + (100L * (n - i) / n).toInt() }
            seedTop = words.distinct().take(60)
        }
        if (file.exists()) runCatching {
            file.forEachLine { line ->
                val p = line.split('\t')
                when {
                    p.size == 3 && p[0] == "U" -> learned[p[1]] = p[2].toIntOrNull() ?: 0
                    p.size == 4 && p[0] == "B" ->
                        bigrams.getOrPut(p[1]) { HashMap() }[p[2]] = p[3].toIntOrNull() ?: 1
                }
            }
        }
    }

    private fun score(word: String): Int = (seed[word] ?: 0) + (learned[word] ?: 0)

    /** Completions for the word being typed. */
    @Synchronized
    fun complete(prefix: String, max: Int = 3): List<String> {
        ensureLoaded()
        val p = prefix.lowercase()
        if (p.isEmpty()) return emptyList()
        val candidates = HashSet<String>()
        candidates += seed.subMap(p, true, p + '￿', true).keys
        candidates += learned.subMap(p, true, p + '￿', true).keys
        candidates.remove(p)
        return topK(candidates, max).map { matchCase(prefix, it) }
    }

    /** Likely next words after [previous] (null or blank at the start of a sentence). */
    @Synchronized
    fun nextWords(previous: String?, max: Int = 3): List<String> {
        ensureLoaded()
        val prev = previous?.lowercase()?.takeIf { it.isNotBlank() }
        val fromBigrams = prev?.let { bigrams[it] }
        if (fromBigrams != null && fromBigrams.isNotEmpty()) {
            return fromBigrams.entries.sortedByDescending { it.value }.take(max).map { it.key }
        }
        // Fall back to the most common words overall.
        val pool = HashSet<String>()
        pool += seedTop
        pool += learned.entries.sortedByDescending { it.value }.take(20).map { it.key }
        val out = topK(pool, max)
        return if (previous == null) out.map { it.replaceFirstChar { c -> c.uppercase() } } else out
    }

    private fun topK(words: Collection<String>, k: Int): List<String> {
        val heap = PriorityQueue<String>(k + 1) { a, b -> score(a).compareTo(score(b)) }
        for (w in words) {
            heap.add(w)
            if (heap.size > k) heap.poll()
        }
        return heap.sortedByDescending { score(it) }
    }

    private fun matchCase(typed: String, suggestion: String): String = when {
        typed.length > 1 && typed.all { !it.isLetter() || it.isUpperCase() } -> suggestion.uppercase()
        typed.first().isUpperCase() -> suggestion.replaceFirstChar { it.uppercase() }
        else -> suggestion
    }

    @Synchronized
    fun learn(previous: String?, word: String) {
        ensureLoaded()
        val w = word.lowercase()
        if (w.length < 2 || w.none { it.isLetter() }) return
        learned[w] = (learned[w] ?: 0) + LEARN_BOOST
        previous?.lowercase()?.takeIf { it.isNotBlank() }?.let { prev ->
            val m = bigrams.getOrPut(prev) { HashMap() }
            m[w] = (m[w] ?: 0) + 1
        }
        if (learned.size > MAX_LEARNED) prune()
        if (++dirty >= 25) save()
    }

    private fun prune() {
        val drop = learned.entries.sortedBy { it.value }.take(learned.size / 10).map { it.key }
        drop.forEach { learned.remove(it) }
    }

    @Synchronized
    fun save() {
        if (!loaded) return
        dirty = 0
        runCatching {
            val tmp = File(file.path + ".tmp")
            tmp.bufferedWriter().use { w ->
                learned.forEach { (k, v) -> w.write("U\t$k\t$v\n") }
                bigrams.forEach { (prev, m) -> m.forEach { (n, c) -> w.write("B\t$prev\t$n\t$c\n") } }
            }
            tmp.renameTo(file)
        }
    }

    // ---- management used by the settings screen --------------------------------------------

    @Synchronized
    fun learnedWords(): List<String> {
        ensureLoaded()
        return learned.entries.sortedByDescending { it.value }.map { it.key }
    }

    @Synchronized
    fun addWord(word: String) {
        ensureLoaded()
        val w = word.trim().lowercase()
        if (w.isNotEmpty()) { learned[w] = (learned[w] ?: 0) + LEARN_BOOST * 3; save() }
    }

    @Synchronized
    fun removeWord(word: String) {
        ensureLoaded()
        learned.remove(word.lowercase())
        bigrams.remove(word.lowercase())
        bigrams.values.forEach { it.remove(word.lowercase()) }
        save()
    }

    @Synchronized
    fun clearLearned() {
        ensureLoaded()
        learned.clear()
        bigrams.clear()
        save()
    }
}

object Predictors {
    private val map = HashMap<String, Predictor>()

    @Synchronized
    fun get(context: Context, lang: String): Predictor =
        map.getOrPut(lang) { Predictor(context.applicationContext, lang) }

    @Synchronized
    fun saveAll() = map.values.forEach { it.save() }
}

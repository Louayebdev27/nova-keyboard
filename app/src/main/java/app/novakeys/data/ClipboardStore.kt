package app.novakeys.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ClipItem(val text: String, val pinned: Boolean, val time: Long)

/**
 * Clipboard history kept on the device. Unpinned entries are capped and can be wiped in one tap.
 * Nothing is sent anywhere.
 */
class ClipboardStore(context: Context) {
    private val file = File(context.filesDir, "clipboard.json")
    private var items: MutableList<ClipItem>? = null

    companion object { const val MAX_UNPINNED = 40 }

    @Synchronized
    fun all(): List<ClipItem> {
        items?.let { return it.sortedWith(compareByDescending<ClipItem> { it.pinned }.thenByDescending { it.time }) }
        val list = mutableListOf<ClipItem>()
        runCatching {
            if (file.exists()) {
                val arr = JSONArray(file.readText())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list += ClipItem(o.getString("t"), o.optBoolean("p"), o.optLong("ts"))
                }
            }
        }
        items = list
        return all()
    }

    @Synchronized
    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty() || t.length > 5000) return
        all()
        val list = items!!
        val existing = list.indexOfFirst { it.text == t }
        val pinned = existing >= 0 && list[existing].pinned
        if (existing >= 0) list.removeAt(existing)
        list += ClipItem(t, pinned, System.currentTimeMillis())
        val unpinned = list.filter { !it.pinned }.sortedByDescending { it.time }
        if (unpinned.size > MAX_UNPINNED) list.removeAll(unpinned.drop(MAX_UNPINNED).toSet())
        save()
    }

    @Synchronized
    fun togglePin(item: ClipItem) {
        all()
        val i = items!!.indexOfFirst { it.text == item.text }
        if (i >= 0) { items!![i] = items!![i].copy(pinned = !items!![i].pinned); save() }
    }

    @Synchronized
    fun remove(item: ClipItem) {
        all()
        items!!.removeAll { it.text == item.text }
        save()
    }

    @Synchronized
    fun clearUnpinned() {
        all()
        items!!.removeAll { !it.pinned }
        save()
    }

    private fun save() {
        val arr = JSONArray()
        items?.forEach { arr.put(JSONObject().put("t", it.text).put("p", it.pinned).put("ts", it.time)) }
        file.writeText(arr.toString())
    }
}

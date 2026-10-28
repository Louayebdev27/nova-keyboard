package app.novakeys.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Text shortcuts: type "omw" then space and it becomes "On my way!". Stored locally as JSON. */
class ShortcutStore(context: Context) {
    private val file = File(context.filesDir, "shortcuts.json")
    private var cache: LinkedHashMap<String, String>? = null

    @Synchronized
    fun all(): Map<String, String> {
        cache?.let { return it }
        val m = LinkedHashMap<String, String>()
        runCatching {
            if (file.exists()) {
                val arr = JSONArray(file.readText())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    m[o.getString("k")] = o.getString("v")
                }
            }
        }
        cache = m
        return m
    }

    @Synchronized
    fun expand(trigger: String): String? = all()[trigger.lowercase()]

    @Synchronized
    fun put(trigger: String, text: String) {
        val k = trigger.trim().lowercase()
        if (k.isEmpty() || text.isEmpty() || k.any { it.isWhitespace() }) return
        all() as LinkedHashMap
        cache!![k] = text
        save()
    }

    @Synchronized
    fun remove(trigger: String) {
        all()
        cache!!.remove(trigger.lowercase())
        save()
    }

    private fun save() {
        val arr = JSONArray()
        cache?.forEach { (k, v) -> arr.put(JSONObject().put("k", k).put("v", v)) }
        file.writeText(arr.toString())
    }

    fun toJson(): JSONArray = JSONArray().also { a ->
        all().forEach { (k, v) -> a.put(JSONObject().put("k", k).put("v", v)) }
    }

    fun fromJson(arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            put(o.getString("k"), o.getString("v"))
        }
    }
}

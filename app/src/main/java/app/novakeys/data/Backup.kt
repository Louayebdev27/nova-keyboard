package app.novakeys.data

import android.content.Context
import android.net.Uri
import app.novakeys.theme.ThemeStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local backup: one JSON file the user saves wherever they like (Files app, cloud drive, USB).
 * It replaces the old cloud-account backup, so nothing leaves the phone unless the user moves the file.
 * Background images are not included (they can be large); themes keep their colours and shape.
 */
object Backup {
    private const val VERSION = 1

    fun export(context: Context, target: Uri): Boolean = runCatching {
        val prefs = Prefs.get(context)
        val root = JSONObject()
        root.put("version", VERSION)

        val settings = JSONObject()
        prefs.sp.all.forEach { (k, v) ->
            when (v) {
                is Boolean, is Int, is Long, is String -> settings.put(k, v)
                is Float -> settings.put(k, v.toDouble())
            }
        }
        root.put("settings", settings)

        val themes = JSONArray()
        ThemeStore(context).custom().forEach { themes.put(it.toJson()) }
        root.put("themes", themes)

        root.put("shortcuts", ShortcutStore(context).toJson())

        val learned = JSONObject()
        prefs.languages.forEach { l -> learned.put(l, JSONArray(Predictors.get(context, l).learnedWords())) }
        root.put("learned", learned)

        context.contentResolver.openOutputStream(target, "wt")!!.use { it.write(root.toString(2).toByteArray()) }
        true
    }.getOrDefault(false)

    /** Returns a short human summary, or null if the file is not a valid backup. */
    fun import(context: Context, source: Uri): String? = runCatching {
        val text = context.contentResolver.openInputStream(source)!!.use { it.readBytes().decodeToString() }
        val root = JSONObject(text)
        require(root.optInt("version", 0) in 1..VERSION)
        val prefs = Prefs.get(context)

        val settings = root.optJSONObject("settings")
        val ed = prefs.sp.edit()
        settings?.keys()?.forEach { k ->
            when (val v = settings.get(k)) {
                is Boolean -> ed.putBoolean(k, v)
                is Int -> ed.putInt(k, v)
                is Long -> ed.putLong(k, v)
                is String -> ed.putString(k, v)
                is Double -> ed.putFloat(k, v.toFloat())
            }
        }
        ed.apply()

        val store = ThemeStore(context)
        val themes = root.optJSONArray("themes") ?: JSONArray()
        var themeCount = 0
        for (i in 0 until themes.length()) {
            val t = app.novakeys.theme.KeyboardTheme.fromJson(themes.getJSONObject(i))
            store.save(t.copy(builtIn = false, backgroundImage = null)); themeCount++
        }

        val sc = root.optJSONArray("shortcuts") ?: JSONArray()
        ShortcutStore(context).fromJson(sc)

        var words = 0
        root.optJSONObject("learned")?.let { l ->
            l.keys().forEach { lang ->
                val arr = l.getJSONArray(lang)
                for (i in 0 until arr.length()) { Predictors.get(context, lang).addWord(arr.getString(i)); words++ }
            }
        }
        "Restored settings, $themeCount themes, ${sc.length()} shortcuts, $words words."
    }.getOrNull()
}

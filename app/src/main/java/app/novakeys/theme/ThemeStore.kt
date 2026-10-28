package app.novakeys.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import app.novakeys.data.Prefs
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Built-in presets plus user-made themes saved as small JSON files in app storage. */
class ThemeStore(private val context: Context) {
    private val dir = File(context.filesDir, "themes").apply { mkdirs() }

    fun custom(): List<KeyboardTheme> =
        (dir.listFiles { f -> f.extension == "json" } ?: emptyArray())
            .sortedBy { it.lastModified() }
            .mapNotNull { runCatching { KeyboardTheme.fromJson(JSONObject(it.readText())) }.getOrNull() }

    fun all(): List<KeyboardTheme> = Themes.presets + custom()

    fun find(id: String): KeyboardTheme? = all().firstOrNull { it.id == id }

    fun active(): KeyboardTheme = find(Prefs.get(context).activeTheme) ?: Themes.default

    fun setActive(id: String) { Prefs.get(context).activeTheme = id }

    fun save(theme: KeyboardTheme): KeyboardTheme {
        val t = if (theme.builtIn || theme.id.isBlank()) theme.copy(id = newId(), builtIn = false) else theme
        File(dir, "${t.id}.json").writeText(t.toJson().toString())
        return t
    }

    fun delete(id: String) {
        find(id)?.let { t -> backgroundFile(t)?.delete() }
        File(dir, "$id.json").delete()
        if (Prefs.get(context).activeTheme == id) setActive(Themes.default.id)
    }

    fun export(theme: KeyboardTheme): String = theme.toJson().toString(2)

    /** Imports a theme exported by [export]. Returns null if the text is not a theme. */
    fun import(json: String): KeyboardTheme? = runCatching {
        val parsed = KeyboardTheme.fromJson(JSONObject(json))
        save(parsed.copy(id = newId(), builtIn = false, backgroundImage = null))
    }.getOrNull()

    fun backgroundFile(theme: KeyboardTheme): File? =
        theme.backgroundImage?.let { File(dir, it) }?.takeIf { it.exists() }

    /** Copies a picked image into app storage, scaled down so the keyboard stays light. */
    fun storeBackground(themeId: String, source: Uri, maxEdge: Int = 1280): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        // A new file name each time, so cached previews never show an old picture.
        val name = "$themeId-${System.currentTimeMillis()}.jpg"
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bmp.recycle()
        dir.listFiles { f -> f.name.startsWith("$themeId-") && f.name != name }?.forEach { it.delete() }
        name
    }.getOrNull()

    fun newId(): String = "custom-" + UUID.randomUUID().toString().take(8)
}

package app.novakeys.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/** User-imported sticker images, scaled to a sane size and kept in app storage. */
class StickerStore(private val context: Context) {
    private val dir = File(context.filesDir, "stickers").apply { mkdirs() }

    fun all(): List<File> =
        (dir.listFiles { f -> f.extension == "png" } ?: emptyArray()).sortedByDescending { it.lastModified() }

    fun import(source: Uri, maxEdge: Int = 512): File? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val bmp = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val out = File(dir, UUID.randomUUID().toString().take(10) + ".png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        out
    }.getOrNull()

    fun delete(file: File) { file.delete() }
}

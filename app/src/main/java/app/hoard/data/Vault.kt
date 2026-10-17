package app.hoard.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.max

/**
 * Where the moments themselves live: photos and clips in the app's own files, never in the shared
 * gallery. Each has a small still beside it (the clip's first frame, or the photo shrunk) for the art.
 */
class Vault(private val context: Context) {
    private val dir = File(context.filesDir, "vault").apply { mkdirs() }
    private val stills = File(dir, "stills").apply { mkdirs() }
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun file(media: String) = File(dir, media)
    private fun stillFile(media: String) = File(stills, media.substringBeforeLast('.') + ".jpg")

    /** A fresh file in the cache for the camera to write into. */
    fun scratch(ext: String): File = File(context.cacheDir, "capture").apply { mkdirs() }.let { File(it, "m-" + System.currentTimeMillis() + "." + ext) }

    /** Copies a picked photo or clip into the cache, so it can be looked at before it goes to the forge. */
    fun importToScratch(uri: Uri, video: Boolean): File? = runCatching {
        val out = scratch(if (video) "mp4" else "jpg")
        context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(out).use { input.copyTo(it) } } ?: return null
        out
    }.getOrNull()

    /** Moves a captured file into the vault and makes its still. Returns the vault name. */
    fun keep(src: File, video: Boolean): String {
        val name = UUID.randomUUID().toString() + if (video) ".mp4" else ".jpg"
        val dst = file(name)
        if (video) {
            if (!src.renameTo(dst)) { src.copyTo(dst, overwrite = true); src.delete() }
        } else {
            // Photos are stored upright and no bigger than they need to be.
            val bmp = decodeUpright(src, 2048)
            if (bmp != null) {
                FileOutputStream(dst).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                bmp.recycle()
                src.delete()
            } else if (!src.renameTo(dst)) { src.copyTo(dst, overwrite = true); src.delete() }
        }
        makeStill(name, video)
        return name
    }

    private fun makeStill(name: String, video: Boolean) {
        val bmp = (if (video) frameOf(file(name)) else decodeUpright(file(name), 640)) ?: return
        val s = scaleDown(bmp, 640)
        FileOutputStream(stillFile(name)).use { s.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (s !== bmp) s.recycle()
        bmp.recycle()
    }

    /** The small picture for art and lists, from the cache when it can. */
    fun still(media: String): Bitmap? {
        cache.get(media)?.let { return it }
        val f = stillFile(media)
        if (!f.exists()) {
            if (!file(media).exists()) return null
            makeStill(media, media.endsWith(".mp4"))
        }
        val b = BitmapFactory.decodeFile(f.path) ?: return null
        cache.put(media, b)
        return b
    }

    /** A picture of a file still in the cache (for the forge screen, before it is kept). */
    fun preview(f: File, video: Boolean): Bitmap? = if (video) frameOf(f)?.let { scaleDown(it, 640) } else decodeUpright(f, 640)

    /** The full photo, for looking at. */
    fun full(media: String, max: Int = 2048): Bitmap? = decodeUpright(file(media), max)

    fun durationMs(f: File): Long = runCatching {
        MediaMetadataRetriever().run {
            try {
                setDataSource(f.path)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally { release() }
        }
    }.getOrDefault(0L)

    /** When a photo was taken, from its EXIF, or null. */
    fun takenAt(f: File): Long? = runCatching {
        val exif = ExifInterface(f.path)
        val s = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME) ?: return null
        val fmt = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getDefault() }
        fmt.parse(s)?.time
    }.getOrNull()

    /** Where a photo was taken, from its EXIF, or null. */
    fun placeOf(f: File): Pair<Double, Double>? = runCatching {
        ExifInterface(f.path).latLong?.let { it[0] to it[1] }
    }.getOrNull()

    fun delete(media: String) {
        file(media).delete()
        stillFile(media).delete()
        cache.remove(media)
    }

    fun all(): List<File> = dir.listFiles()?.filter { it.isFile }.orEmpty()

    fun wipe() {
        dir.listFiles()?.forEach { if (it.isFile) it.delete() }
        stills.listFiles()?.forEach { it.delete() }
        cache.evictAll()
    }

    /** Takes in the files of a restored backup; their stills are made again when first needed. */
    fun adopt(from: File) {
        from.listFiles()?.forEach { f ->
            if (!f.isFile) return@forEach
            if (!f.renameTo(file(f.name))) { f.copyTo(file(f.name), overwrite = true); f.delete() }
        }
        from.deleteRecursively()
    }

    private fun frameOf(f: File): Bitmap? = runCatching {
        MediaMetadataRetriever().run {
            try {
                setDataSource(f.path)
                getFrameAtTime(300_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: frameAtTime
            } finally { release() }
        }
    }.getOrNull()

    companion object {
        fun scaleDown(b: Bitmap, max: Int): Bitmap {
            val m = max(b.width, b.height)
            if (m <= max) return b
            val k = max.toFloat() / m
            return Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true)
        }

        /** Decodes with subsampling and turns it the way EXIF says. */
        fun decodeUpright(f: File, max: Int): Bitmap? {
            if (!f.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= max) sample *= 2
            val raw = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            val rot = runCatching { ExifInterface(f.path).rotationDegrees }.getOrDefault(0)
            val scaled = scaleDown(raw, max)
            if (scaled !== raw) raw.recycle()
            if (rot == 0) return scaled
            val m = Matrix().apply { postRotate(rot.toFloat()) }
            val r = Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, m, true)
            if (r !== scaled) scaled.recycle()
            return r
        }
    }
}

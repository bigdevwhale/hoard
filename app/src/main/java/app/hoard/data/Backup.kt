package app.hoard.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The whole hoard in one zip: a JSON list of treasures and every photo and clip. Nothing leaves the
 * phone unless the keeper exports it.
 */
object Backup {
    private const val VERSION = 1
    private const val INDEX = "hoard.json"

    fun encode(all: List<Treasure>): String = JSONObject()
        .put("app", "hoard")
        .put("version", VERSION)
        .put("treasures", JSONArray(all.map {
            JSONObject().put("id", it.id).put("kind", it.kind).put("title", it.title).put("note", it.note).put("hall", it.hall)
                .put("metal", it.metal).put("stone", it.stone).put("cut", it.cut).put("relic", it.relic).put("media", it.media)
                .put("video", it.video).put("durationMs", it.durationMs).put("takenAt", it.takenAt).put("momentAt", it.momentAt)
                .put("revealAt", it.revealAt).put("seen", it.seen).put("place", it.place).put("lat", it.lat ?: JSONObject.NULL)
                .put("lon", it.lon ?: JSONObject.NULL).put("sky", it.sky).put("tempC", it.tempC ?: JSONObject.NULL).put("quest", it.quest)
                .put("sealedUntil", it.sealedUntil ?: JSONObject.NULL).put("opened", it.opened).put("seed", it.seed).put("favorite", it.favorite)
        }))
        .toString(2)

    /** Throws [IllegalArgumentException] when the text is not a Hoard index. */
    fun decode(text: String): List<Treasure> {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "hoard") { "Not a Hoard backup" }
        val arr = root.optJSONArray("treasures") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            arr.getJSONObject(i).run {
                Treasure(
                    id = getLong("id"), kind = getString("kind"), title = optString("title"), note = optString("note"), hall = getString("hall"),
                    metal = optString("metal", "GOLD"), stone = optString("stone", "RUBY"), cut = optString("cut", "ROUND"), relic = optString("relic", "CROWN"),
                    media = getString("media"), video = optBoolean("video"), durationMs = optLong("durationMs"), takenAt = getLong("takenAt"),
                    momentAt = optLong("momentAt", getLong("takenAt")), revealAt = optLong("revealAt", getLong("takenAt")), seen = optBoolean("seen", true),
                    place = optString("place"), lat = optD("lat"), lon = optD("lon"), sky = optString("sky", "UNKNOWN"), tempC = optI("tempC"),
                    quest = optString("quest"), sealedUntil = optL("sealedUntil"), opened = optBoolean("opened"), seed = optInt("seed"), favorite = optBoolean("favorite"),
                )
            }
        }
    }

    private fun JSONObject.optL(key: String): Long? = if (!has(key) || isNull(key)) null else getLong(key)
    private fun JSONObject.optI(key: String): Int? = if (!has(key) || isNull(key)) null else getInt(key)
    private fun JSONObject.optD(key: String): Double? = if (!has(key) || isNull(key)) null else getDouble(key)

    fun write(context: Context, uri: Uri, all: List<Treasure>, vault: Vault) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            ZipOutputStream(os.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(INDEX))
                zip.write(encode(all).toByteArray())
                zip.closeEntry()
                all.forEach { t ->
                    val f = vault.file(t.media)
                    if (!f.exists()) return@forEach
                    // Photos and clips are already compressed; storing them saves time.
                    zip.putNextEntry(ZipEntry("media/" + t.media))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("Can't write there")
    }

    /** Reads a backup into a fresh folder first, so a broken file never wipes the hoard. */
    fun read(context: Context, uri: Uri): Pair<List<Treasure>, File> {
        val tmp = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var index: String? = null
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    when {
                        e.name == INDEX -> index = zip.readBytes().decodeToString()
                        e.name.startsWith("media/") && !e.isDirectory -> {
                            val name = e.name.removePrefix("media/")
                            require(!name.contains('/') && !name.contains("..")) { "Bad entry" }
                            File(tmp, name).outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
        } ?: error("Can't read that")
        return decode(index ?: throw IllegalArgumentException("Not a Hoard backup")) to tmp
    }
}

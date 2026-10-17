package app.hoard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.hoard.domain.Cut
import app.hoard.domain.Daypart
import app.hoard.domain.Hall
import app.hoard.domain.Metal
import app.hoard.domain.RelicForm
import app.hoard.domain.Sky
import app.hoard.domain.Stone
import app.hoard.ui.pix.Glyphs
import app.hoard.ui.pix.Mint
import app.hoard.ui.pix.Px
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Paints the treasures and the cave to PNG sheets (run with `-Prender`), so they can be looked at without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArt {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }
    private val photos = File(System.getProperty("render.photos") ?: "src/demo/assets/demo")

    private fun photo(name: String): Bitmap? = File(photos, "$name.jpg").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    private fun sheet(name: String, cells: List<List<Px?>>, scale: Int = 4, bg: Int = 0xFF1A1420.toInt(), pad: Int = 4) {
        val cw = cells.flatten().filterNotNull().maxOf { it.w } + pad * 2
        val ch = cells.flatten().filterNotNull().maxOf { it.h } + pad * 2
        val rows = cells.size
        val cols = cells.maxOf { it.size }
        val bmp = Bitmap.createBitmap(cols * cw * scale, rows * ch * scale, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(bg)
        cells.forEachIndexed { r, row ->
            row.forEachIndexed { c, px ->
                if (px == null) return@forEachIndexed
                val ox = c * cw + (cw - px.w) / 2
                val oy = r * ch + (ch - px.h) / 2
                for (y in 0 until px.h) for (x in 0 until px.w) {
                    val p = px.c[y * px.w + x]
                    val a = (p ushr 24) and 0xFF
                    if (a == 0) continue
                    val col = blend(bg, p, a / 255f)
                    for (j in 0 until scale) for (i in 0 until scale) bmp.setPixel((ox + x) * scale + i, (oy + y) * scale + j, col)
                }
            }
        }
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(s: Int) = (((a shr s) and 0xFF) + ((((b shr s) and 0xFF) - ((a shr s) and 0xFF)) * t)).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private val names = listOf("237", "40", "63", "129", "288", "177", "38", "152", "174", "75", "64", "195")

    @Test
    fun coins() {
        val metals = Metal.entries
        sheet("coins", listOf(
            names.take(7).mapIndexed { i, n -> Mint.coin(photo(n), metals[i % metals.size], 64) },
            names.take(7).mapIndexed { i, n -> Mint.coin(photo(n), metals[i % metals.size], 20) },
            Hall.entries.take(7).mapIndexed { i, h -> Mint.coinBack(metals[i], h, listOf(Glyphs.sky.getValue(Sky.RAIN), Glyphs.daypart.getValue(Daypart.DUSK), Glyphs.place), 2026, 64) },
        ), scale = 4)
    }

    @Test
    fun gems() {
        val stones = Stone.entries
        val cuts = Cut.entries
        sheet("gems", listOf(
            names.take(7).mapIndexed { i, n -> Mint.gem(photo(n), stones[i], cuts[i % cuts.size], 64) },
            names.drop(5).take(7).mapIndexed { i, n -> Mint.gem(photo(n), stones[(i + 3) % stones.size], cuts[(i + 2) % cuts.size], 64, tiltX = 0.6f, tiltY = 0.3f) },
            names.take(7).mapIndexed { i, n -> Mint.gem(photo(n), stones[i], cuts[i % cuts.size], 16) },
        ), scale = 4)
    }

    @Test
    fun relics() {
        val metals = Metal.entries
        sheet("relics", listOf(
            RelicForm.entries.mapIndexed { i, f -> Mint.relic(photo(names[i]), f, metals[i % metals.size], 72) },
            RelicForm.entries.mapIndexed { i, f -> Mint.relic(photo(names[i]), f, Metal.GOLD, 24) },
        ), scale = 4)
    }

    @Test
    fun dragons() {
        sheet("dragons", app.hoard.domain.Hue.entries.map { h ->
            app.hoard.domain.Stage.entries.map { app.hoard.ui.pix.Dragon.frame(h, it, 0) } + listOf(app.hoard.ui.pix.Dragon.frame(h, app.hoard.domain.Stage.GROWN, 4, awake = true))
        }, scale = 3)
    }

    private fun items(n: Int): List<app.hoard.ui.pix.PileItem> = names.take(n).mapIndexed { i, name ->
        val p = photo(name)
        val px = when (i % 4) {
            0 -> Mint.coin(p, Metal.entries[i % 3], 16)
            1 -> Mint.gem(p, Stone.entries[i % 7], Cut.entries[i % 6], 13)
            2 -> Mint.relic(p, RelicForm.entries[i % 8], Metal.GOLD, 26)
            else -> Mint.mirror(p, Metal.SILVER, 14, 18)
        }
        app.hoard.ui.pix.PileItem(i.toLong(), px, i * 7919 + 13)
    }

    @Test
    fun caves() {
        val all = app.hoard.domain.Feature.entries.toSet()
        val looks = listOf(
            app.hoard.ui.pix.CaveLook(app.hoard.domain.Hue.EMERALD, app.hoard.domain.Stage.HATCHLING, 2, emptySet(), items(2), forging = 1),
            app.hoard.ui.pix.CaveLook(app.hoard.domain.Hue.CRIMSON, app.hoard.domain.Stage.YOUNG, 20, setOf(app.hoard.domain.Feature.MUSHROOMS, app.hoard.domain.Feature.STALACTITES, app.hoard.domain.Feature.CRYSTALS), items(6), listOf(Metal.SILVER), listOf(0xFFD23A4E.toInt()), chests = 1, gnome = true),
            app.hoard.ui.pix.CaveLook(app.hoard.domain.Hue.SAPPHIRE, app.hoard.domain.Stage.GROWN, 60, all - app.hoard.domain.Feature.SKYLIGHT - app.hoard.domain.Feature.BONES, items(9), listOf(Metal.SILVER, Metal.COPPER), listOf(0xFFD23A4E.toInt(), 0xFF3A6FD8.toInt()), forging = 2, chests = 2),
            app.hoard.ui.pix.CaveLook(app.hoard.domain.Hue.OBSIDIAN, app.hoard.domain.Stage.ELDER, 150, all, items(12), listOf(Metal.SILVER, Metal.COPPER, Metal.MITHRIL), listOf(0xFFD23A4E.toInt(), 0xFF3A6FD8.toInt(), 0xFF2FA96A.toInt()), chests = 3, gnome = true, night = false),
        )
        sheet("caves", looks.chunked(2).map { row -> row.map { app.hoard.ui.pix.Cave(it).frame(0) } }, scale = 4, pad = 2)
        sheet("cave_anim", listOf(List(4) { app.hoard.ui.pix.Cave(looks[2]).frame(it * 2) }), scale = 2, pad = 2)
    }

    /** The launcher icon: a cut ruby on a little heap of coins, pixel for pixel. */
    private fun iconArt(): Px {
        val px = Px(48, 48)
        px.blit(Mint.coin(null, Metal.GOLD, 16), 4, 30)
        px.blit(Mint.coin(null, Metal.GOLD, 16), 28, 30)
        px.blit(Mint.coin(null, Metal.SILVER, 15), 17, 32)
        px.blit(Mint.gem(null, Stone.RUBY, Cut.CUSHION, 34), 7, 2)
        return px
    }

    @Test
    fun icon() {
        val res = File(System.getProperty("render.res") ?: return)
        val px = iconArt()
        for ((dir, side) in listOf("mdpi" to 108, "hdpi" to 162, "xhdpi" to 216, "xxhdpi" to 324, "xxxhdpi" to 432)) {
            val k = maxOf(1, (side * 0.6f / px.w).toInt())
            val fg = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
            val ox = (side - px.w * k) / 2
            val oy = (side - px.h * k) / 2
            for (y in 0 until px.h) for (x in 0 until px.w) {
                val p = px.c[y * px.w + x]
                if ((p ushr 24) == 0) continue
                for (j in 0 until k) for (i in 0 until k) fg.setPixel(ox + x * k + i, oy + y * k + j, p)
            }
            File(res, "mipmap-$dir").mkdirs()
            File(res, "mipmap-$dir/ic_launcher_foreground.png").outputStream().use { fg.compress(Bitmap.CompressFormat.PNG, 100, it) }
            // Legacy square icon (48dp) for launchers without adaptive icons.
            val legacy = side * 48 / 108
            val lg = Bitmap.createBitmap(legacy, legacy, Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(lg)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            paint.shader = android.graphics.RadialGradient(legacy / 2f, legacy * 0.45f, legacy * 0.75f, 0xFF4A2A1C.toInt(), 0xFF140E18.toInt(), android.graphics.Shader.TileMode.CLAMP)
            c.drawRoundRect(0f, 0f, legacy.toFloat(), legacy.toFloat(), legacy * 0.22f, legacy * 0.22f, paint)
            c.drawBitmap(Bitmap.createScaledBitmap(fg, (legacy * 1.5f).toInt(), (legacy * 1.5f).toInt(), false), -legacy * 0.25f, -legacy * 0.25f, null)
            for (name in listOf("ic_launcher", "ic_launcher_round")) File(res, "mipmap-$dir/$name.png").outputStream().use { lg.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val docs = File(System.getProperty("render.docs") ?: return).apply { mkdirs() }
        val big = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(big)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.shader = android.graphics.RadialGradient(256f, 230f, 380f, 0xFF4A2A1C.toInt(), 0xFF140E18.toInt(), android.graphics.Shader.TileMode.CLAMP)
        c.drawRoundRect(0f, 0f, 512f, 512f, 112f, 112f, paint)
        val k = 8
        for (y in 0 until px.h) for (x in 0 until px.w) {
            val p = px.c[y * px.w + x]
            if ((p ushr 24) == 0) continue
            for (j in 0 until k) for (i in 0 until k) big.setPixel((512 - px.w * k) / 2 + x * k + i, (512 - px.h * k) / 2 + y * k + j, p)
        }
        File(docs, "logo.png").outputStream().use { big.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun mirrors() {
        sheet("mirrors", listOf(
            listOf(Mint.mirror(photo("129"), Metal.GOLD, 56, 72), Mint.mirror(photo("288"), Metal.SILVER, 56, 72, 3), Mint.mirror(null, Metal.MITHRIL, 56, 72, empty = true),
                Mint.mirror(photo("174"), Metal.GOLD, 16, 21), Mint.chest(Metal.GOLD, 60, 51), Mint.chest(Metal.SILVER, 60, 51, open = true), Mint.chest(Metal.GOLD, 20, 19)),
        ), scale = 4)
    }
}

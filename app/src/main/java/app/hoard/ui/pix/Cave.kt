package app.hoard.ui.pix

import app.hoard.domain.Feature
import app.hoard.domain.Hue
import app.hoard.domain.Metal
import app.hoard.domain.Stage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A treasure lying on the pile: its small sprite and where it was dropped. */
class PileItem(val id: Long, val px: Px, val seed: Int)

/** Everything that shapes the cave right now. */
data class CaveLook(
    val hue: Hue,
    val stage: Stage,
    /** Treasures out of the forge; the pile grows with them. */
    val count: Int,
    val features: Set<Feature>,
    /** The newest few, drawn on the pile and tappable. */
    val items: List<PileItem> = emptyList(),
    /** Colours sprinkled in the pile: the metals of the coins and the gems kept. */
    val metals: List<Metal> = emptyList(),
    val stones: List<Int> = emptyList(),
    /** Treasures still in the forge. */
    val forging: Int = 0,
    val chests: Int = 0,
    /** A gnome waits with an errand. */
    val gnome: Boolean = false,
    val night: Boolean = true,
    val awake: Boolean = false,
)

/** Something on the scene that can be tapped, in scene pixels. */
sealed interface Hit {
    data class Treasure(val id: Long) : Hit
    data object Dragon : Hit
    data object Forge : Hit
    data object Chests : Hit
    data object Gnome : Hit
}

class HitBox(val hit: Hit, val x0: Int, val y0: Int, val x1: Int, val y1: Int) {
    fun has(x: Float, y: Float) = x >= x0 && x <= x1 && y >= y0 && y <= y1
}

/**
 * The dragon's cave, 180 x 150 pixels: rock lit by torches and by the gold itself, the forge on the
 * left, chests on a ledge on the right, and the dragon asleep on the pile in the middle.
 */
class Cave(val look: CaveLook) {
    companion object {
        const val W = 180
        const val H = 150
        const val FRAMES = 8
        const val GROUND = 141
        private const val CX = 90f

        private val rock = Ramp(rgb(10, 8, 16), rgb(18, 14, 26), rgb(28, 22, 38), rgb(40, 32, 52), rgb(56, 46, 68), rgb(76, 64, 86), rgb(100, 88, 106))
        private val floorRamp = Ramp(rgb(12, 9, 16), rgb(22, 16, 26), rgb(34, 26, 36), rgb(48, 38, 46), rgb(64, 52, 58))
        private val water = Ramp(rgb(8, 18, 36), rgb(14, 34, 60), rgb(24, 58, 92), rgb(42, 92, 126), rgb(74, 140, 170), rgb(140, 200, 220))
        private val shroom = Ramp(rgb(10, 40, 56), rgb(20, 80, 96), rgb(40, 140, 150), rgb(90, 210, 200), rgb(180, 252, 236))
        private val crystal = Ramp(rgb(30, 12, 54), rgb(58, 24, 98), rgb(96, 44, 150), rgb(146, 78, 202), rgb(196, 134, 240), rgb(240, 206, 255))
        private val bone = Ramp(rgb(70, 60, 64), rgb(124, 112, 104), rgb(178, 166, 146), rgb(220, 210, 186), rgb(246, 240, 220))
        private val wood = Ramp(rgb(30, 14, 18), rgb(56, 28, 26), rgb(86, 46, 34), rgb(120, 70, 46))
        private val hat = Ramp(rgb(70, 10, 24), rgb(130, 24, 34), rgb(190, 44, 44), rgb(232, 86, 66))
        private val coat = Ramp(rgb(16, 30, 60), rgb(30, 56, 100), rgb(50, 90, 140), rgb(84, 130, 180))
        private val skin = Ramp(rgb(120, 64, 54), rgb(186, 116, 90), rgb(230, 170, 134), rgb(250, 210, 176))

        private class Light(val x: Float, val y: Float, val r: Float, val color: Int, val k: Float)
    }

    /** How big the pile is: half-width and height in pixels. */
    private val fill = (ln(1f + look.count) / ln(151f)).coerceIn(0f, 1f)
    private val hw = 22f + 52f * fill
    private val ht = 6f + 32f * fill

    private fun pileTop(x: Float): Float {
        val u = (x - CX) / hw
        if (abs(u) >= 1f) return GROUND.toFloat()
        val h = ht * (1f - u * u).pow(0.75f) + noise(x.toInt() / 3, 1, 90) * 2f * fill
        return GROUND - h
    }

    private val torches = listOf(24f to 54f, 156f to 54f)
    private val forgeLit = look.forging > 0

    private val lights: List<Light> = buildList {
        torches.forEach { (x, y) -> add(Light(x, y - 6, 62f, rgb(255, 170, 80), 0.75f)) }
        add(Light(CX, GROUND - ht * 0.6f, hw * 1.7f + 20f, rgb(255, 200, 90), 0.4f + 0.35f * fill))
        add(Light(24f, 130f, if (forgeLit) 54f else 30f, rgb(255, 120, 50), if (forgeLit) 0.8f else 0.3f))
        if (Feature.CRYSTALS in look.features) add(Light(168f, 86f, 34f, rgb(180, 110, 255), 0.45f))
        if (Feature.MUSHROOMS in look.features) { add(Light(12f, 96f, 22f, rgb(90, 240, 220), 0.4f)); add(Light(170f, 124f, 22f, rgb(90, 240, 220), 0.4f)) }
        if (Feature.SKYLIGHT in look.features) add(Light(116f, 40f, 70f, if (look.night) rgb(150, 170, 255) else rgb(255, 250, 220), 0.45f))
        if (Feature.LANTERN in look.features) add(Light(46f, 84f, 20f, rgb(255, 210, 120), 0.4f))
    }

    private fun lightAt(x: Float, y: Float): Pair<Float, Int> {
        var l = 0.08f
        var cr = 0f; var cg = 0f; var cb = 0f; var cw = 0.001f
        for (s in lights) {
            val d = hypot(x - s.x, y - s.y) / s.r
            if (d >= 1f) continue
            val k = s.k * (1 - d) * (1 - d)
            l += k
            cr += s.color.r() * k; cg += s.color.g() * k; cb += s.color.b() * k; cw += k
        }
        return l.coerceAtMost(1.2f) to rgb((cr / cw).toInt().coerceIn(0, 255), (cg / cw).toInt().coerceIn(0, 255), (cb / cw).toInt().coerceIn(0, 255))
    }

    /** Distances to the two nearest points of a jittered grid: stones and the cracks between them. */
    private fun cells(x: Float, y: Float, size: Float, salt: Int): Triple<Float, Float, Int> {
        val gx = kotlin.math.floor(x / size).toInt()
        val gy = kotlin.math.floor(y / size).toInt()
        var d1 = 1e9f; var d2 = 1e9f; var id = 0
        for (j in -1..1) for (i in -1..1) {
            val cx = gx + i; val cy = gy + j
            val px = (cx + 0.5f + noise(cx, cy, salt) * 0.8f) * size
            val py = (cy + 0.5f + noise(cx, cy, salt + 1) * 0.8f) * size
            val d = hypot(x - px, y - py)
            if (d < d1) { d2 = d1; d1 = d; id = cx * 7919 + cy } else if (d < d2) d2 = d
        }
        return Triple(d1, d2, id)
    }

    private fun lit(base: Ramp, shadeV: Float, x: Int, y: Int, dither: Float = 0.5f): Int {
        val (l, tint) = lightAt(x + 0.5f, y + 0.5f)
        val v = (shadeV * (0.35f + l)).coerceIn(0f, 1f)
        val idx = (v * (base.size - 1) + bayer(x, y) * dither).roundToInt().coerceIn(0, base.size - 1)
        return mixc(base[idx], tint, (l * 0.22f).coerceAtMost(0.28f))
    }

    /** The parts that don't move, painted once. */
    private val still: Px by lazy { paintStill() }

    private fun paintStill(): Px {
        val px = Px(W, H)
        // Rock everywhere: big near stones round the edges, small stones on the far wall.
        for (y in 0 until H) for (x in 0 until W) {
            val fx = x + 0.5f; val fy = y + 0.5f
            val ax = (fx - CX) / 104f; val ay = (fy - 150f) / 152f
            val near = ax * ax + ay * ay > 1f
            if (y >= GROUND - 2 && !near) {
                val (d1, d2, id) = cells(fx, fy * 2.2f, 9f, 11)
                val crack = d2 - d1 < 1.6f
                val v = if (crack) 0.25f else 0.55f + noise(id, 0, 12) * 0.3f
                px.set(x, y, lit(floorRamp, v, x, y))
                continue
            }
            val size = if (near) 15f else 9f
            val (d1, d2, id) = cells(fx, fy, size, if (near) 3 else 5)
            val crack = d2 - d1 < (if (near) 1.5f else 1.1f)
            // Each stone leans its own way; its top-left catches the light.
            val tilt = noise(id, 1, 7) * 0.5f
            var v = if (near) 0.62f else 0.38f
            v += tilt + (if (d1 < size * 0.35f) 0.06f else -0.04f)
            v += noise(x, y, 9) * 0.08f
            if (crack) v = 0.12f
            if (near) v *= 0.85f + 0.15f * (y / H.toFloat())
            px.set(x, y, lit(rock, v.coerceIn(0f, 1f), x, y))
        }
        if (Feature.SKYLIGHT in look.features) skylight(px)
        if (Feature.STALACTITES in look.features) stalactites(px)
        if (Feature.BANNERS in look.features) banners(px)
        if (Feature.CRYSTALS in look.features) crystals(px)
        ledge(px)
        if (Feature.POOL in look.features) pool(px)
        if (Feature.LANTERN in look.features) wallLantern(px)
        forgeBody(px)
        if (Feature.BONES in look.features) bones(px)
        pile(px)
        if (Feature.MUSHROOMS in look.features) mushrooms(px)
        return px
    }

    private fun skylight(px: Px) {
        val hole = ellipse(116f, -2f, 16f, 10f)
        val sky = if (look.night) Ramp(rgb(10, 12, 40), rgb(20, 26, 70), rgb(36, 44, 104)) else Ramp(rgb(120, 170, 220), rgb(160, 200, 240), rgb(210, 232, 250))
        val m = px.cover(hole)
        for (y in 0 until H) for (x in 0 until W) if (m[y * W + x]) px.set(x, y, sky[(2 - y / 3).coerceIn(0, 2)])
        if (look.night) for (i in 0 until 5) px.set(104 + (noise(i, 0, 61) + 0.5f).times(24).toInt(), (noise(i, 1, 61) + 0.5f).times(6).toInt(), WHITE)
        px.ring(m, rock[0])
        // A shaft of light down to the gold.
        val c = if (look.night) rgb(170, 190, 255) else rgb(255, 248, 210)
        for (y in 8 until GROUND) {
            val t = y / GROUND.toFloat()
            val x0 = 104f - t * 22f; val x1 = 128f - t * 6f
            for (x in x0.toInt()..x1.toInt()) {
                val edge = min(x - x0, x1 - x) / ((x1 - x0) / 2f)
                val a = (34 * edge.coerceIn(0f, 1f) * (1 - t * 0.5f)).toInt()
                if (a > 4 && bayer(x, y) + 0.5f < 0.8f) px.set(x, y, c, a)
            }
        }
    }

    private fun stalactites(px: Px) {
        val xs = listOf(10f, 30f, 47f, 68f, 84f, 100f, 136f, 150f, 166f)
        xs.forEachIndexed { i, x ->
            val len = 7f + (noise(i, 2, 44) + 0.5f) * 13f
            val w = 3f + (noise(i, 3, 44) + 0.5f) * 3f
            val s = poly(x - w, -1f, x + w, -1f, x + 0.6f, len, x - 0.6f, len)
            val m = px.cover(s)
            for (y in 0..len.toInt() + 1) for (xx in (x - w).toInt()..(x + w).toInt()) {
                if (xx !in 0 until W || y !in 0 until H || !m[y * W + xx]) continue
                val side = (xx + 0.5f - x) / w
                px.set(xx, y, lit(rock, (0.6f - side * 0.35f + noise(xx, y, 4) * 0.08f).coerceIn(0f, 1f), xx, y))
            }
            px.ring(m, rock[0])
        }
    }

    private fun banners(px: Px) {
        val col = Dragons.of(look.hue)
        for ((i, bx) in listOf(58f, 122f).withIndex()) {
            px.hline(bx.toInt() - 9, bx.toInt() + 9, 5, Metals.gold[2])
            px.hline(bx.toInt() - 9, bx.toInt() + 9, 4, Metals.gold[4])
            val wave = if (i == 0) 0.6f else -0.6f
            val cloth = poly(bx - 8f, 6f, bx + 8f, 6f, bx + 8f + wave, 34f, bx, 29f, bx - 8f + wave, 34f)
            val m = px.cover(cloth)
            for (y in 6..35) for (x in (bx - 9).toInt()..(bx + 9).toInt()) {
                if (x !in 0 until W || !m[y * W + x]) continue
                val fold = sin((x - bx) * 0.9f + i).toFloat() * 0.15f
                px.set(x, y, lit(col.scale, (0.55f + fold + noise(x, y, 5) * 0.05f).coerceIn(0f, 1f), x, y, 0.4f))
            }
            px.ring(m, col.scale[0])
            Glyphs.stamp(px, Glyphs.halls.getValue(app.hoard.domain.Hall.WONDERS), bx.toInt() - 4, 13, Metals.gold[4])
        }
    }

    private fun crystals(px: Px) {
        val shards = listOf(Triple(166f, 96f, -0.5f), Triple(172f, 98f, 0.1f), Triple(160f, 99f, -1f), Triple(177f, 92f, 0.4f), Triple(169f, 100f, 0.6f))
        shards.forEachIndexed { i, (bx, by, lean) ->
            val h = 12f + (noise(i, 0, 8) + 0.5f) * 10f
            val w = 2.4f + (noise(i, 1, 8) + 0.5f) * 1.6f
            val tx = bx + lean * h * 0.5f
            val ty = by - h
            val s = poly(bx - w, by, tx - w * 0.6f, ty + 2f, tx, ty - 1f, tx + w * 0.6f, ty + 2f, bx + w, by)
            px.shade(s, facing(-0.4f + lean * 0.3f, -0.3f, 1f), crystal, spec = true, rim = crystal[5])
            px.line(bx.toInt(), by.toInt() - 1, tx.toInt(), ty.toInt() + 1, crystal[5], 150)
        }
        px.glow(168f, 88f, 18f, rgb(200, 150, 255), 50, 4)
    }

    /** A rock ledge on the right wall where the sealed chests wait. */
    private fun ledge(px: Px) {
        val s = poly(138f, 118f, 180f, 114f, 180f, 126f, 146f, 126f)
        val m = px.cover(s)
        for (y in 110..128) for (x in 136 until W) {
            if (!m[y * W + x]) continue
            val top = y < 118 + (x - 138) * -0.1f + 2
            px.set(x, y, lit(rock, if (top) 0.8f else 0.42f + noise(x, y, 3) * 0.1f, x, y))
        }
        px.ring(m, rock[0])
    }

    private fun pool(px: Px) {
        val s = ellipse(160f, 146f, 26f, 6.5f)
        val m = px.cover(s)
        for (y in 136 until H) for (x in 130 until W) {
            if (!m[y * W + x]) continue
            // The water mirrors the gold glow, in bands.
            val band = ((y - 139) / 2) % 3
            px.set(x, y, lit(water, (0.5f + band * 0.12f + noise(x / 4, y, 2) * 0.1f).coerceIn(0f, 1f), x, y, 0.3f))
        }
        px.ring(m, rock[1])
        // A thin waterfall down the wall into it.
        for (y in 104 until 141) {
            val x = 174 + (sin(y * 0.3f) * 0.6f).roundToInt()
            px.set(x, y, water[4], 180); px.set(x + 1, y, water[3], 140)
        }
    }

    private fun wallLantern(px: Px) {
        px.vline(46, 72, 78, Metals.silver[1])
        px.shade(rect(42f, 78f, 9f, 11f), facing(-0.2f, 0f, 1f), Metals.silver, spec = true)
        px.shade(rect(43.5f, 80f, 6f, 7f), facing(0f, 0f, 1f), Ramp(rgb(200, 120, 40), rgb(255, 196, 90), rgb(255, 238, 170)), outline = false)
    }

    private fun forgeBody(px: Px) {
        // A stone furnace with an arched mouth.
        val body = poly(2f, 146f, 4f, 112f, 10f, 102f, 24f, 98f, 38f, 102f, 44f, 112f, 46f, 146f)
        val m = px.cover(body)
        for (y in 96 until H) for (x in 0..48) {
            if (!m[y * W + x]) continue
            val (d1, d2, id) = cells(x + 0.5f, (y + 0.5f) * 1.4f, 6f, 21)
            val crack = d2 - d1 < 1.2f
            val v = if (crack) 0.15f else 0.55f + noise(id, 0, 6) * 0.25f - (x - 24) / 60f
            px.set(x, y, lit(rock, v.coerceIn(0f, 1f), x, y))
        }
        px.ring(m, rock[0])
        // Chimney.
        px.shade(rect(18f, 78f, 12f, 22f), column(24f, 6f), rock, rim = rock[4])
        // Anvil in front.
        val anvil = union(poly(44f, 134f, 62f, 134f, 66f, 131f, 60f, 138f, 46f, 138f), rect(50f, 138f, 6f, 5f), rect(47f, 143f, 12f, 3f))
        px.shade(anvil, facing(-0.3f, -0.6f, 1f), Metals.silver, spec = true)
    }

    private fun forgeFire(px: Px, f: Int) {
        val mouth = union(ellipse(24f, 126f, 9f, 9f), rect(15f, 126f, 18f, 18f))
        val m = px.cover(mouth)
        for (y in 116 until 145) for (x in 14..34) {
            if (!m[y * W + x]) continue
            val t = (y - 116) / 28f
            val flick = noise(x, y / 2 + f, 50) * 0.5f
            val v = if (forgeLit) (0.3f + t * 0.9f + flick).coerceIn(0f, 1f) else (t * 0.35f + flick * 0.3f).coerceIn(0f, 1f)
            val ramp = if (forgeLit) R.flame else Ramp(rgb(16, 6, 12), rgb(40, 10, 16), rgb(80, 20, 20), rgb(140, 40, 24))
            px.set(x, y, ramp[(v * (ramp.size - 1) + bayer(x, y) * 0.6f).roundToInt().coerceIn(0, ramp.size - 1)])
        }
        px.ring(m, rock[0])
        if (forgeLit) {
            px.glow(24f, 134f, 24f + (f % 2) * 2f, rgb(255, 150, 60), 90, 5)
            // Sparks rising from the chimney.
            for (i in 0 until 3) {
                val p = ((f + i * 3) % FRAMES) / FRAMES.toFloat()
                px.set(24 + (noise(i, f, 3) * 6).toInt(), (76 - p * 22).toInt(), R.ember[4 - (p * 3).toInt()])
            }
        }
    }

    private fun bones(px: Px) {
        // A fallen knight, half buried in gold: helmet, skull and a rusty sword.
        px.shade(ellipse(64f, 142f, 5f, 4f), dome(64f, 141f, 5f, 4f), bone, spec = true)
        px.set(62, 142, OUT); px.set(65, 142, OUT); px.set(63, 144, OUT)
        px.shade(union(ellipse(54f, 141f, 6f, 5f), rect(48f, 141f, 12f, 4f)), dome(54f, 140f, 6f, 5f), Metals.silver, spec = true)
        px.hline(50, 58, 142, OUT)
        px.line(70, 146, 82, 134, Metals.copper[2])
        px.line(71, 146, 83, 134, Metals.copper[1])
        px.line(73, 136, 77, 140, Metals.gold[3])
        for (i in 0 until 3) px.shade(ellipse(42f + i * 4f, 147f, 2.4f, 1.2f), facing(0f, -0.5f, 1f), bone)
    }

    private fun pile(px: Px) {
        val x0 = (CX - hw).toInt() - 1
        val x1 = (CX + hw).toInt() + 1
        // Under the coins: dark gold, so gaps read as depth.
        for (x in x0..x1) {
            val top = pileTop(x + 0.5f)
            for (y in top.toInt()..GROUND + 3) if (y < H && x in 0 until W) px.set(x, y, Metals.gold[1 + if ((x + y) % 3 == 0) 0 else 1])
        }
        // Coins, back to front, each a tiny lit ellipse; a few in other metals, a few gems.
        val metals = look.metals.ifEmpty { listOf(Metal.GOLD) }
        val ys = (GROUND - ht).toInt() - 1
        var k = 0
        for (y in ys..GROUND + 4 step 2) {
            var x = x0 + ((y / 2) % 2) * 2f
            while (x <= x1) {
                val jx = x + noise(x.toInt(), y, 71) * 2.5f
                val jy = y + noise(x.toInt(), y, 72) * 1.5f
                if (jy >= pileTop(jx) + 1f && jy < H) {
                    k++
                    val roll = noise(jx.toInt(), y, 73) + 0.5f
                    if (look.stones.isNotEmpty() && roll > 0.965f) {
                        val c = look.stones[k % look.stones.size]
                        px.shade(poly(jx, jy - 2f, jx + 2f, jy, jx, jy + 2f, jx - 2f, jy), facing(-0.4f, -0.5f, 1f), R.around(c), spec = true)
                    } else {
                        val metal = if (roll > 0.85f) metals[k % metals.size] else Metal.GOLD
                        val ramp = Metals.of(metal)
                        val tilt = noise(jx.toInt(), y, 74)
                        px.shade(ellipse(jx, jy, 2.7f, 1.5f + tilt), facing(tilt, -0.8f, 0.9f), ramp, spec = true, ambient = 0.35f, outline = true, line = ramp[1])
                    }
                }
                x += 4f
            }
        }
        // Coins that rolled off onto the floor.
        for (i in 0 until 6 + (fill * 10).toInt()) {
            val side = if (i % 2 == 0) -1f else 1f
            val x = CX + side * (hw + 2f + (noise(i, 0, 75) + 0.5f) * 14f)
            val y = GROUND + 1f + (noise(i, 1, 75) + 0.5f) * 6f
            px.shade(ellipse(x, y, 2.4f, 1.3f), facing(0f, -0.8f, 0.9f), Metals.gold, spec = true, ambient = 0.3f, line = Metals.gold[1])
        }
        // The pile shines where the light falls.
        px.glow(CX - hw * 0.25f, GROUND - ht * 0.8f, hw * 0.9f, rgb(255, 230, 150), 40, 4, onlyDrawn = true)
    }

    private fun mushrooms(px: Px) {
        for ((bx, by) in listOf(8f to 100f, 15f to 99f, 166f to 115f, 172f to 114f, 177f to 115f)) {
            val r = 2.2f + (noise(bx.toInt(), 0, 4) + 0.5f) * 1.6f
            px.vline(bx.toInt(), (by - 1).toInt(), (by + 2).toInt(), shroom[3])
            px.shade(ellipse(bx, by - 1f, r, r * 0.65f), dome(bx, by - 1f, r, r), shroom, spec = true)
        }
        px.glow(12f, 98f, 10f, rgb(110, 255, 230), 50, 3)
        px.glow(171f, 113f, 11f, rgb(110, 255, 230), 50, 3)
    }

    private fun torch(px: Px, x: Float, y: Float, f: Int) {
        px.shade(rect(x - 1.5f, y, 3f, 11f), column(x, 1.5f), wood)
        px.shade(rect(x - 3f, y + 4f, 6f, 2f), facing(0f, -0.5f, 1f), Metals.silver)
        val sway = sin(f / FRAMES.toFloat() * 2 * PI).toFloat()
        val tall = 7f + (f % 3) * 1f
        val flame = poly(x - 3f, y + 1f, x - 2f, y - tall * 0.5f, x + sway * 1.5f, y - tall, x + 2f, y - tall * 0.45f, x + 3f, y + 1f)
        px.shade(flame, dome(x, y - 2f, 3f, tall * 0.7f), R.flame, outline = false, lo = 2)
        px.shade(ellipse(x, y - 1.5f, 1.4f, 2.4f), facing(0f, 0f, 1f), Ramp(rgb(255, 244, 180)), outline = false)
        px.glow(x, y - 3f, 16f + (f % 2), rgb(255, 180, 90), 60, 4)
    }

    private fun drip(px: Px, f: Int) {
        if (Feature.STALACTITES !in look.features) return
        val x = 47
        val p = f / FRAMES.toFloat()
        val y = (16 + p * 70).toInt()
        px.set(x, y, water[5]); px.set(x, y - 1, water[4], 160)
    }

    private fun bats(px: Px, f: Int) {
        for ((i, x) in listOf(36, 76, 142).withIndex()) {
            val y = 2 + i % 2
            val flap = (f + i * 3) % FRAMES == 0
            val body = rgb(34, 24, 40)
            px.set(x, y, body); px.set(x, y + 1, body); px.set(x, y + 2, body)
            if (flap) { px.set(x - 1, y + 1, body); px.set(x - 2, y, body); px.set(x + 1, y + 1, body); px.set(x + 2, y, body) }
            else { px.set(x - 1, y + 1, body); px.set(x + 1, y + 1, body) }
            px.set(x, y + 2, rgb(255, 90, 80))
        }
    }

    private fun gnome(px: Px, f: Int) {
        val x = 68f; val y = 136f
        // A little errand-giver with a lantern, by the forge.
        px.shade(rect(x - 3f, y + 2f, 6f, 6f), column(x, 3f), coat)
        px.shade(ellipse(x, y, 3f, 2.6f), dome(x, y, 3f, 2.6f), skin)
        px.shade(poly(x - 1f, y + 1f, x + 1f, y + 1f, x, y + 6f), facing(0f, 0f, 1f), Ramp(rgb(200, 200, 210), rgb(240, 240, 246), rgb(255, 255, 255)), outline = false)
        val tip = if (f % 4 < 2) 0f else 1f
        px.shade(poly(x - 4f, y - 1f, x + 4f, y - 1f, x + 2f + tip, y - 9f), facing(-0.3f, -0.3f, 1f), hat)
        px.set(x.toInt() - 1, y.toInt(), OUT); px.set(x.toInt() + 1, y.toInt(), OUT)
        px.shade(rect(x + 3f, y + 3f, 3f, 4f), facing(0f, 0f, 1f), Ramp(rgb(255, 200, 90), rgb(255, 240, 170)), outline = false)
        px.glow(x + 4.5f, y + 5f, 8f, rgb(255, 210, 120), 70, 3)
        // An exclamation mark, bobbing.
        val by = y - 16f - (f % 4) * 0.5f
        px.vline(x.toInt(), by.toInt(), by.toInt() + 2, Metals.gold[5]); px.set(x.toInt(), by.toInt() + 4, Metals.gold[5])
    }

    /** Where the dragon lies, in scene pixels: left, top. */
    private val dragonAt: Pair<Int, Int> by lazy {
        val w = Dragon.width(look.stage)
        val h = Dragon.height(look.stage)
        val bottom = GROUND - ht * 0.45f
        ((CX + 4f - w / 2f).roundToInt()) to ((bottom - h + 3).roundToInt())
    }

    /** Spots on the front of the pile for the newest treasures, kept apart and out from under the dragon. */
    private val placed: List<Triple<PileItem, Int, Int>> by lazy {
        val out = ArrayList<Triple<PileItem, Int, Int>>()
        val boxes = ArrayList<IntArray>()
        val (dx, dy) = dragonAt
        val dw = Dragon.width(look.stage)
        val dh = Dragon.height(look.stage)
        val dragonBox = intArrayOf(dx + 4, dy + 4, dx + dw - 4, dy + dh - 6)
        for (item in look.items) {
            var best: IntArray? = null
            for (attempt in 0 until 40) {
                val u = noise(item.seed, attempt, 81) * 1.7f
                val cx = CX + u * hw
                val top = pileTop(cx)
                val depth = (noise(item.seed, attempt, 82) + 0.5f)
                val cy = top + item.px.h * 0.4f + depth * max(0f, GROUND - top - item.px.h * 0.4f)
                val box = intArrayOf((cx - item.px.w / 2f).toInt(), (cy - item.px.h * 0.75f).toInt(), (cx + item.px.w / 2f).toInt(), (cy + item.px.h * 0.25f).toInt())
                if (box[0] < 2 || box[2] > W - 2 || box[3] > H - 1) continue
                fun overlap(a: IntArray, b: IntArray, pad: Int) = a[0] < b[2] + pad && a[2] > b[0] - pad && a[1] < b[3] + pad && a[3] > b[1] - pad
                if (overlap(box, dragonBox, 0)) continue
                if (boxes.any { overlap(it, box, 1) }) continue
                best = box
                break
            }
            if (best != null) { boxes += best; out += Triple(item, best[0], best[1]) }
        }
        // Back to front.
        out.sortedBy { it.third + it.first.px.h }
    }

    val hits: List<HitBox> by lazy {
        val (dx, dy) = dragonAt
        buildList {
            placed.reversed().forEach { (item, x, y) -> add(HitBox(Hit.Treasure(item.id), x - 2, y - 2, x + item.px.w + 2, y + item.px.h + 2)) }
            if (look.gnome) add(HitBox(Hit.Gnome, 60, 120, 76, 146))
            add(HitBox(Hit.Dragon, dx, dy, dx + Dragon.width(look.stage), dy + Dragon.height(look.stage)))
            add(HitBox(Hit.Forge, 0, 76, 46, 148))
            if (look.chests > 0) add(HitBox(Hit.Chests, 138, 92, 180, 120))
        }
    }

    fun frame(f: Int): Px {
        val px = Px(W, H)
        System.arraycopy(still.c, 0, px.c, 0, still.c.size)
        forgeFire(px, f)
        torches.forEach { (x, y) -> torch(px, x, y, f) }
        if (Feature.BATS in look.features) bats(px, f)
        drip(px, f)
        // Chests on the ledge.
        if (look.chests > 0) {
            val n = min(3, look.chests)
            for (i in 0 until n) {
                val c = Mint.chest(Metal.GOLD, 20, 19, f)
                px.blit(c, 141 + i * 12 - (n - 1) * 2, 99 + (i % 2))
            }
        }
        val (dx, dy) = dragonAt
        px.blit(Dragon.frame(look.hue, look.stage, f, look.awake), dx, dy)
        for ((item, x, y) in placed) px.blit(item.px, x, y)
        if (look.gnome) gnome(px, f)
        // Glints wandering over the gold.
        for (i in 0 until 4 + (fill * 8).toInt()) {
            val u = noise(i, f / 2, 91) * 1.8f
            val x = CX + u * hw
            val y = pileTop(x) + 2f + (noise(i, f / 2, 92) + 0.5f) * (GROUND - pileTop(x) - 2f)
            if ((i + f) % 3 == 0 && y < GROUND) Mint.spark(px, x.toInt(), y.toInt(), 1)
        }
        return px
    }
}

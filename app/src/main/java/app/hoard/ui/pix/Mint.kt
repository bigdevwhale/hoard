package app.hoard.ui.pix

import android.graphics.Bitmap
import app.hoard.domain.Cut
import app.hoard.domain.Hall
import app.hoard.domain.Metal
import app.hoard.domain.RelicForm
import app.hoard.domain.Stone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where moments become treasure: a photo struck into a coin, cut into a gem, set into a relic or
 * framed in a mirror. Everything is painted in pixels from the photo itself, at any size.
 */
object Mint {
    private val LIGHT = norm(-0.55f, -0.75f, 0.65f)

    private fun norm(x: Float, y: Float, z: Float): V3 {
        val n = sqrt(x * x + y * y + z * z)
        return V3(x / n, y / n, z / n)
    }

    private fun tone(ramp: Ramp, light: Float, x: Int, y: Int, lo: Int = 0, hi: Int = ramp.size - 1, dither: Float = 0.55f): Int {
        val g = light.coerceIn(0f, 1f).pow(1.25f)
        val v = lo + g * (hi - lo + 0.4f) - 0.2f + bayer(x, y) * dither
        return ramp[v.roundToInt().coerceIn(lo, hi)]
    }

    private fun blur(a: FloatArray, w: Int, h: Int): FloatArray {
        val o = FloatArray(a.size)
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0f; var n = 0f
            for (j in -1..1) for (i in -1..1) {
                val xx = (x + i).coerceIn(0, w - 1); val yy = (y + j).coerceIn(0, h - 1)
                val k = if (i == 0 && j == 0) 2f else 1f
                s += a[yy * w + xx] * k; n += k
            }
            o[y * w + x] = s / n
        }
        return o
    }

    /** A dark line around everything drawn, darkest tone of [ramp]. */
    private fun outline(px: Px, color: Int) {
        val solid = BooleanArray(px.w * px.h) { px.c[it].al() >= 128 }
        px.ring(solid, color)
    }

    private fun rimLight(light: Float, nz: Float) = max(0f, light) * 0.85f + 0.16f + (1 - nz) * 0.02f

    /* ---------- Coins ---------- */

    /** The face of a coin: the photo struck in relief, a raised rim and a ring of beads. */
    fun coin(photo: Bitmap?, metal: Metal, size: Int, f: Int = 0): Px {
        val px = Px(size, size)
        val ramp = Metals.of(metal)
        val c = size / 2f
        val outer = size / 2f - 1f
        val rim = max(1.6f, size * 0.085f)
        val r = outer - rim
        val d = max(4, ceil(2 * r).toInt())
        val s = photo?.let { Sample.of(it, d, d) } ?: Sample.blank(d, d)
        val lv = s.levels()
        val hm = blur(lv, d, d)
        val o = c - d / 2f
        val beads = size >= 36
        val rb = r - 1.6f
        for (y in 0 until size) for (x in 0 until size) {
            val dx = x + 0.5f - c
            val dy = y + 0.5f - c
            val dist = hypot(dx, dy)
            if (dist > outer) continue
            if (dist >= r) {
                val t = ((dist - (r + outer) / 2f) / (rim / 2f)).coerceIn(-1f, 1f)
                val n = norm(dx / dist * t, dy / dist * t, sqrt(max(0.05f, 1 - t * t)))
                val l = dot(n, LIGHT)
                var col = tone(ramp, rimLight(l, n.z), x, y, 1, 6)
                if (l > 0.93f) col = ramp[6]
                // A milled edge on big coins.
                if (size >= 48 && dist > outer - 1.2f && ((atan2(dy, dx) / (2 * PI) * size * 1.6).roundToInt() % 2 == 0)) col = mixc(col, ramp[1], 0.5f)
                px.set(x, y, col)
            } else {
                val fx = (x - o).toInt().coerceIn(0, d - 1)
                val fy = (y - o).toInt().coerceIn(0, d - 1)
                fun h(i: Int, j: Int) = hm[j.coerceIn(0, d - 1) * d + i.coerceIn(0, d - 1)]
                val gx = h(fx + 1, fy) - h(fx - 1, fy)
                val gy = h(fx, fy + 1) - h(fx, fy - 1)
                val k = 2.6f
                val n = norm(-gx * k, -gy * k, 1f)
                val l = 0.02f + 0.68f * lv[fy * d + fx] + 0.42f * max(0f, dot(n, LIGHT)) - 0.06f
                var col = tone(ramp, l, x, y, 1, 5, 0.7f)
                if (r - dist < 1.15f) col = if (dx + dy < 0) ramp[1] else ramp[4]
                px.set(x, y, col)
            }
        }
        if (beads) {
            val n = (2 * PI * rb / 3.4f).roundToInt()
            for (i in 0 until n) {
                val a = i * 2 * PI / n
                val bx = (c + cos(a) * rb).toFloat()
                val by = (c + sin(a) * rb).toFloat()
                px.set(bx.toInt() + 1, by.toInt() + 1, ramp[2])
                px.set(bx.toInt(), by.toInt(), ramp[5])
            }
        }
        // A blank coin (a swatch, the icon) gets a struck star instead of a moment.
        if (photo == null && size >= 14) {
            val k = if (size >= 40) 2 else 1
            val star = scale(Glyphs.halls.getValue(Hall.WONDERS), k)
            val w = star[0].length
            Glyphs.emboss(px, star, (c - w / 2f).roundToInt(), (c - w / 2f).roundToInt(), ramp[5], ramp[2], ramp[6])
        }
        if (Metals.glitter(metal)) glitter(px, ramp, f, size)
        outline(px, ramp[0])
        return px
    }

    /** The back of a coin: the hall's sign, the runes of that day and the year. */
    fun coinBack(metal: Metal, hall: Hall, runes: List<Array<String>>, year: Int, size: Int): Px {
        val px = Px(size, size)
        val ramp = Metals.of(metal)
        val c = size / 2f
        val outer = size / 2f - 1f
        val rim = max(1.6f, size * 0.085f)
        val r = outer - rim
        for (y in 0 until size) for (x in 0 until size) {
            val dx = x + 0.5f - c
            val dy = y + 0.5f - c
            val dist = hypot(dx, dy)
            if (dist > outer) continue
            if (dist >= r) {
                val t = ((dist - (r + outer) / 2f) / (rim / 2f)).coerceIn(-1f, 1f)
                val n = norm(dx / dist * t, dy / dist * t, sqrt(max(0.05f, 1 - t * t)))
                val l = dot(n, LIGHT)
                px.set(x, y, if (l > 0.93f) ramp[6] else tone(ramp, rimLight(l, n.z), x, y, 1, 6))
            } else {
                // A field that bulges very slightly, with a faint grain of the die.
                val n = norm(dx / r * 0.35f, dy / r * 0.35f, 1f)
                val l = 0.32f + 0.4f * max(0f, dot(n, LIGHT)) + noise(x, y, 3) * 0.06f
                var col = tone(ramp, l, x, y, 2, 4, 0.5f)
                if (r - dist < 1.15f) col = if (dx + dy < 0) ramp[1] else ramp[4]
                px.set(x, y, col)
            }
        }
        val k = if (size >= 56) 2 else 1
        val sign = scale(Glyphs.halls.getValue(hall), k)
        val gw = sign[0].length
        Glyphs.emboss(px, sign, (c - gw / 2f).roundToInt(), (c - gw / 2f).roundToInt() - if (size >= 40) 2 else 0, ramp[5], ramp[1], ramp[6])
        if (size >= 40) {
            val shown = runes.take(3)
            val total = shown.size * 8 - 1
            shown.forEachIndexed { i, rune ->
                Glyphs.emboss(px, rune, (c - total / 2f).roundToInt() + i * 8, (c - r + 3).roundToInt(), ramp[5], ramp[1])
            }
            Glyphs.number(px, year, c.roundToInt(), (c + r - 9).roundToInt(), ramp[5], ramp[1])
        }
        if (Metals.glitter(metal)) glitter(px, ramp, 0, size)
        outline(px, ramp[0])
        return px
    }

    fun scale(pat: Array<String>, k: Int): Array<String> =
        if (k <= 1) pat else pat.flatMap { row -> val wide = row.map { ch -> ch.toString().repeat(k) }.joinToString(""); List(k) { wide } }.toTypedArray()

    private fun glitter(px: Px, ramp: Ramp, f: Int, size: Int) {
        val n = size / 6
        for (i in 0 until n) {
            val x = ((noise(i, 1, 70) + 0.5f) * size).toInt()
            val y = ((noise(i, 2, 70) + 0.5f) * size).toInt()
            if (!px.has(x, y)) continue
            val on = (i + f) % 4 != 0
            if (on) px.set(x, y, WHITE)
            if (on && i % 3 == 0) { px.set(x + 1, y, ramp[6]); px.set(x - 1, y, ramp[6]); px.set(x, y + 1, ramp[6]); px.set(x, y - 1, ramp[6]) }
        }
    }

    /* ---------- Gems ---------- */

    private fun outlineOf(cut: Cut): List<Pair<Float, Float>> = when (cut) {
        Cut.ROUND -> List(10) { i -> val a = -PI / 2 + i * 2 * PI / 10; (cos(a).toFloat()) to (sin(a).toFloat()) }
        Cut.OVAL -> List(10) { i -> val a = -PI / 2 + i * 2 * PI / 10; (cos(a).toFloat() * 0.78f) to (sin(a).toFloat()) }
        Cut.CUSHION -> listOf(-0.62f to -0.95f, 0.62f to -0.95f, 0.95f to -0.62f, 0.95f to 0.62f, 0.62f to 0.95f, -0.62f to 0.95f, -0.95f to 0.62f, -0.95f to -0.62f)
        Cut.STEP -> listOf(-0.42f to -1f, 0.42f to -1f, 0.72f to -0.72f, 0.72f to 0.72f, 0.42f to 1f, -0.42f to 1f, -0.72f to 0.72f, -0.72f to -0.72f)
        Cut.PEAR -> listOf(0f to -1f, 0.38f to -0.5f, 0.74f to 0.06f, 0.82f to 0.46f, 0.6f to 0.84f, 0.2f to 1f, -0.2f to 1f, -0.6f to 0.84f, -0.82f to 0.46f, -0.74f to 0.06f, -0.38f to -0.5f)
        Cut.HEART -> listOf(0f to -0.52f, 0.3f to -0.9f, 0.72f to -0.92f, 0.98f to -0.56f, 0.9f to -0.06f, 0.5f to 0.48f, 0f to 1f, -0.5f to 0.48f, -0.9f to -0.06f, -0.98f to -0.56f, -0.72f to -0.92f, -0.3f to -0.9f)
    }

    private class Facet(val ax: Float, val ay: Float, val bx: Float, val by: Float, val cx: Float, val cy: Float, val n: V3, val table: Boolean) {
        fun has(x: Float, y: Float): Boolean {
            val d1 = (x - bx) * (ay - by) - (ax - bx) * (y - by)
            val d2 = (x - cx) * (by - cy) - (bx - cx) * (y - cy)
            val d3 = (x - ax) * (cy - ay) - (cx - ax) * (y - ay)
            val neg = d1 < 0 || d2 < 0 || d3 < 0
            val pos = d1 > 0 || d2 > 0 || d3 > 0
            return !(neg && pos)
        }
    }

    /**
     * A cut stone with the moment caught inside: clear in the table, broken into pieces in the facets
     * around it. [tiltX], [tiltY] (-1..1) swing the light, as when the phone is turned in the hand.
     */
    fun gem(photo: Bitmap?, stone: Stone, cut: Cut, size: Int, tiltX: Float = 0f, tiltY: Float = 0f, f: Int = 0): Px {
        val px = Px(size, size)
        val ramp = Metals.stone(stone)
        val c = size / 2f
        val ext = size / 2f - 1.5f
        val pts = outlineOf(cut).map { (x, y) -> (c + x * ext) to (c + y * ext) }
        val shift = if (cut == Cut.PEAR) 0.16f else if (cut == Cut.HEART) -0.08f else -0.05f
        val tk = if (cut == Cut.STEP) 0.6f else 0.52f
        val tab = outlineOf(cut).map { (x, y) -> (c + x * ext * tk) to (c + (y * tk + shift) * ext) }
        val facets = ArrayList<Facet>()
        val n = pts.size
        // The table, as a fan from its middle.
        val tcx = tab.map { it.first }.average().toFloat()
        val tcy = tab.map { it.second }.average().toFloat()
        for (i in 0 until n) {
            val j = (i + 1) % n
            facets += Facet(tcx, tcy, tab[i].first, tab[i].second, tab[j].first, tab[j].second, V3(0f, 0f, 1f), true)
        }
        for (i in 0 until n) {
            val j = (i + 1) % n
            val mx = (pts[i].first + pts[j].first) / 2f
            val my = (pts[i].second + pts[j].second) / 2f
            fun out(x: Float, y: Float, k: Float, twist: Float): V3 {
                val dx = x - c; val dy = y - c
                val l = max(0.01f, hypot(dx, dy))
                val ux = dx / l; val uy = dy / l
                return norm(ux * k - uy * twist, uy * k + ux * twist, 1f)
            }
            facets += Facet(tab[i].first, tab[i].second, tab[j].first, tab[j].second, mx, my, out(mx, my, 0.45f, 0f), false)
            facets += Facet(pts[i].first, pts[i].second, mx, my, tab[i].first, tab[i].second, out((pts[i].first + mx) / 2, (pts[i].second + my) / 2, 0.95f, 0.28f), false)
            facets += Facet(mx, my, pts[j].first, pts[j].second, tab[j].first, tab[j].second, out((pts[j].first + mx) / 2, (pts[j].second + my) / 2, 0.95f, -0.28f), false)
        }
        val light = norm(-0.55f + tiltX * 0.9f, -0.72f + tiltY * 0.9f, 0.62f)
        val samp = photo?.let { Sample.of(it, size, size) }
        val id = IntArray(size * size) { -1 }
        val clear = stone == Stone.DIAMOND || stone == Stone.MOONSTONE
        for (y in 0 until size) for (x in 0 until size) {
            val fx = x + 0.5f; val fy = y + 0.5f
            val k = facets.indexOfFirst { it.has(fx, fy) }
            if (k < 0) continue
            id[y * size + x] = k
            val fc = facets[k]
            val lit = max(0f, dot(fc.n, light))
            val base = if (samp == null) ramp[3] else if (fc.table) samp.at(fx, fy) else {
                // Refraction: each facet shows the picture again, flipped and shifted.
                val rx = c - (fx - c) * 0.7f + fc.n.x * size * 0.3f
                val ry = c - (fy - c) * 0.7f + fc.n.y * size * 0.3f
                samp.at(rx, ry)
            }
            val l = lum(base)
            val glow = (0.22f + 0.55f * l + (if (fc.table) 0.25f else 0.6f) * lit - if (fc.table) 0.02f else 0.12f).coerceIn(0f, 1f)
            val tint = tone(ramp, glow, x, y, 0, ramp.size - 1, 0.35f)
            val mixAmt = if (samp == null) 0f else if (clear) (if (fc.table) 0.55f else 0.28f) else (if (fc.table) 0.5f else 0.2f)
            val shaded = mixc(OUT, punch(base, 1.15f, 1.05f), 0.55f + 0.5f * lit)
            px.set(x, y, paint(mixc(tint, shaded, mixAmt), x, y, 8, 0.6f))
        }
        // Facet edges catch the light.
        for (y in 0 until size) for (x in 0 until size) {
            val k = id[y * size + x]
            if (k < 0) continue
            val r = x + 1 < size && id[y * size + x + 1] >= 0 && id[y * size + x + 1] != k
            val b = y + 1 < size && id[(y + 1) * size + x] >= 0 && id[(y + 1) * size + x] != k
            if (r || b) {
                val lit = facets[k].n.let { dot(it, light) } > 0.75f
                px.set(x, y, if (lit) ramp.lightest else ramp[ramp.size - 3], if (size >= 40) 150 else 110)
            }
        }
        // A glint on the facet facing the light, and a twinkle that moves with the frame.
        val best = facets.withIndex().filter { !it.value.table }.maxByOrNull { dot(it.value.n, light) }
        if (best != null) {
            val fc = best.value
            val gx = ((fc.ax + fc.bx + fc.cx) / 3f).toInt()
            val gy = ((fc.ay + fc.by + fc.cy) / 3f).toInt()
            spark(px, gx, gy, if (size >= 48) 3 else 2)
        }
        if (size >= 40) {
            val k = Math.floorMod(f, 4)
            val tx = (c + cos(k * 1.7) * ext * 0.45f).toInt()
            val ty = (c + sin(k * 2.3) * ext * 0.4f).toInt()
            if (id[ty.coerceIn(0, size - 1) * size + tx.coerceIn(0, size - 1)] >= 0) spark(px, tx, ty, 1)
        }
        outline(px, ramp[0])
        return px
    }

    fun spark(px: Px, x: Int, y: Int, len: Int) {
        px.set(x, y, WHITE)
        for (i in 1..len) {
            val a = if (i == len) 150 else 230
            px.set(x + i, y, WHITE, a); px.set(x - i, y, WHITE, a); px.set(x, y + i, WHITE, a); px.set(x, y - i, WHITE, a)
        }
    }

    /* ---------- Windows onto the moment ---------- */

    /** Paints the photo into [s] as pixel paint, darkening toward the edges, with a glint of glass. */
    private fun picture(px: Px, photo: Bitmap?, s: Shape, glass: Boolean = true, warm: Boolean = false) {
        val x0 = max(0, s.x0); val y0 = max(0, s.y0); val x1 = min(px.w - 1, s.x1); val y1 = min(px.h - 1, s.y1)
        val w = x1 - x0 + 1; val h = y1 - y0 + 1
        if (w < 2 || h < 2) return
        val samp = photo?.let { Sample.of(it, w, h) }
        val cx = (x0 + x1) / 2f; val cy = (y0 + y1) / 2f
        val m = px.cover(s)
        for (y in y0..y1) for (x in x0..x1) {
            if (!m[y * px.w + x]) continue
            var col = samp?.let { punch(it[x - x0, y - y0]) } ?: rgb(70, 60, 80)
            if (warm) col = mixc(col, rgb(255, 196, 110), 0.28f)
            val dx = (x + 0.5f - cx) / (w / 2f); val dy = (y + 0.5f - cy) / (h / 2f)
            val v = (dx * dx + dy * dy).coerceIn(0f, 1.4f)
            col = mixc(col, OUT, 0.32f * v)
            col = paint(col, x, y, if (w >= 16) 7 else 5, 0.75f)
            if (glass) {
                val band = (x - x0) + (y - y0)
                if (band in (w + h) / 6..(w + h) / 6 + 1 || band == (w + h) / 6 + 4) col = mixc(col, WHITE, 0.4f)
            }
            px.set(x, y, col)
        }
        // A dark lip where the picture meets its setting.
        px.ring(m, OUT)
    }

    /** A bezel: a ring of metal around an inner shape. */
    private fun bezel(px: Px, pen: Pen, cx: Float, cy: Float, rx: Float, ry: Float, w: Float, ramp: Ramp) {
        val ring = minus(pen.ell(cx, cy, rx + w, ry + w), pen.ell(cx, cy, rx, ry))
        px.shade(ring, pen.dome(cx, cy, rx + w, ry + w, flat = -0.25f), ramp, spec = true, rim = ramp[5])
    }

    /** The photo in an oval, set in a bezel of [ramp]. */
    private fun medallion(px: Px, pen: Pen, photo: Bitmap?, cx: Float, cy: Float, rx: Float, ry: Float, ramp: Ramp, w: Float = 3f) {
        bezel(px, pen, cx, cy, rx, ry, w, ramp)
        picture(px, photo, pen.ell(cx, cy, rx, ry))
    }

    /* ---------- Relics ---------- */

    fun relic(photo: Bitmap?, form: RelicForm, metal: Metal, size: Int, f: Int = 0): Px {
        val px = Px(size, size)
        val pen = Pen(px, 1f)
        val m = Metals.of(metal)
        val iron = Metals.silver
        when (form) {
            RelicForm.CROWN -> crown(px, pen, photo, m)
            RelicForm.CHALICE -> chalice(px, pen, photo, m)
            RelicForm.SWORD -> sword(px, pen, photo, m, iron)
            RelicForm.AMULET -> amulet(px, pen, photo, m)
            RelicForm.HORN -> horn(px, pen, photo, m)
            RelicForm.KEY -> key(px, pen, photo, m)
            RelicForm.RING -> ring(px, pen, photo, m)
            RelicForm.LANTERN -> lantern(px, pen, photo, m, f)
        }
        if (Metals.glitter(metal)) glitter(px, m, f, size)
        return px
    }

    private val velvet = Ramp(rgb(40, 8, 30), rgb(78, 16, 42), rgb(120, 28, 54), rgb(164, 46, 66), rgb(204, 78, 86))
    private val leather = Ramp(rgb(30, 14, 20), rgb(56, 28, 28), rgb(86, 46, 36), rgb(118, 70, 48), rgb(150, 98, 64))

    private fun jewel(px: Px, pen: Pen, cx: Float, cy: Float, r: Float, color: Int) {
        val ramp = R.around(color)
        px.shade(pen.ell(cx, cy, r, r), pen.dome(cx, cy, r, r), ramp, spec = true)
        px.set(pen.x(cx - r * 0.35f).toInt(), pen.y(cy - r * 0.35f).toInt(), WHITE)
    }

    private fun crown(px: Px, pen: Pen, photo: Bitmap?, m: Ramp) {
        px.shade(pen.ell(50f, 52f, 29f, 22f), pen.dome(50f, 48f, 29f, 24f), velvet, texture = { x, y -> noise(x, y, 8) * 0.06f })
        val xs = listOf(19f, 34.5f, 50f, 65.5f, 81f)
        xs.forEachIndexed { i, x ->
            val tip = if (i == 2) 22f else if (i == 1 || i == 3) 30f else 36f
            val spike = pen.path().m(x - 7f, 62f).q(x - 3f, 48f, x, tip).q(x + 3f, 48f, x + 7f, 62f).l(x - 7f, 62f).shape()
            px.shade(spike, pen.column(x, 7f, -0.2f), m, spec = true, rim = m[5])
            jewel(px, pen, x, tip - 2.5f, 3.6f, if (i % 2 == 0) 0xFFF4F0E8.toInt() else 0xFFE8E2F0.toInt())
        }
        val band = pen.path().m(16f, 58f).q(50f, 66f, 84f, 58f).l(84f, 80f).q(50f, 88f, 16f, 80f).l(16f, 58f).shape()
        px.shade(band, pen.column(50f, 36f, 0.1f), m, spec = true, rim = m[5], texture = { _, y -> if (y % 4 == 0) -0.08f else 0f })
        for (x in listOf(26f, 74f)) jewel(px, pen, x, 71f, 4.2f, if (x < 50f) Stone.SAPPHIRE.argb.toInt() else Stone.EMERALD.argb.toInt())
        medallion(px, pen, photo, 50f, 66f, 12f, 13.5f, m)
    }

    private fun chalice(px: Px, pen: Pen, photo: Bitmap?, m: Ramp) {
        px.shade(pen.ell(50f, 84f, 21f, 5f), pen.dome(50f, 82f, 21f, 7f), m, spec = true)
        px.shade(pen.path().m(40f, 82f).q(46f, 76f, 47f, 66f).l(53f, 66f).q(54f, 76f, 60f, 82f).l(40f, 82f).shape(), pen.column(50f, 10f), m, spec = true, rim = m[5])
        px.shade(pen.ell(50f, 64f, 7f, 4f), pen.dome(50f, 63f, 7f, 4f), m, spec = true)
        val bowl = pen.path().m(22f, 20f).l(78f, 20f).c(78f, 42f, 66f, 58f, 50f, 60f).c(34f, 58f, 22f, 42f, 22f, 20f).shape()
        px.shade(bowl, pen.dome(50f, 28f, 30f, 32f), m, spec = true, rim = m[5])
        px.shade(pen.ell(50f, 20f, 28f, 5.5f), pen.dome(50f, 21f, 28f, 5.5f), m, spec = true)
        px.shade(pen.ell(50f, 20.5f, 24f, 3.6f), facing(0f, -0.4f, 1f), Ramp(rgb(30, 4, 20), rgb(70, 10, 34), rgb(116, 20, 48)), outline = false)
        for (x in listOf(30f, 70f)) jewel(px, pen, x, 34f, 3.4f, Stone.RUBY.argb.toInt())
        medallion(px, pen, photo, 50f, 38f, 11f, 11.5f, m)
    }

    private fun sword(px: Px, pen: Pen, photo: Bitmap?, m: Ramp, iron: Ramp) {
        val blade = pen.path().m(50f, 2f).l(57f, 12f).l(57f, 60f).l(43f, 60f).l(43f, 12f).l(50f, 2f).shape()
        px.shade(blade, pen.column(50f, 7f), iron, spec = true, rim = iron[5])
        for (y in pen.y(10f).toInt()..pen.y(58f).toInt()) px.set(pen.x(50f).toInt(), y, iron[1], 140)
        px.shade(pen.ell(50f, 81f, 5f, 4.5f), pen.dome(50f, 80f, 5f, 4.5f), m, spec = true)
        px.shade(pen.box(46f, 66f, 54f, 78f), pen.column(50f, 4f), leather, texture = { _, y -> if (y % 2 == 0) -0.22f else 0f })
        val guard = pen.path().m(20f, 60f).q(35f, 63f, 50f, 61f).q(65f, 63f, 80f, 60f).l(82f, 66f).q(65f, 69f, 50f, 68f).q(35f, 69f, 18f, 66f).l(20f, 60f).shape()
        px.shade(guard, pen.dome(50f, 62f, 32f, 6f), m, spec = true, rim = m[5])
        for (x in listOf(18f, 82f)) px.shade(pen.ell(x, 63.5f, 4f, 4f), pen.dome(x, 63f, 4f, 4f), m, spec = true)
        medallion(px, pen, photo, 50f, 64f, 9f, 9f, m, 2.6f)
    }

    private fun amulet(px: Px, pen: Pen, photo: Bitmap?, m: Ramp) {
        for (side in listOf(-1f, 1f)) {
            for (i in 0..9) {
                val t = i / 9f
                val x = 50f + side * (34f - 34f * t) * (1f - 0.1f * t)
                val y = 4f + 30f * t * t
                px.shade(pen.ell(x, y, 2.2f, 1.6f), pen.dome(x, y, 2.2f, 1.6f), m, outline = true)
            }
        }
        px.shade(minus(pen.ell(50f, 37f, 4f, 4f), pen.ell(50f, 37f, 2f, 2f)), pen.dome(50f, 37f, 4f, 4f), m, spec = true)
        // A scalloped frame.
        val frame = union(pen.ell(50f, 62f, 25f, 24f), *Array(12) { i -> val a = i * PI / 6; pen.ell(50f + cos(a).toFloat() * 24f, 62f + sin(a).toFloat() * 23f, 4f, 4f) })
        px.shade(frame, pen.dome(50f, 60f, 28f, 27f), m, spec = true, rim = m[5])
        medallion(px, pen, photo, 50f, 62f, 18f, 17f, m, 2.4f)
        px.shade(pen.path().m(50f, 86f).q(45f, 92f, 50f, 98f).q(55f, 92f, 50f, 86f).shape(), pen.dome(50f, 92f, 4f, 5f), R.around(Stone.RUBY.argb.toInt()), spec = true)
    }

    private fun horn(px: Px, pen: Pen, photo: Bitmap?, m: Ramp) {
        val t = Tube.curve(pen.x(16f), pen.y(30f), pen.x(40f), pen.y(78f), pen.x(74f), pen.y(84f), pen.x(90f), pen.y(58f), pen.r(15f), pen.r(2.5f), 22, 0.9f)
        px.shade(t.shape, t.normal, R.horn, texture = { x, y -> if ((x + y) % 7 == 0) -0.12f else 0f }, rim = R.horn[4])
        // Metal fittings: the mouth and two bands.
        px.shade(pen.ell(16f, 30f, 7f, 15f), pen.dome(16f, 30f, 7f, 15f), m, spec = true)
        px.shade(pen.ell(17f, 30f, 4f, 11.5f), facing(-0.5f, 0f, 1f), Ramp(rgb(30, 14, 18), rgb(60, 30, 30), rgb(90, 48, 40)), outline = false)
        for (k in listOf(0.82f, 0.95f)) {
            val tt = k
            val bx = (1 - tt).let { u -> u * u * u * 16f + 3 * u * u * tt * 40f + 3 * u * tt * tt * 74f + tt * tt * tt * 90f }
            val by = (1 - tt).let { u -> u * u * u * 30f + 3 * u * u * tt * 78f + 3 * u * tt * tt * 84f + tt * tt * tt * 58f }
            px.shade(pen.ell(bx, by, 3.2f, 4.6f), pen.dome(bx, by, 3.2f, 4.6f), m, spec = true)
        }
        px.shade(pen.ell(90f, 56f, 3.4f, 3.4f), pen.dome(90f, 56f, 3.4f, 3.4f), m, spec = true)
        medallion(px, pen, photo, 46f, 64f, 11f, 10.5f, m)
    }

    private fun key(px: Px, pen: Pen, photo: Bitmap?, m: Ramp) {
        px.shade(pen.box(46.5f, 50f, 53.5f, 92f), pen.column(50f, 3.5f), m, spec = true, rim = m[5])
        px.shade(pen.path().m(53f, 76f).l(68f, 76f).l(68f, 80f).l(63f, 80f).l(63f, 84f).l(68f, 84f).l(68f, 90f).l(53f, 90f).l(53f, 76f).shape(), facing(-0.2f, -0.3f, 1f), m, spec = true)
        px.shade(pen.ell(50f, 53f, 7f, 3f), pen.dome(50f, 53f, 7f, 3f), m, spec = true)
        // An open-work bow: a ring with four lobes.
        val bow = union(pen.ell(50f, 28f, 23f, 23f), pen.ell(50f, 4f, 5f, 5f), pen.ell(26f, 28f, 5f, 5f), pen.ell(74f, 28f, 5f, 5f))
        px.shade(bow, pen.dome(50f, 26f, 26f, 26f), m, spec = true, rim = m[5])
        medallion(px, pen, photo, 50f, 28f, 16.5f, 16.5f, m, 1.8f)
    }

    private fun ring(px: Px, pen: Pen, photo: Bitmap?, m: Ramp) {
        val band = minus(pen.ell(50f, 72f, 32f, 15f), pen.ell(50f, 70f, 23f, 8f))
        px.shade(band, pen.dome(50f, 70f, 32f, 16f, flat = -0.2f), m, spec = true, rim = m[5])
        // Prongs and the setting, then the moment as the stone.
        for (x in listOf(33f, 67f)) px.shade(pen.path().m(x - 3f, 64f).l(x, 34f).l(x + 3f, 64f).l(x - 3f, 64f).shape(), pen.column(x, 3f), m, spec = true)
        px.shade(pen.path().m(32f, 60f).q(50f, 70f, 68f, 60f).l(62f, 66f).q(50f, 74f, 38f, 66f).l(32f, 60f).shape(), pen.dome(50f, 64f, 20f, 8f), m, spec = true)
        medallion(px, pen, photo, 50f, 44f, 18f, 16f, m, 2.6f)
        for (x in listOf(34f, 66f)) px.shade(pen.ell(x, 33f, 2.6f, 2.6f), pen.dome(x, 32f, 2.6f, 2.6f), m, spec = true)
    }

    private fun lantern(px: Px, pen: Pen, photo: Bitmap?, m: Ramp, f: Int) {
        px.shade(minus(pen.ell(50f, 8f, 7f, 6f), pen.ell(50f, 8f, 4f, 3f)), pen.dome(50f, 8f, 7f, 6f), m, spec = true)
        px.shade(pen.path().m(34f, 24f).l(40f, 13f).l(60f, 13f).l(66f, 24f).l(34f, 24f).shape(), pen.dome(50f, 16f, 18f, 10f), m, spec = true, rim = m[5])
        val glass = pen.box(32f, 24f, 68f, 74f)
        picture(px, photo, glass, glass = false, warm = true)
        // Candle glow inside, flickering.
        val flick = if (f % 2 == 0) 1f else 0.85f
        px.glow(pen.x(50f), pen.y(52f), pen.r(22f), rgb(255, 210, 120), (70 * flick).toInt(), 4, onlyDrawn = true)
        for (x in listOf(32f, 50f, 68f)) px.shade(pen.box(x - 2f, 22f, x + 2f, 76f), pen.column(x, 2f), m, spec = true)
        for (y in listOf(24f, 74f)) px.shade(pen.box(30f, y - 2f, 70f, y + 2f), facing(0f, -0.4f, 1f), m, spec = true)
        px.shade(pen.path().m(28f, 76f).l(72f, 76f).l(68f, 86f).l(32f, 86f).l(28f, 76f).shape(), pen.dome(50f, 78f, 24f, 10f), m, spec = true, rim = m[5])
    }

    /* ---------- Mirrors ---------- */

    /** Where the picture sits inside a mirror of a given size, as fractions of the sprite. */
    fun mirrorWindow(w: Int, h: Int): FloatArray {
        val f = mirrorFrame(w, h)
        return floatArrayOf(f[0] / w, f[1] / h, f[2] / w, f[3] / h)
    }

    /** Inner oval of the mirror: cx, cy, rx, ry in pixels. */
    private fun mirrorFrame(w: Int, h: Int): FloatArray {
        val border = max(2.5f, w * 0.11f)
        val top = max(3f, h * 0.1f)
        val cx = w / 2f
        val ry = (h - top - 2f) / 2f - border
        val cy = top + border + ry
        val rx = w / 2f - 1.5f - border
        return floatArrayOf(cx, cy, rx, ry)
    }

    /** An oval wall mirror with a crest; the moment shows in the glass ([still] for a clip's first frame). */
    fun mirror(still: Bitmap?, metal: Metal, w: Int, h: Int, f: Int = 0, empty: Boolean = false): Px {
        val px = Px(w, h)
        val m = Metals.of(metal)
        val (cx, cy, rx, ry) = mirrorFrame(w, h).toList()
        val border = max(2.5f, w * 0.11f)
        val outer = ellipse(cx, cy, rx + border, ry + border)
        px.shade(outer, dome(cx, cy, rx + border, ry + border, flat = -0.3f), m, spec = true, rim = m[5], texture = { x, y -> if ((x * 3 + y * 5) % 11 == 0) -0.18f else 0f })
        // Little scrolls on the frame.
        if (w >= 32) for (k in 0 until 8) {
            val a = k * PI / 4 + PI / 8
            val sx = (cx + cos(a) * (rx + border * 0.5f)).toInt()
            val sy = (cy + sin(a) * (ry + border * 0.5f)).toInt()
            px.set(sx, sy, m[6]); px.set(sx + 1, sy + 1, m[1])
        }
        // A crest: a small gem in a fan.
        val crestR = max(2f, w * 0.1f)
        val crest = ellipse(cx, cy - ry - border * 0.6f, crestR * 2f, crestR * 1.3f)
        px.shade(crest, dome(cx, cy - ry - border * 0.6f, crestR * 2f, crestR * 1.3f), m, spec = true)
        val gr = max(1.5f, crestR * 0.8f)
        px.shade(ellipse(cx, cy - ry - border * 0.75f, gr, gr), dome(cx, cy - ry - border * 0.75f, gr, gr), R.around(Stone.SAPPHIRE.argb.toInt()), spec = true)
        val glassShape = ellipse(cx, cy, rx, ry)
        if (empty) {
            px.shade(glassShape, dome(cx, cy, rx, ry), Ramp(rgb(18, 26, 44), rgb(30, 46, 70), rgb(50, 76, 104), rgb(84, 116, 146), rgb(140, 172, 200)), outline = false)
        } else {
            picture(px, still, glassShape, glass = true)
        }
        // A shimmer of enchantment that runs round the glass.
        val m2 = px.cover(glassShape)
        val k = Math.floorMod(f, 8) / 8f * 2 * PI
        for (y in 0 until h) for (x in 0 until w) {
            if (!m2[y * w + x]) continue
            val a = atan2(y + 0.5f - cy, x + 0.5f - cx)
            val edge = ((x + 0.5f - cx) / rx).let { it * it } + ((y + 0.5f - cy) / ry).let { it * it }
            if (edge > 0.72f && abs(((a - k + 3 * PI) % (2 * PI)) - PI) < 0.5) px.set(x, y, rgb(170, 230, 255), 90)
        }
        outline(px, m[0])
        return px
    }

    /* ---------- Chests ---------- */

    /** An iron-bound chest with a rune lock that glows; [open] shows it lifted, light pouring out. */
    fun chest(metal: Metal, w: Int, h: Int, f: Int = 0, open: Boolean = false): Px {
        val px = Px(w, h)
        val pen = Pen(px, 1f)
        val m = Metals.of(metal)
        val wood = Metals.oak
        // Body.
        px.shade(pen.box(10f, 50f, 90f, 86f), facing(-0.2f, -0.2f, 1f), wood, texture = { x, y -> noise(x / 5, y, 4) * 0.2f + if (y % 5 == 0) -0.25f else 0f })
        if (open) {
            // The lid thrown back, its dark inside showing, and treasure glowing in the box.
            px.shade(pen.path().m(12f, 46f).l(88f, 46f).l(82f, 14f).l(18f, 14f).l(12f, 46f).shape(), facing(0f, 0.5f, 1f), wood, hi = 3, texture = { x, y -> noise(x / 5, y, 6) * 0.2f })
            px.shade(pen.box(16f, 18f, 84f, 44f), facing(0f, 0.6f, 1f), Ramp(rgb(20, 8, 14), rgb(34, 16, 22), rgb(50, 26, 30)), outline = false)
            for (i in 0 until 9) px.shade(pen.ell(18f + i * 8f, 49f - (i % 3), 5f, 3.2f), pen.dome(18f + i * 8f, 48f, 5f, 3.2f), Metals.gold, spec = true)
            px.glow(pen.x(50f), pen.y(40f), pen.r(42f), rgb(255, 220, 120), 110, 5)
        } else {
            px.shade(pen.path().m(10f, 50f).l(10f, 34f).c(10f, 20f, 90f, 20f, 90f, 34f).l(90f, 50f).l(10f, 50f).shape(), pen.dome(50f, 38f, 42f, 18f), wood, texture = { x, y -> noise(x / 5, y, 5) * 0.2f + if (y % 5 == 0) -0.2f else 0f })
        }
        // Iron bands.
        for (bx in listOf(22f, 78f)) {
            px.shade(pen.box(bx - 4f, if (open) 50f else 25f, bx + 4f, 86f), pen.column(bx, 4f), m, spec = true)
        }
        px.shade(pen.box(8f, 48f, 92f, 53f), facing(0f, -0.5f, 1f), m, spec = true)
        if (!open) {
            // The lock plate and its rune.
            px.shade(pen.box(40f, 44f, 60f, 68f), facing(-0.2f, -0.3f, 1f), m, spec = true)
            val pulse = 0.55f + 0.45f * sin(f / 8f * 2 * PI).toFloat()
            val rune = Glyphs.seal
            val k = if (w >= 40) 1 else 0
            if (k > 0) {
                Glyphs.stamp(px, rune, pen.x(50f).toInt() - 3, pen.y(52f).toInt() - 1, rgb(120, 240, 255), (180 + 75 * pulse).toInt().coerceAtMost(255))
            } else px.set(pen.x(50f).toInt(), pen.y(56f).toInt(), rgb(120, 240, 255))
            px.glow(pen.x(50f), pen.y(56f), pen.r(16f), rgb(120, 230, 255), (60 * pulse).toInt(), 3)
        }
        return px
    }
}

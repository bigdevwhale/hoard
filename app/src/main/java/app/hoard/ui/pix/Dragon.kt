package app.hoard.ui.pix

import app.hoard.domain.Hue
import app.hoard.domain.Stage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The dragon asleep on its hoard, curled up, head on its paws, facing left. It breathes over
 * [FRAMES] frames; [awake] opens one eye (when it rolls a treasure out of the pile).
 */
object Dragon {
    const val FRAMES = 8

    fun width(stage: Stage) = when (stage) {
        Stage.HATCHLING -> 50
        Stage.YOUNG -> 68
        Stage.GROWN -> 88
        Stage.ELDER -> 108
    }

    fun height(stage: Stage) = (width(stage) * 0.6f).roundToInt()

    private val smoke = Ramp(rgb(96, 88, 104), rgb(140, 134, 146), rgb(186, 182, 190), rgb(220, 218, 224))

    fun frame(hue: Hue, stage: Stage, f: Int, awake: Boolean = false): Px {
        val w = width(stage)
        val px = Px(w, height(stage))
        val pen = Pen(px, 1f)
        val col = Dragons.of(hue)
        val ph = (f % FRAMES) / FRAMES.toFloat()
        val breath = sin(ph * 2 * PI).toFloat()
        val young = stage == Stage.HATCHLING
        val horn = when (stage) { Stage.HATCHLING -> 0.45f; Stage.YOUNG -> 0.7f; Stage.GROWN -> 1f; Stage.ELDER -> 1.3f }
        // A hatchling is mostly head; an elder is long in the body.
        val hk = if (young) 1.25f else if (stage == Stage.ELDER) 0.92f else 1f
        val scales = { x: Int, y: Int -> val r = y / 2; val xx = x + (r % 2) * 2; if (y % 2 == 1 && xx % 4 == 0) -0.1f else 0f }

        // Hind leg and haunch.
        px.shade(pen.ell(80f, 80f, 9f, 7f), pen.dome(80f, 78f, 9f, 8f), col.scale, texture = scales, rim = col.scale[5])
        // The body, rising and falling.
        val ry = 13f + breath * 0.7f
        val bcy = 88f - ry - 2f
        px.shade(pen.ell(62f, bcy, 27f, ry), pen.dome(60f, bcy - 2f, 27f, ry + 2f), col.scale, texture = scales, rim = col.scale[5])
        // Spines along the back ridge.
        val spines = when (stage) { Stage.HATCHLING -> 4; Stage.YOUNG -> 5; Stage.GROWN -> 6; Stage.ELDER -> 8 }
        for (i in 0 until spines) {
            val a = PI * (1.55 + 0.4 * i / (spines - 1).coerceAtLeast(1))
            val bx = 62f + cos(a).toFloat() * 26f
            val by = bcy + sin(a).toFloat() * (ry - 0.5f)
            val len = if (young) 3.5f else 5f
            val spine = pen.path().m(bx - 2.2f, by + 1f).l(bx + 1.5f, by - len).l(bx + 2.2f, by + 1f).shape()
            px.shade(spine, facing(0.3f, -0.6f, 0.8f), col.horn, lo = 1)
        }
        // The tail wraps round the front of the pile and ends by the head.
        val tail = Tube.curve(pen.x(86f), pen.y(78f), pen.x(104f), pen.y(90f), pen.x(72f), pen.y(93f), pen.x(42f), pen.y(87f), pen.r(7f), pen.r(1.8f), 24, 0.8f)
        px.shade(tail.shape, tail.normal, col.scale, texture = scales, rim = col.scale[5])
        val tipX = 39.5f; val tipY = 86.5f
        px.shade(pen.path().m(tipX + 3f, tipY - 1f).l(tipX - 2f, tipY - 4.5f).l(tipX - 5f, tipY).l(tipX - 2f, tipY + 2.5f).l(tipX + 3f, tipY + 1f).shape(), facing(-0.3f, -0.4f, 1f), col.wing, lo = 1)

        // A folded wing: the wrist up front, fingers fanned back and down, the membrane scalloped between their tips.
        val lift = breath * 1.1f
        val wy = if (young) 5f else 0f
        val wrist = 46f to 49f + wy - lift * 1.3f
        val t1 = 92f to 63f + wy * 0.6f - lift * 0.5f
        val t2 = 80f to 73f
        val t3 = 66f to 75f
        val wing = pen.path().m(40f, 72f).q(40f, 58f + wy, wrist.first, wrist.second)
            .q(72f, 46f + wy - lift, t1.first, t1.second)
            .q(82f, 64f, t2.first, t2.second)
            .q(70f, 66f, t3.first, t3.second)
            .q(54f, 73f, 40f, 72f).shape()
        px.shade(wing, pen.dome(62f, 58f + wy, 28f, 16f), col.wing, lo = 1, texture = { x, y -> noise(x, y, 7) * 0.05f }, rim = col.wing[4])
        val (sx, sy) = pen.at(wrist.first, wrist.second)
        for ((tx, ty) in listOf(t1, t2, t3)) {
            val (ex, ey) = pen.at(tx, ty)
            px.line(sx, sy, ex, ey, col.wing[0])
            px.set(ex, ey, col.horn[3])
        }
        val (shx, shy) = pen.at(40f, 70f)
        px.line(shx, shy, sx, sy, col.wing[0])
        px.set(sx, sy - 1, col.horn[4]); px.set(sx - 1, sy - 2, col.horn[3])

        // Neck, from the shoulders down to the head on the ground; plates underneath.
        val hx = 20f; val hy = 79f - (hk - 1f) * 6f
        val neck = Tube.curve(pen.x(44f), pen.y(72f), pen.x(36f), pen.y(68f), pen.x(30f), pen.y(74f), pen.x(hx + 6f), pen.y(hy - 1f), pen.r(7f), pen.r(5.5f * hk), 12)
        val nm = px.shade(neck.shape, neck.normal, col.scale, texture = scales, rim = col.scale[5])
        for (y in 0 until px.h) for (x in 0 until px.w) if (nm[y * px.w + x]) {
            val n = neck.normal(x + 0.5f, y + 0.5f)
            if (n.y > 0.62f) px.set(x, y, if (y % 2 == 0) col.belly[1] else col.belly[2])
        }
        // Front paw under the chin.
        px.shade(pen.ell(31f, 85.5f, 7f, 3.4f), pen.dome(31f, 84.5f, 7f, 3.4f), col.scale, lo = 1)
        for (c in 0..2) pen.set(25.5f + c * 2.4f, 88.4f, col.horn[3])

        // Horns sweep back from the brow, behind the head.
        for ((dx, k) in listOf(3f to 0.75f, 0f to 1f)) {
            val h = Tube.curve(pen.x(hx + 3f + dx), pen.y(hy - 5f * hk), pen.x(hx + 7f + dx), pen.y(hy - 9f * hk), pen.x(hx + 9f + dx), pen.y(hy - 11f * hk - 2f * horn), pen.x(hx + 13f + 3f * horn * k + dx), pen.y(hy - 11f * hk - 6f * horn * k), pen.r(2.2f * hk), pen.r(0.5f), 10)
            px.shade(h.shape, h.normal, col.horn, texture = { _, y -> if (y % 3 == 0) -0.15f else 0f })
        }
        // Head: skull, a long snout, a heavy brow.
        px.shade(pen.ell(hx + 1f, hy - 1f, 8f * hk, 6.4f * hk), pen.dome(hx + 0.5f, hy - 2f, 8f * hk, 6.6f * hk), col.scale, rim = col.scale[5])
        val snout = pen.path().m(hx - 1f * hk, hy - 5f * hk).q(hx - 9f * hk, hy - 4.6f * hk, hx - 14f * hk, hy - 1f * hk)
            .q(hx - 15.5f * hk, hy + 3.5f * hk, hx - 11f * hk, hy + 4.4f * hk).l(hx + 3f * hk, hy + 4.6f * hk).shape()
        px.shade(snout, pen.dome(hx - 6f * hk, hy - 1.5f * hk, 10f * hk, 6f * hk), col.scale, rim = col.scale[5])
        px.shade(pen.ell(hx + 0.5f, hy - 4.4f * hk, 4.8f * hk, 1.9f * hk), pen.dome(hx + 0.5f, hy - 4.8f * hk, 4.8f * hk, 2.2f * hk), col.scale, lo = 2, outline = false)
        // Jaw line and nostril.
        val (jx0, jy) = pen.at(hx - 13f * hk, hy + 2.6f * hk)
        val (jx1, _) = pen.at(hx + 2f * hk, hy + 2.6f * hk)
        px.hline(jx0 + 1, jx1, jy, col.scale[1])
        val (nx, ny) = pen.at(hx - 12.5f * hk, hy - 1.6f * hk)
        px.set(nx, ny, col.scale[0]); px.set(nx + 1, ny, col.scale[1])
        // The eye: closed in sleep, or a slit open and glowing.
        val (ex, ey) = pen.at(hx - 0.5f * hk, hy - 2f * hk)
        val ew = (pen.r(2.4f * hk)).roundToInt().coerceAtLeast(1)
        if (awake) {
            for (i in -ew..ew) { px.set(ex + i, ey - 1, col.scale[0]); px.set(ex + i, ey, col.eye); px.set(ex + i, ey + 1, col.scale[1]) }
            px.set(ex, ey, OUT)
            px.glow(ex + 0.5f, ey + 0.5f, ew * 2.4f + 2f, col.eye, 90, 3)
        } else {
            for (i in -ew..ew) px.set(ex + i, ey + if (kotlin.math.abs(i) == ew) -1 else 0, col.scale[0])
        }
        // A little frill behind the jaw.
        px.shade(pen.path().m(hx + 5f * hk, hy).l(hx + 11f * hk, hy - 1.5f * hk).l(hx + 9f * hk, hy + 1.5f * hk).l(hx + 12f * hk, hy + 3.5f * hk).l(hx + 5f * hk, hy + 3.5f * hk).shape(), facing(0.3f, 0f, 1f), col.wing, lo = 1)
        if (stage == Stage.ELDER) for (i in 0..2) pen.set(hx - 4f + i * 2f, hy + 5.4f + i % 2, col.horn[2])
        // Wisps of smoke from the nostril while asleep.
        if (!awake) for (j in 0 until 2) {
            val p = ((ph + j * 0.5f) % 1f)
            val r = pen.r(1.2f + p * 2.2f)
            val sx = pen.x(hx - 14f * hk - p * 6f)
            val sy = pen.y(hy - 3f - p * 14f)
            px.shade(ellipse(sx, sy, r, r), facing(-0.3f, -0.5f, 1f), smoke, outline = false)
        }
        return px
    }
}

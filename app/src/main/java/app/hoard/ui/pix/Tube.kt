package app.hoard.ui.pix

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A round body swept along a line of points with a radius at each one: a tail, a neck, a drinking horn.
 * Coordinates are sprite pixels. [shape] covers it and [normal] lights it like a cylinder that bends.
 */
class Tube(private val xs: FloatArray, private val ys: FloatArray, private val rs: FloatArray) {
    private class Hit(val d: Float, val r: Float, val nx: Float, val ny: Float)

    private fun nearest(x: Float, y: Float): Hit {
        var best = Float.MAX_VALUE
        var br = 1f
        var bx = 0f
        var by = 0f
        for (i in 0 until xs.size - 1) {
            val ax = xs[i]; val ay = ys[i]
            val dx = xs[i + 1] - ax; val dy = ys[i + 1] - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 < 1e-6f) 0f else (((x - ax) * dx + (y - ay) * dy) / len2).coerceIn(0f, 1f)
            val px = ax + dx * t; val py = ay + dy * t
            val r = rs[i] + (rs[i + 1] - rs[i]) * t
            // Distance measured against the local radius, so a thick part wins over a thin one nearby.
            val d = sqrt((x - px) * (x - px) + (y - py) * (y - py)) / r
            if (d < best) { best = d; br = r; bx = x - px; by = y - py }
        }
        return Hit(best, br, bx, by)
    }

    val shape: Shape
        get() {
            var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
            for (i in xs.indices) {
                x0 = min(x0, xs[i] - rs[i]); y0 = min(y0, ys[i] - rs[i]); x1 = max(x1, xs[i] + rs[i]); y1 = max(y1, ys[i] + rs[i])
            }
            return Shape(x0.toInt() - 1, y0.toInt() - 1, x1.toInt() + 1, y1.toInt() + 1) { x, y -> nearest(x, y).d <= 1f }
        }

    val normal: (Float, Float) -> V3 = { x, y ->
        val h = nearest(x, y)
        val nx = (h.nx / h.r).coerceIn(-1f, 1f)
        val ny = (h.ny / h.r).coerceIn(-1f, 1f)
        val z = sqrt(max(0f, 1 - nx * nx - ny * ny))
        V3(nx, ny, z)
    }

    /** 0 at the first point .. 1 at the last, for the pixel's nearest place along the tube. */
    fun along(x: Float, y: Float): Float {
        var best = Float.MAX_VALUE
        var bt = 0f
        var total = 0f
        val lens = FloatArray(xs.size - 1) { i -> val dx = xs[i + 1] - xs[i]; val dy = ys[i + 1] - ys[i]; sqrt(dx * dx + dy * dy).also { total += it } }
        var acc = 0f
        for (i in 0 until xs.size - 1) {
            val ax = xs[i]; val ay = ys[i]
            val dx = xs[i + 1] - ax; val dy = ys[i + 1] - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 < 1e-6f) 0f else (((x - ax) * dx + (y - ay) * dy) / len2).coerceIn(0f, 1f)
            val px = ax + dx * t; val py = ay + dy * t
            val d = (x - px) * (x - px) + (y - py) * (y - py)
            if (d < best) { best = d; bt = (acc + lens[i] * t) / max(total, 1e-3f) }
            acc += lens[i]
        }
        return bt
    }

    companion object {
        /** Points along a cubic Bézier, with the radius easing from [r0] to [r1]. */
        fun curve(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, r0: Float, r1: Float, n: Int = 16, ease: Float = 1f): Tube {
            val xs = FloatArray(n + 1); val ys = FloatArray(n + 1); val rs = FloatArray(n + 1)
            for (i in 0..n) {
                val t = i / n.toFloat()
                val u = 1 - t
                xs[i] = u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3
                ys[i] = u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3
                rs[i] = r0 + (r1 - r0) * Math.pow(t.toDouble(), ease.toDouble()).toFloat()
            }
            return Tube(xs, ys, rs)
        }
    }
}

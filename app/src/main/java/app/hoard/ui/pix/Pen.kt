package app.hoard.ui.pix

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Draws on a [Px] in design units: a 100 x 100 grid whose ground (y = 88) lands on the sprite's
 * bottom row, scaled by [s] for the dragon's stage and lifted by [dy] pixels for breathing.
 */
class Pen(val px: Px, val s: Float, val dy: Float = 0f, val dx: Float = 0f) {
    private val k = px.w / 100f * s
    private val ground = px.h - 3f

    fun x(v: Float) = px.w / 2f + (v - 50f) * k + dx
    fun y(v: Float) = ground + (v - 88f) * k + dy
    fun r(v: Float) = v * k
    fun ux(p: Float) = (p - px.w / 2f - dx) / k + 50f
    fun uy(p: Float) = (p - ground - dy) / k + 88f

    fun ell(cx: Float, cy: Float, rx: Float, ry: Float = rx) = ellipse(x(cx), y(cy), r(rx), r(ry))
    fun box(x0: Float, y0: Float, x1: Float, y1: Float) = rect(x(x0), y(y0), r(x1 - x0), r(y1 - y0))
    fun dome(cx: Float, cy: Float, rx: Float, ry: Float = rx, flat: Float = 0f) = app.hoard.ui.pix.dome(x(cx), y(cy), r(rx), r(ry), flat)
    fun column(cx: Float, rx: Float, tilt: Float = 0f) = app.hoard.ui.pix.column(x(cx), r(rx), tilt)

    /** A dome normal for a shape drawn rotated by [deg] around [pivot] (design units). */
    fun turnedDome(cx: Float, cy: Float, rx: Float, ry: Float, deg: Float, pivotX: Float, pivotY: Float): (Float, Float) -> V3 {
        val a = Math.toRadians(deg.toDouble())
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        return { px, py ->
            val ux = ux(px) - pivotX
            val uy = uy(py) - pivotY
            // Back into the shape's own, unrotated frame.
            val lx = ux * ca + uy * sa + pivotX
            val ly = -ux * sa + uy * ca + pivotY
            val ddx = ((lx - cx) / rx).coerceIn(-1f, 1f)
            val ddy = ((ly - cy) / ry).coerceIn(-1f, 1f)
            val z = sqrt(max(0f, 1 - ddx * ddx - ddy * ddy))
            val nx = ddx * ca - ddy * sa
            val ny = ddx * sa + ddy * ca
            val n = sqrt(nx * nx + ny * ny + z * z).coerceAtLeast(1e-4f)
            V3(nx / n, ny / n, z / n)
        }
    }

    /** Builds a polygon from lines and curves in design units, optionally rotated about a pivot. */
    inner class Path(private val deg: Float = 0f, private val pivotX: Float = 50f, private val pivotY: Float = 50f, private val mirror: Boolean = false) {
        private val pts = ArrayList<Float>()
        private var cx = 0f
        private var cy = 0f
        private val ca = cos(Math.toRadians(deg.toDouble())).toFloat()
        private val sa = sin(Math.toRadians(deg.toDouble())).toFloat()

        private fun add(ax: Float, ay: Float) {
            val mx = if (mirror) 100f - ax else ax
            val rx = (mx - pivotX) * ca - (ay - pivotY) * sa + pivotX
            val ry = (mx - pivotX) * sa + (ay - pivotY) * ca + pivotY
            pts += x(rx); pts += y(ry)
            cx = ax; cy = ay
        }

        fun m(x: Float, y: Float) = apply { add(x, y) }
        fun l(x: Float, y: Float) = apply { add(x, y) }
        fun c(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, n: Int = 9) = apply {
            val b = bez(cx, cy, x1, y1, x2, y2, x3, y3, n)
            for (i in 0 until n) add(b[i * 2], b[i * 2 + 1])
        }
        fun q(x1: Float, y1: Float, x2: Float, y2: Float, n: Int = 7) = c(cx + (x1 - cx) * 2 / 3, cy + (y1 - cy) * 2 / 3, x2 + (x1 - x2) * 2 / 3, y2 + (y1 - y2) * 2 / 3, x2, y2, n)
        fun shape(): Shape = poly(*pts.toFloatArray())
    }

    fun path(deg: Float = 0f, pivotX: Float = 50f, pivotY: Float = 50f, mirror: Boolean = false) = Path(deg, pivotX, pivotY, mirror)

    /** A design point in sprite pixels, after the same rotation and mirroring a [Path] would apply. */
    fun at(ax: Float, ay: Float, deg: Float = 0f, pivotX: Float = 50f, pivotY: Float = 50f, mirror: Boolean = false): Pair<Int, Int> {
        val a = Math.toRadians(deg.toDouble())
        val mx = if (mirror) 100f - ax else ax
        val rx = (mx - pivotX) * cos(a).toFloat() - (ay - pivotY) * sin(a).toFloat() + pivotX
        val ry = (mx - pivotX) * sin(a).toFloat() + (ay - pivotY) * cos(a).toFloat() + pivotY
        return x(rx).toInt() to y(ry).toInt()
    }

    fun set(x: Float, y: Float, c: Int, a: Int = 255) = px.set(x(x).toInt(), y(y).toInt(), c, a)
}

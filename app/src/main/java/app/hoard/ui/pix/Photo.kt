package app.hoard.ui.pix

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A photo shrunk to a handful of pixels, ready to be struck, cut or painted. Cropped to the middle so
 * the subject (usually there) survives; [fx], [fy] move the crop toward a point of interest (0..1).
 */
class Sample private constructor(val w: Int, val h: Int, val c: IntArray) {
    operator fun get(x: Int, y: Int): Int = c[y.coerceIn(0, h - 1) * w + x.coerceIn(0, w - 1)]

    /** Bilinear, for sampling at a refracted offset. */
    fun at(x: Float, y: Float): Int {
        val x0 = kotlin.math.floor(x).toInt()
        val y0 = kotlin.math.floor(y).toInt()
        val tx = x - x0
        val ty = y - y0
        return mixc(mixc(this[x0, y0], this[x0 + 1, y0], tx), mixc(this[x0, y0 + 1], this[x0 + 1, y0 + 1], tx), ty)
    }

    /**
     * Brightness 0..1, stretched so a dim or washed-out photo still uses the whole range — a coin
     * struck from a dark room should still show a face.
     */
    fun levels(): FloatArray {
        val l = FloatArray(w * h) { lum(c[it]) }
        val sorted = l.sortedArray()
        val lo = sorted[(sorted.size * 0.03f).toInt()]
        val hi = sorted[min(sorted.size - 1, (sorted.size * 0.97f).toInt())]
        // A flat picture (a blank, a wall) stays a flat mid-tone rather than going black.
        if (hi - lo < 0.04f) { l.fill(0.55f); return l }
        val span = max(0.08f, hi - lo)
        for (i in l.indices) l[i] = ((l[i] - lo) / span).coerceIn(0f, 1f)
        return l
    }

    /** Average colour, for a quick tint. */
    fun mean(): Int {
        var r = 0L; var g = 0L; var b = 0L
        c.forEach { r += it.r(); g += it.g(); b += it.b() }
        val n = c.size
        return rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    companion object {
        fun of(src: Bitmap, w: Int, h: Int, fx: Float = 0.5f, fy: Float = 0.5f): Sample {
            val aspect = w.toFloat() / h
            val sw: Int
            val sh: Int
            if (src.width.toFloat() / src.height > aspect) { sh = src.height; sw = (sh * aspect).roundToInt() } else { sw = src.width; sh = (sw / aspect).roundToInt() }
            val x0 = ((src.width - sw) * fx).roundToInt().coerceIn(0, src.width - sw)
            val y0 = ((src.height - sh) * fy).roundToInt().coerceIn(0, src.height - sh)
            // Shrink in two steps: a plain bilinear jump from a big photo straight to 40 px aliases badly.
            var cur = src
            var cw = sw
            var ch = sh
            var crop = Rect(x0, y0, x0 + sw, y0 + sh)
            while (cw > w * 4) {
                val nw = max(w * 2, cw / 2)
                val nh = max(h * 2, ch / 2)
                val next = Bitmap.createBitmap(nw, nh, Bitmap.Config.ARGB_8888)
                Canvas(next).drawBitmap(cur, crop, Rect(0, 0, nw, nh), Paint(Paint.FILTER_BITMAP_FLAG))
                if (cur !== src) cur.recycle()
                cur = next; cw = nw; ch = nh; crop = Rect(0, 0, nw, nh)
            }
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(cur, crop, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
            if (cur !== src) cur.recycle()
            val px = IntArray(w * h)
            out.getPixels(px, 0, w, 0, 0, w, h)
            out.recycle()
            return Sample(w, h, px)
        }

        /** A flat grey stand-in when the photo can't be read. */
        fun blank(w: Int, h: Int) = Sample(w, h, IntArray(w * h) { rgb(128, 120, 112) })
    }
}

/** Posterises a colour to a few levels per channel with ordered dither: a photo turned into pixel paint. */
fun paint(c: Int, x: Int, y: Int, levels: Int = 6, dither: Float = 0.9f): Int {
    val d = bayer(x, y) * dither
    fun q(v: Int): Int {
        val f = v / 255f * (levels - 1) + d
        return (f.roundToInt().coerceIn(0, levels - 1) * 255f / (levels - 1)).roundToInt()
    }
    return rgb(q(c.r()), q(c.g()), q(c.b()))
}

/** Pushes saturation and contrast up a little: tiny pictures read better a bit louder. */
fun punch(c: Int, sat: Float = 1.25f, con: Float = 1.1f): Int {
    val l = lum(c) * 255f
    fun ch(v: Int): Int {
        val s = l + (v - l) * sat
        return ((s - 128f) * con + 128f).roundToInt().coerceIn(0, 255)
    }
    return rgb(ch(c.r()), ch(c.g()), ch(c.b()))
}

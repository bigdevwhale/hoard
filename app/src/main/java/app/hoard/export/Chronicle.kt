package app.hoard.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import app.hoard.R
import app.hoard.data.Settings
import app.hoard.data.Treasure
import app.hoard.data.Vault
import app.hoard.domain.Kind
import app.hoard.domain.Stage
import app.hoard.ui.T
import app.hoard.ui.components.Art
import app.hoard.ui.pix.Dragon
import app.hoard.ui.pix.Px
import java.time.ZoneId

/**
 * The year's hoard as a little printed book: a title page with the dragon, then every treasure with
 * its art, its photo and its legend, two to a page.
 */
object Chronicle {
    private const val W = 420
    private const val H = 595
    private const val M = 32f

    private val ink = Color.rgb(43, 30, 24)
    private val muted = Color.rgb(120, 98, 84)
    private val gold = Color.rgb(170, 120, 34)
    private val paper = Color.rgb(246, 238, 222)

    fun write(context: Context, uri: Uri, year: Int, all: List<Treasure>, s: Settings, stage: Stage, vault: Vault, zone: ZoneId) {
        val serif = ResourcesCompat.getFont(context, R.font.prata) ?: Typeface.SERIF
        val sans = ResourcesCompat.getFont(context, R.font.nunito_regular) ?: Typeface.SANS_SERIF
        val items = all.filter { it.visible(System.currentTimeMillis()) && T.local(it.momentAt, zone).year == year }.sortedBy { it.momentAt }
        val doc = PdfDocument()
        var n = 1
        fun page(draw: (Canvas) -> Unit) {
            val p = doc.startPage(PdfDocument.PageInfo.Builder(W, H, n++).create())
            p.canvas.drawColor(paper)
            draw(p.canvas)
            doc.finishPage(p)
        }
        val name = s.dragonName.ifBlank { context.getString(R.string.dragon_default) }
        page { c ->
            text(c, context.getString(R.string.pdf_title), serif, 13f, muted, M, 120f, W - 2 * M, center = true)
            text(c, name, serif, 34f, ink, M, 150f, W - 2 * M, center = true)
            text(c, year.toString(), serif, 20f, gold, M, 200f, W - 2 * M, center = true)
            pix(c, Dragon.frame(s.hue, stage, 0), RectF(70f, 250f, W - 70f, 250f + (W - 140f) * 0.6f))
            val counts = Kind.entries.map { k -> items.count { it.kindOf == k } }
            val line = Kind.entries.zip(counts).filter { it.second > 0 }.joinToString("  ·  ") { (k, v) -> "${T.kind(context, k)}: $v" }
            text(c, line, sans, 11f, muted, M, 470f, W - 2 * M, center = true)
            text(c, context.getString(R.string.pdf_footer), sans, 9f, muted, M, H - 50f, W - 2 * M, center = true)
        }
        items.chunked(2).forEach { pair ->
            page { c ->
                pair.forEachIndexed { i, t ->
                    val top = M + i * (H - 2 * M) / 2f
                    entry(context, c, t, vault, top, serif, sans)
                }
            }
        }
        context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        doc.close()
    }

    private fun entry(context: Context, c: Canvas, t: Treasure, vault: Vault, top: Float, serif: Typeface, sans: Typeface) {
        val still = vault.still(t.media)
        val art = Art.of(t, still, 64)
        pix(c, art, RectF(M, top + 6f, M + 120f, top + 126f))
        if (still != null) {
            val box = RectF(M + 140f, top + 6f, W - M, top + 126f)
            val src = cover(still, box.width() / box.height())
            c.drawBitmap(still, src, box, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        var y = top + 140f
        y = text(c, T.title(context, t), serif, 16f, ink, M, y, W - 2 * M)
        y = text(c, T.matter(context, t) + " · " + T.hall(context, t.hallOf), sans, 9.5f, gold, M, y + 2f, W - 2 * M)
        y = text(c, T.legend(context, t), serif, 10f, muted, M, y + 4f, W - 2 * M)
        if (t.note.isNotBlank()) text(c, t.note, sans, 10f, ink, M, y + 6f, W - 2 * M, maxLines = 5)
    }

    private fun cover(b: Bitmap, aspect: Float): Rect {
        val a = b.width.toFloat() / b.height
        return if (a > aspect) { val w = (b.height * aspect).toInt(); Rect((b.width - w) / 2, 0, (b.width + w) / 2, b.height) }
        else { val h = (b.width / aspect).toInt(); Rect(0, (b.height - h) / 2, b.width, (b.height + h) / 2) }
    }

    /** Pixel art, square pixels, centred in [box]. */
    private fun pix(c: Canvas, px: Px, box: RectF) {
        val k = minOf(box.width() / px.w, box.height() / px.h)
        val ox = box.left + (box.width() - px.w * k) / 2
        val oy = box.top + (box.height() - px.h * k) / 2
        val p = Paint()
        for (y in 0 until px.h) for (x in 0 until px.w) {
            val col = px.c[y * px.w + x]
            if ((col ushr 24) == 0) continue
            p.color = col
            c.drawRect(ox + x * k, oy + y * k, ox + (x + 1) * k + 0.4f, oy + (y + 1) * k + 0.4f, p)
        }
    }

    /** Draws wrapped text and returns the y under it. */
    private fun text(c: Canvas, s: String, face: Typeface, size: Float, color: Int, x: Float, y: Float, w: Float, center: Boolean = false, maxLines: Int = 6): Float {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = face; textSize = size; this.color = color }
        val l = StaticLayout.Builder.obtain(s, 0, s.length, tp, w.toInt())
            .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        c.save()
        c.translate(x, y)
        l.draw(c)
        c.restore()
        return y + l.height
    }
}

package app.hoard.ui.pix

import app.hoard.domain.Daypart
import app.hoard.domain.Hall
import app.hoard.domain.Sky

/** Small pixel marks: the sign of each hall, the runes of place and weather, and digits for the year. */
object Glyphs {
    val halls: Map<Hall, Array<String>> = mapOf(
        Hall.TRAVELS to arrayOf("....x....", "....x....", "...xxx...", ".x.x.x.x.", "xxxx.xxxx", ".x.x.x.x.", "...xxx...", "....x....", "....x...."),
        Hall.KIN to arrayOf(".........", ".xx...xx.", "xxxx.xxxx", "xxxxxxxxx", "xxxxxxxxx", ".xxxxxxx.", "..xxxxx..", "...xxx...", "....x...."),
        Hall.FEASTS to arrayOf("xxxxxxxxx", "x.......x", "xxxxxxxxx", ".xxxxxxx.", "..xxxxx..", "....x....", "....x....", "..xxxxx..", ".xxxxxxx."),
        Hall.BEASTS to arrayOf("..x...x..", ".xxx.xxx.", "..x...x..", "x.......x", "xx.xxx.xx", "..xxxxx..", ".xxxxxxx.", ".xxxxxxx.", "..xx.xx.."),
        Hall.SKIES to arrayOf("...xxx...", "..xx.....", ".xx....x.", ".xx...xxx", ".xx....x.", ".xx......", "..xx.....", "...xxx...", "........."),
        Hall.HEARTH to arrayOf("....x....", "...xx....", "...xxx...", "..xxxx.x.", ".xxx.xxx.", ".xx...xx.", ".xx...xx.", "..xx.xx..", "...xxx..."),
        Hall.DEEDS to arrayOf("........x", ".......xx", "......xx.", ".x...xx..", "..x.xx...", "...xx....", "..xxx....", ".xx..x...", "xx......."),
        Hall.WONDERS to arrayOf("....x....", "....x....", "...xxx...", "..xxxxx..", "xxxxxxxxx", "..xxxxx..", "...xxx...", "....x....", "....x...."),
    )

    val sky: Map<Sky, Array<String>> = mapOf(
        Sky.CLEAR to arrayOf("x..x..x", ".x.x.x.", "..xxx..", "xxxxxxx", "..xxx..", ".x.x.x.", "x..x..x"),
        Sky.CLOUDS to arrayOf(".......", "..xx...", ".xxxxx.", "xxxxxxx", "xxxxxxx", ".......", "......."),
        Sky.FOG to arrayOf(".......", "xxxxx..", ".......", "..xxxxx", ".......", "xxxxxx.", "......."),
        Sky.RAIN to arrayOf(".xxxx..", "xxxxxx.", ".......", "x..x..x", ".x..x..", "x..x..x", "......."),
        Sky.SNOW to arrayOf("...x...", ".x.x.x.", "..xxx..", "xxx.xxx", "..xxx..", ".x.x.x.", "...x..."),
        Sky.STORM to arrayOf("...xxx.", "..xxx..", ".xxx...", ".xxxxx.", "...xx..", "..xx...", ".x....."),
    )

    val daypart: Map<Daypart, Array<String>> = mapOf(
        Daypart.DAWN to arrayOf(".......", "...x...", ".x...x.", "..xxx..", ".xxxxx.", "xxxxxxx", "......."),
        Daypart.DAY to arrayOf("..xxx..", ".xxxxx.", "xxxxxxx", "xxxxxxx", "xxxxxxx", ".xxxxx.", "..xxx.."),
        Daypart.DUSK to arrayOf(".......", "..xxx..", ".xxxxx.", "xxxxxxx", ".......", "xxxxxxx", "......."),
        Daypart.NIGHT to arrayOf("..xxx..", ".xx....", "xx.....", "xx.....", "xx.....", ".xx....", "..xxx.."),
    )

    val place = arrayOf("...x...", "..xxx..", "..x.x..", ".xx.xx.", ".x...x.", "xx...xx", "xxxxxxx")
    val quest = arrayOf("...x...", "...xx..", "..xxx..", "..xxxx.", ".xxxxx.", "xxxxxxx", ".......")
    val seal = arrayOf("..xxx..", ".x...x.", ".x...x.", "xxxxxxx", "xxx.xxx", "xxx.xxx", "xxxxxxx")

    private val digits = arrayOf(
        arrayOf("xxx", "x.x", "x.x", "x.x", "xxx"), arrayOf(".x.", "xx.", ".x.", ".x.", "xxx"),
        arrayOf("xxx", "..x", "xxx", "x..", "xxx"), arrayOf("xxx", "..x", ".xx", "..x", "xxx"),
        arrayOf("x.x", "x.x", "xxx", "..x", "..x"), arrayOf("xxx", "x..", "xxx", "..x", "xxx"),
        arrayOf("xxx", "x..", "xxx", "x.x", "xxx"), arrayOf("xxx", "..x", ".x.", ".x.", ".x."),
        arrayOf("xxx", "x.x", "xxx", "x.x", "xxx"), arrayOf("xxx", "x.x", "xxx", "..x", "xxx"),
    )

    /** Flat marks. */
    fun stamp(px: Px, pat: Array<String>, x0: Int, y0: Int, c: Int, a: Int = 255) {
        pat.forEachIndexed { j, row -> row.forEachIndexed { i, ch -> if (ch == 'x') px.set(x0 + i, y0 + j, c, a) } }
    }

    /** Raised marks, as if struck in metal: lit face, a shadow below and to the right. */
    fun emboss(px: Px, pat: Array<String>, x0: Int, y0: Int, face: Int, shade: Int, light: Int? = null) {
        fun on(i: Int, j: Int) = j in pat.indices && i in pat[j].indices && pat[j][i] == 'x'
        pat.forEachIndexed { j, row -> row.forEachIndexed { i, ch -> if (ch == 'x' && !on(i + 1, j + 1)) px.set(x0 + i + 1, y0 + j + 1, shade) } }
        pat.forEachIndexed { j, row ->
            row.forEachIndexed { i, ch ->
                if (ch != 'x') return@forEachIndexed
                val top = !on(i, j - 1) || !on(i - 1, j)
                px.set(x0 + i, y0 + j, if (top && light != null) light else face)
            }
        }
    }

    fun number(px: Px, n: Int, cx: Int, y0: Int, face: Int, shade: Int? = null) {
        val s = n.toString()
        val w = s.length * 4 - 1
        s.forEachIndexed { k, ch ->
            val pat = digits[ch - '0']
            val x = cx - w / 2 + k * 4
            if (shade != null) emboss(px, pat, x, y0, face, shade) else stamp(px, pat, x, y0, face)
        }
    }

    /** A glyph alone, for the interface: [scale] pixels per dot. */
    fun icon(pat: Array<String>, color: Int): Px {
        val px = Px(pat[0].length + 2, pat.size + 2)
        emboss(px, pat, 1, 1, color, mixc(color, OUT, 0.7f))
        return px
    }
}

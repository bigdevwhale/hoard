package app.hoard.ui.pix

import app.hoard.domain.Hue
import app.hoard.domain.Metal
import app.hoard.domain.Stone

/** Hue-shifted ramps for everything that shines in the hoard. */
object Metals {
    val gold = Ramp(rgb(52, 22, 26), rgb(98, 50, 28), rgb(150, 92, 32), rgb(200, 138, 40), rgb(234, 182, 64), rgb(252, 220, 112), rgb(255, 246, 196))
    val silver = Ramp(rgb(28, 30, 50), rgb(58, 64, 88), rgb(96, 104, 128), rgb(140, 150, 170), rgb(184, 194, 208), rgb(222, 230, 238), rgb(250, 252, 255))
    val copper = Ramp(rgb(46, 16, 22), rgb(92, 34, 28), rgb(144, 62, 36), rgb(190, 96, 52), rgb(222, 136, 78), rgb(244, 180, 120), rgb(255, 222, 180))
    val rose = Ramp(rgb(58, 20, 38), rgb(108, 46, 58), rgb(160, 84, 82), rgb(206, 126, 108), rgb(234, 168, 140), rgb(250, 206, 180), rgb(255, 238, 222))
    val mithril = Ramp(rgb(14, 34, 58), rgb(30, 70, 102), rgb(56, 116, 148), rgb(98, 166, 190), rgb(148, 210, 222), rgb(204, 240, 244), rgb(246, 255, 255))
    val obsidian = Ramp(rgb(10, 6, 18), rgb(24, 14, 38), rgb(42, 26, 64), rgb(66, 42, 98), rgb(100, 66, 140), rgb(150, 110, 190), rgb(206, 176, 236))
    val star = Ramp(rgb(10, 10, 40), rgb(24, 24, 78), rgb(44, 44, 122), rgb(72, 70, 164), rgb(112, 108, 204), rgb(170, 164, 236), rgb(236, 232, 255))

    fun of(m: Metal): Ramp = when (m) {
        Metal.GOLD -> gold
        Metal.SILVER -> silver
        Metal.COPPER -> copper
        Metal.ROSE -> rose
        Metal.MITHRIL -> mithril
        Metal.OBSIDIAN -> obsidian
        Metal.STAR -> star
    }

    /** Star-metal glitters with tiny points of light. */
    fun glitter(m: Metal): Boolean = m == Metal.STAR

    val diamond = Ramp(rgb(40, 60, 92), rgb(80, 112, 150), rgb(130, 168, 200), rgb(180, 212, 232), rgb(216, 236, 248), rgb(240, 250, 255), rgb(255, 255, 255))
    val moonstone = Ramp(rgb(44, 42, 86), rgb(82, 84, 136), rgb(124, 130, 182), rgb(166, 176, 220), rgb(200, 210, 240), rgb(228, 234, 252), rgb(255, 250, 255))

    fun stone(s: Stone): Ramp = when (s) {
        Stone.DIAMOND -> diamond
        Stone.MOONSTONE -> moonstone
        else -> R.around(s.argb.toInt())
    }

    /** The wood of chests. */
    val oak = Ramp(rgb(36, 16, 22), rgb(64, 30, 28), rgb(98, 50, 36), rgb(134, 74, 46), rgb(170, 102, 60), rgb(204, 138, 84))
}

/** The dragon's colours: scales, belly plates, wing membrane, horns and the glow of its eye. */
class DragonColors(val scale: Ramp, val belly: Ramp, val wing: Ramp, val horn: Ramp, val eye: Int)

object Dragons {
    fun of(h: Hue): DragonColors = when (h) {
        Hue.EMERALD -> DragonColors(
            Ramp(rgb(10, 30, 38), rgb(16, 58, 56), rgb(24, 92, 70), rgb(40, 130, 84), rgb(76, 170, 98), rgb(132, 206, 120), rgb(200, 236, 164)),
            Ramp(rgb(96, 64, 40), rgb(156, 114, 62), rgb(206, 166, 88), rgb(236, 206, 128), rgb(252, 236, 178)),
            Ramp(rgb(30, 22, 52), rgb(54, 40, 88), rgb(86, 62, 128), rgb(124, 92, 168), rgb(164, 132, 204)),
            R.horn, rgb(255, 200, 80),
        )
        Hue.CRIMSON -> DragonColors(
            Ramp(rgb(40, 8, 26), rgb(78, 16, 34), rgb(124, 28, 40), rgb(172, 44, 46), rgb(212, 76, 58), rgb(240, 124, 84), rgb(255, 184, 130)),
            Ramp(rgb(108, 62, 36), rgb(170, 112, 52), rgb(220, 164, 72), rgb(246, 206, 112), rgb(255, 236, 170)),
            Ramp(rgb(46, 14, 30), rgb(84, 26, 44), rgb(128, 44, 58), rgb(176, 72, 74), rgb(216, 112, 98)),
            Ramp(rgb(60, 40, 40), rgb(110, 86, 76), rgb(164, 140, 118), rgb(212, 196, 168), rgb(244, 236, 214)), rgb(255, 222, 96),
        )
        Hue.SAPPHIRE -> DragonColors(
            Ramp(rgb(10, 14, 44), rgb(18, 30, 82), rgb(28, 54, 124), rgb(42, 86, 168), rgb(70, 126, 206), rgb(118, 172, 234), rgb(186, 220, 252)),
            Ramp(rgb(70, 72, 98), rgb(122, 128, 152), rgb(176, 184, 200), rgb(216, 224, 232), rgb(244, 248, 252)),
            Ramp(rgb(16, 30, 54), rgb(26, 54, 86), rgb(40, 86, 120), rgb(62, 124, 156), rgb(98, 168, 192)),
            R.horn, rgb(140, 240, 255),
        )
        Hue.OBSIDIAN -> DragonColors(
            Ramp(rgb(8, 6, 14), rgb(18, 14, 28), rgb(30, 24, 44), rgb(46, 38, 62), rgb(68, 58, 86), rgb(100, 88, 118), rgb(146, 134, 160)),
            Ramp(rgb(70, 24, 20), rgb(124, 44, 26), rgb(182, 76, 32), rgb(226, 124, 44), rgb(250, 178, 72)),
            Ramp(rgb(30, 8, 14), rgb(60, 16, 22), rgb(98, 28, 30), rgb(142, 44, 36), rgb(186, 70, 44)),
            Ramp(rgb(40, 36, 40), rgb(76, 70, 72), rgb(118, 110, 108), rgb(164, 156, 148), rgb(206, 198, 186)), rgb(255, 120, 60),
        )
        Hue.IVORY -> DragonColors(
            Ramp(rgb(70, 56, 70), rgb(116, 100, 106), rgb(162, 146, 142), rgb(204, 190, 176), rgb(232, 222, 204), rgb(248, 242, 228), rgb(255, 255, 248)),
            Ramp(rgb(120, 92, 70), rgb(176, 142, 100), rgb(222, 188, 132), rgb(244, 222, 168), rgb(255, 244, 210)),
            Ramp(rgb(80, 52, 80), rgb(130, 90, 118), rgb(180, 132, 154), rgb(220, 176, 188), rgb(244, 214, 220)),
            Metals.gold, rgb(120, 200, 255),
        )
    }
}

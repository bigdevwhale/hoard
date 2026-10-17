package app.hoard.domain

/** What a moment is struck into. A clip always becomes a [MIRROR]. */
enum class Kind { COIN, GEM, RELIC, MIRROR }

/** Coins and relics are cast in a metal; the first three are there from the start, the rest come from the gnomes. */
enum class Metal(val starter: Boolean) {
    GOLD(true), SILVER(true), COPPER(true), ROSE(false), MITHRIL(false), OBSIDIAN(false), STAR(false),
}

enum class Stone(val starter: Boolean, val argb: Long) {
    RUBY(true, 0xFFD23A4E), SAPPHIRE(true, 0xFF3A6FD8), EMERALD(true, 0xFF2FA96A),
    AMETHYST(false, 0xFF9B5BD6), TOPAZ(false, 0xFFF0A23A), DIAMOND(false, 0xFFCFE6F2), MOONSTONE(false, 0xFFB8C4E8),
}

/** How a gem is cut: the outline of the stone and its facets. */
enum class Cut { ROUND, OVAL, CUSHION, PEAR, HEART, STEP }

/** The shapes a relic can take. Each holds the moment in a medallion. */
enum class RelicForm { CROWN, CHALICE, SWORD, AMULET, HORN, KEY, RING, LANTERN }

/** The halls of the cave, one per kind of moment. */
enum class Hall(val argb: Long) {
    TRAVELS(0xFF5FA8D3), KIN(0xFFE58A8A), FEASTS(0xFFE7A64B), BEASTS(0xFFB98A5A),
    SKIES(0xFF8E9CF0), HEARTH(0xFFE07A4A), DEEDS(0xFFE9C26A), WONDERS(0xFFB07FD9),
}

/** The weather a moment was struck under, as a rune. */
enum class Sky {
    CLEAR, CLOUDS, FOG, RAIN, SNOW, STORM, UNKNOWN;

    companion object {
        /** From a WMO weather code (Open-Meteo). */
        fun ofWmo(code: Int): Sky = when (code) {
            0, 1 -> CLEAR
            2, 3 -> CLOUDS
            45, 48 -> FOG
            in 51..67, in 80..82 -> RAIN
            in 71..77, 85, 86 -> SNOW
            in 95..99 -> STORM
            else -> UNKNOWN
        }
    }
}

enum class Daypart {
    DAWN, DAY, DUSK, NIGHT;

    companion object {
        fun of(hour: Int): Daypart = when (hour) {
            in 5..8 -> DAWN
            in 9..16 -> DAY
            in 17..20 -> DUSK
            else -> NIGHT
        }
    }
}

enum class Season {
    WINTER, SPRING, SUMMER, AUTUMN;

    companion object {
        fun of(month: Int): Season = when (month) {
            12, 1, 2 -> WINTER
            3, 4, 5 -> SPRING
            6, 7, 8 -> SUMMER
            else -> AUTUMN
        }
    }
}

/** The colour of the sleeping dragon. */
enum class Hue { EMERALD, CRIMSON, SAPPHIRE, OBSIDIAN, IVORY }

/** The dragon grows with the hoard. */
enum class Stage(val from: Int) {
    HATCHLING(0), YOUNG(8), GROWN(30), ELDER(100);

    companion object {
        fun of(count: Int): Stage = entries.last { count >= it.from }
        fun next(s: Stage): Stage? = entries.getOrNull(s.ordinal + 1)
    }
}

/** Something that can be unlocked by a gnome's errand. */
sealed interface Material {
    val key: String

    data class M(val metal: Metal) : Material { override val key get() = "m:" + metal.name }
    data class S(val stone: Stone) : Material { override val key get() = "s:" + stone.name }

    companion object {
        /** The order the gnomes pay in. */
        val rewards: List<Material> = listOf(
            M(Metal.ROSE), S(Stone.AMETHYST), M(Metal.MITHRIL), S(Stone.TOPAZ),
            M(Metal.OBSIDIAN), S(Stone.DIAMOND), M(Metal.STAR), S(Stone.MOONSTONE),
        )
    }
}

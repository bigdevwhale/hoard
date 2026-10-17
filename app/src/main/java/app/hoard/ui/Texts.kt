package app.hoard.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.hoard.R
import app.hoard.data.Treasure
import app.hoard.domain.Cut
import app.hoard.domain.Daypart
import app.hoard.domain.Feature
import app.hoard.domain.Hall
import app.hoard.domain.Hue
import app.hoard.domain.Kind
import app.hoard.domain.Material
import app.hoard.domain.Metal
import app.hoard.domain.RelicForm
import app.hoard.domain.Sky
import app.hoard.domain.Stage
import app.hoard.domain.Stone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

/** Names and words, in the app's language. Plain functions over a [Context] so receivers can use them too. */
object T {
    fun hall(c: Context, h: Hall): String = c.getString(
        when (h) {
            Hall.TRAVELS -> R.string.hall_travels
            Hall.KIN -> R.string.hall_kin
            Hall.FEASTS -> R.string.hall_feasts
            Hall.BEASTS -> R.string.hall_beasts
            Hall.SKIES -> R.string.hall_skies
            Hall.HEARTH -> R.string.hall_hearth
            Hall.DEEDS -> R.string.hall_deeds
            Hall.WONDERS -> R.string.hall_wonders
        }
    )

    fun hallHint(c: Context, h: Hall): String = c.getString(
        when (h) {
            Hall.TRAVELS -> R.string.hall_travels_hint
            Hall.KIN -> R.string.hall_kin_hint
            Hall.FEASTS -> R.string.hall_feasts_hint
            Hall.BEASTS -> R.string.hall_beasts_hint
            Hall.SKIES -> R.string.hall_skies_hint
            Hall.HEARTH -> R.string.hall_hearth_hint
            Hall.DEEDS -> R.string.hall_deeds_hint
            Hall.WONDERS -> R.string.hall_wonders_hint
        }
    )

    fun kind(c: Context, k: Kind): String = c.getString(
        when (k) {
            Kind.COIN -> R.string.kind_coin
            Kind.GEM -> R.string.kind_gem
            Kind.RELIC -> R.string.kind_relic
            Kind.MIRROR -> R.string.kind_mirror
        }
    )

    fun kindHint(c: Context, k: Kind): String = c.getString(
        when (k) {
            Kind.COIN -> R.string.kind_coin_hint
            Kind.GEM -> R.string.kind_gem_hint
            Kind.RELIC -> R.string.kind_relic_hint
            Kind.MIRROR -> R.string.kind_mirror_hint
        }
    )

    fun metal(c: Context, m: Metal): String = c.getString(
        when (m) {
            Metal.GOLD -> R.string.metal_gold
            Metal.SILVER -> R.string.metal_silver
            Metal.COPPER -> R.string.metal_copper
            Metal.ROSE -> R.string.metal_rose
            Metal.MITHRIL -> R.string.metal_mithril
            Metal.OBSIDIAN -> R.string.metal_obsidian
            Metal.STAR -> R.string.metal_star
        }
    )

    fun stone(c: Context, s: Stone): String = c.getString(
        when (s) {
            Stone.RUBY -> R.string.stone_ruby
            Stone.SAPPHIRE -> R.string.stone_sapphire
            Stone.EMERALD -> R.string.stone_emerald
            Stone.AMETHYST -> R.string.stone_amethyst
            Stone.TOPAZ -> R.string.stone_topaz
            Stone.DIAMOND -> R.string.stone_diamond
            Stone.MOONSTONE -> R.string.stone_moonstone
        }
    )

    fun cut(c: Context, k: Cut): String = c.getString(
        when (k) {
            Cut.ROUND -> R.string.cut_round
            Cut.OVAL -> R.string.cut_oval
            Cut.CUSHION -> R.string.cut_cushion
            Cut.PEAR -> R.string.cut_pear
            Cut.HEART -> R.string.cut_heart
            Cut.STEP -> R.string.cut_step
        }
    )

    fun relic(c: Context, r: RelicForm): String = c.getString(
        when (r) {
            RelicForm.CROWN -> R.string.relic_crown
            RelicForm.CHALICE -> R.string.relic_chalice
            RelicForm.SWORD -> R.string.relic_sword
            RelicForm.AMULET -> R.string.relic_amulet
            RelicForm.HORN -> R.string.relic_horn
            RelicForm.KEY -> R.string.relic_key
            RelicForm.RING -> R.string.relic_ring
            RelicForm.LANTERN -> R.string.relic_lantern
        }
    )

    fun material(c: Context, m: Material): String = when (m) {
        is Material.M -> metal(c, m.metal)
        is Material.S -> stone(c, m.stone)
    }

    fun hue(c: Context, h: Hue): String = c.getString(
        when (h) {
            Hue.EMERALD -> R.string.hue_emerald
            Hue.CRIMSON -> R.string.hue_crimson
            Hue.SAPPHIRE -> R.string.hue_sapphire
            Hue.OBSIDIAN -> R.string.hue_obsidian
            Hue.IVORY -> R.string.hue_ivory
        }
    )

    fun stage(c: Context, s: Stage): String = c.getString(
        when (s) {
            Stage.HATCHLING -> R.string.stage_hatchling
            Stage.YOUNG -> R.string.stage_young
            Stage.GROWN -> R.string.stage_grown
            Stage.ELDER -> R.string.stage_elder
        }
    )

    fun feature(c: Context, f: Feature): String = c.getString(
        when (f) {
            Feature.MUSHROOMS -> R.string.feat_mushrooms
            Feature.STALACTITES -> R.string.feat_stalactites
            Feature.CRYSTALS -> R.string.feat_crystals
            Feature.BANNERS -> R.string.feat_banners
            Feature.POOL -> R.string.feat_pool
            Feature.LANTERN -> R.string.feat_lantern
            Feature.BATS -> R.string.feat_bats
            Feature.BONES -> R.string.feat_bones
            Feature.SKYLIGHT -> R.string.feat_skylight
        }
    )

    fun featureGoal(c: Context, f: Feature): String {
        val n = f.goal
        return when (f) {
            Feature.CRYSTALS -> c.resources.getQuantityString(R.plurals.goal_gems, n, n)
            Feature.BANNERS -> c.resources.getQuantityString(R.plurals.goal_relics, n, n)
            Feature.LANTERN -> c.resources.getQuantityString(R.plurals.goal_quests, n, n)
            Feature.BATS -> c.resources.getQuantityString(R.plurals.goal_nights, n, n)
            else -> c.resources.getQuantityString(R.plurals.goal_treasures, n, n)
        }
    }

    fun quest(c: Context, id: String): String {
        // R.array.quests lists them in the order of Quests.ids.
        val all = c.resources.getStringArray(R.array.quests)
        return all.getOrNull(app.hoard.domain.Quests.ids.indexOf(id)) ?: id
    }

    fun sky(c: Context, s: Sky): String = c.getString(
        when (s) {
            Sky.CLEAR -> R.string.sky_clear
            Sky.CLOUDS -> R.string.sky_clouds
            Sky.FOG -> R.string.sky_fog
            Sky.RAIN -> R.string.sky_rain
            Sky.SNOW -> R.string.sky_snow
            Sky.STORM -> R.string.sky_storm
            Sky.UNKNOWN -> R.string.sky_unknown
        }
    )

    fun daypart(c: Context, d: Daypart): String = c.getString(
        when (d) {
            Daypart.DAWN -> R.string.part_dawn
            Daypart.DAY -> R.string.part_day
            Daypart.DUSK -> R.string.part_dusk
            Daypart.NIGHT -> R.string.part_night
        }
    )

    /* ---------- The dragon's calendar ---------- */

    fun month(c: Context, m: Int): String = c.resources.getStringArray(R.array.months)[m - 1]

    /** "17th of Leaffall" / "17 Листопада". */
    fun day(c: Context, d: LocalDate): String {
        val gen = c.resources.getStringArray(R.array.months_of)[d.monthValue - 1]
        return c.getString(R.string.day_of_month, ordinal(c, d.dayOfMonth), gen)
    }

    /** "17th of Leaffall, 2026". */
    fun date(c: Context, d: LocalDate): String = c.getString(R.string.date_with_year, day(c, d), d.year)

    private fun ordinal(c: Context, n: Int): String {
        if (c.resources.configuration.locales[0].language != "en") return n.toString()
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        return "$n$suffix"
    }

    fun local(ms: Long, zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(ms).atZone(zone)

    /** The name shown for a treasure: its own title, or "Coin of the 17th of Leaffall". */
    fun title(c: Context, t: Treasure): String {
        if (t.title.isNotBlank()) return t.title
        val day = day(c, local(t.momentAt).toLocalDate())
        return c.getString(
            when (t.kindOf) {
                Kind.COIN -> R.string.title_coin
                Kind.GEM -> R.string.title_gem
                Kind.RELIC -> R.string.title_relic
                Kind.MIRROR -> R.string.title_mirror
            },
            day,
        )
    }

    /** "Struck on the 17th of Leaffall, 2026 · Prague · rain, +12° · at dusk". */
    fun legend(c: Context, t: Treasure): String {
        val z = local(t.momentAt)
        val verb = c.getString(
            when (t.kindOf) {
                Kind.COIN -> R.string.verb_coin
                Kind.GEM -> R.string.verb_gem
                Kind.RELIC -> R.string.verb_relic
                Kind.MIRROR -> R.string.verb_mirror
            },
            date(c, z.toLocalDate()),
        )
        val parts = mutableListOf(verb)
        if (t.place.isNotBlank()) parts += t.place
        val sky = t.skyOf
        if (sky != Sky.UNKNOWN) parts += sky(c, sky) + (t.tempC?.let { ", " + (if (it > 0) "+" else "") + it + "°" } ?: "")
        parts += daypart(c, Daypart.of(z.hour))
        return parts.joinToString(" · ")
    }

    /** What it's made of: "Gold", "Ruby, heart cut", "Silver crown". */
    fun matter(c: Context, t: Treasure): String = when (t.kindOf) {
        Kind.COIN -> metal(c, t.metalOf)
        Kind.GEM -> c.getString(R.string.matter_gem, stone(c, t.stoneOf), cut(c, t.cutOf).lowercase(c.resources.configuration.locales[0]))
        Kind.RELIC -> c.getString(R.string.matter_relic, relic(c, t.relicOf), metal(c, t.metalOf).lowercase(c.resources.configuration.locales[0]))
        Kind.MIRROR -> c.getString(R.string.matter_mirror, metal(c, t.metalOf).lowercase(c.resources.configuration.locales[0]), ((t.durationMs + 500) / 1000).toInt())
    }
}

/** The same, for composables. */
@Composable
fun ctx(): Context = LocalContext.current

package app.hoard.domain

import app.hoard.data.ForgeMode
import app.hoard.data.Settings
import app.hoard.data.Treasure
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The rules of the forge: how many moments a day, and when they come out. */
object Forge {
    /** When something put in at [takenAt] leaves the forge: the next cooling at [Settings.forgeAt], or at once. */
    fun revealAt(takenAt: Long, s: Settings, zone: ZoneId): Long {
        if (s.forge == ForgeMode.INSTANT) return takenAt
        val t = LocalDateTime.ofInstant(Instant.ofEpochMilli(takenAt), zone)
        val m = Math.floorMod(s.forgeAt, 24 * 60)
        var at = t.toLocalDate().atTime(m / 60, m % 60)
        if (!at.isAfter(t)) at = at.plusDays(1)
        return at.atZone(zone).toInstant().toEpochMilli()
    }

    fun day(ms: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun usedToday(all: List<Treasure>, now: Long, zone: ZoneId): Int {
        val today = day(now, zone)
        return all.count { day(it.takenAt, zone) == today }
    }

    /** Room left in today's forge; null when there is no limit. */
    fun left(all: List<Treasure>, s: Settings, now: Long, zone: ZoneId): Int? =
        if (s.dailyLimit <= 0) null else (s.dailyLimit - usedToday(all, now, zone)).coerceAtLeast(0)
}

/** The gnomes' errands: one every three days, the same for everyone. */
object Quests {
    const val PERIOD_DAYS = 3

    val ids = listOf(
        "silver", "laugh", "melt", "hands", "red", "round", "above", "old", "tiny", "steam",
        "mirror", "door", "shadow", "first_light", "night_lights", "stranger_beast", "page", "handmade", "new_path", "window",
        "handwriting", "wild_flower", "water", "golden", "music", "cloud_shape", "balance", "pair", "hidden", "gift",
    )

    class Active(val period: Long, val id: String, val endsDay: Long) {
        val key get() = "$period:$id"
    }

    fun active(epochDay: Long): Active {
        val period = Math.floorDiv(epochDay, PERIOD_DAYS.toLong())
        // A fixed shuffle so neighbouring periods don't ask for neighbouring things.
        val i = Math.floorMod(period * 17 + 5, ids.size.toLong()).toInt()
        return Active(period, ids[i], (period + 1) * PERIOD_DAYS - 1)
    }

    /** What the next errand pays, or null once everything is unlocked. */
    fun nextReward(s: Settings): Material? = Material.rewards.firstOrNull { it.key !in s.unlocked }

    fun metals(s: Settings): List<Metal> = Metal.entries.filter { it.starter || Material.M(it).key in s.unlocked }
    fun stones(s: Settings): List<Stone> = Stone.entries.filter { it.starter || Material.S(it).key in s.unlocked }
}

/** Things that appear in the cave as the hoard grows. */
enum class Feature {
    MUSHROOMS, STALACTITES, CRYSTALS, BANNERS, POOL, LANTERN, BATS, BONES, SKYLIGHT;

    fun reached(st: HoardStats): Boolean = progress(st) >= 1f

    /** 0..1 toward appearing. */
    fun progress(st: HoardStats): Float = when (this) {
        MUSHROOMS -> st.total / 5f
        STALACTITES -> st.total / 12f
        CRYSTALS -> st.byKind(Kind.GEM) / 3f
        BANNERS -> st.byKind(Kind.RELIC) / 3f
        POOL -> st.total / 25f
        LANTERN -> st.quests / 1f
        BATS -> st.nights / 5f
        BONES -> st.total / 50f
        SKYLIGHT -> st.total / 100f
    }.coerceIn(0f, 1f)

    /** The goal, for the list: "5 treasures", "3 gems". */
    val goal: Int
        get() = when (this) {
            MUSHROOMS -> 5; STALACTITES -> 12; CRYSTALS -> 3; BANNERS -> 3; POOL -> 25; LANTERN -> 1; BATS -> 5; BONES -> 50; SKYLIGHT -> 100
        }
}

/** Counts over the hoard, for the dragon, the cave and the chronicle. */
class HoardStats(all: List<Treasure>, now: Long, private val zone: ZoneId) {
    /** Everything out of the forge, sealed chests included (they lie in the cave). */
    val kept = all.filter { !it.inForge(now) }
    val visible = all.filter { it.visible(now) }
    val inForge = all.filter { it.inForge(now) }
    val sealed = all.filter { !it.inForge(now) && it.sealed(now) }
    val total = kept.size
    val quests = all.count { it.quest.isNotEmpty() }
    val nights = kept.count { Daypart.of(Instant.ofEpochMilli(it.momentAt).atZone(zone).hour) == Daypart.NIGHT }
    val stage = Stage.of(total)
    val days = kept.map { Forge.day(it.momentAt, zone) }.toSet().size
    val features = Feature.entries.filter { it.reached(this) }.toSet()

    fun byKind(k: Kind) = kept.count { it.kindOf == k }
    fun byHall(h: Hall) = kept.count { it.hallOf == h }

    /**
     * Not yet shown: fresh from the forge (a sealed one comes out as a closed chest), or a chest whose day has come.
     */
    val toUnveil = all.filter { !it.inForge(now) && (!it.seen || (it.sealedUntil != null && !it.sealed(now) && !it.opened)) }
        .sortedBy { it.takenAt }
}

/** The dragon stirs in its sleep and rolls an old treasure out of the pile. */
object Roll {
    fun pick(all: List<Treasure>, now: Long, zone: ZoneId, epochDay: Long): Treasure? {
        val today = LocalDate.ofEpochDay(epochDay)
        val old = all.filter { it.visible(now) && Forge.day(it.momentAt, zone).isBefore(today.minusDays(13)) }
        if (old.isEmpty()) return null
        val anniversary = old.filter { val d = Forge.day(it.momentAt, zone); d.month == today.month && d.dayOfMonth == today.dayOfMonth }
        if (anniversary.isNotEmpty()) return anniversary.maxBy { it.momentAt }
        val monthAgo = old.filter { Forge.day(it.momentAt, zone).dayOfMonth == today.dayOfMonth }
        if (monthAgo.isNotEmpty()) return monthAgo.maxBy { it.momentAt }
        val k = Math.floorMod(epochDay * 2654435761L, old.size.toLong()).toInt()
        return old.sortedBy { it.id }[k]
    }

    /** Whole years between the moment and today, when it is an anniversary; else 0. */
    fun years(t: Treasure, zone: ZoneId, today: LocalDate): Int {
        val d = Forge.day(t.momentAt, zone)
        return if (d.month == today.month && d.dayOfMonth == today.dayOfMonth) today.year - d.year else 0
    }
}

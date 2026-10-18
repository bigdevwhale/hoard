package app.hoard

import app.hoard.data.Backup
import app.hoard.data.ForgeMode
import app.hoard.data.Settings
import app.hoard.data.Treasure
import app.hoard.domain.Feature
import app.hoard.domain.Forge
import app.hoard.domain.HoardStats
import app.hoard.domain.Material
import app.hoard.domain.Quests
import app.hoard.domain.Roll
import app.hoard.domain.Sky
import app.hoard.domain.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HoardTest {
    private val zone: ZoneId = ZoneOffset.UTC

    private fun ms(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) = LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun t(id: Long, at: Long, reveal: Long = at, sealed: Long? = null, seen: Boolean = true, opened: Boolean = false) =
        Treasure(id = id, kind = "COIN", hall = "KIN", media = "$id.jpg", takenAt = at, momentAt = at, revealAt = reveal, seed = id.toInt(), sealedUntil = sealed, seen = seen, opened = opened)

    @Test
    fun forgeCoolsAtTheNextMorning() {
        val s = Settings(forgeAt = 7 * 60)
        assertEquals(ms(2026, 10, 18, 7), Forge.revealAt(ms(2026, 10, 17, 21), s, zone))
        // After midnight but before dawn: the same morning.
        assertEquals(ms(2026, 10, 18, 7), Forge.revealAt(ms(2026, 10, 18, 2), s, zone))
        assertEquals(ms(2026, 10, 19, 7), Forge.revealAt(ms(2026, 10, 18, 7), s, zone))
        assertEquals(ms(2026, 10, 17, 21), Forge.revealAt(ms(2026, 10, 17, 21), s.copy(forge = ForgeMode.INSTANT), zone))
    }

    @Test
    fun dailyLimitCountsWhatWentIntoTheForgeToday() {
        val now = ms(2026, 10, 17, 20)
        val all = listOf(t(1, ms(2026, 10, 17, 9)), t(2, ms(2026, 10, 17, 12)), t(3, ms(2026, 10, 16, 23)))
        assertEquals(1, Forge.left(all, Settings(dailyLimit = 3), now, zone))
        assertEquals(0, Forge.left(all, Settings(dailyLimit = 2), now, zone))
        assertNull(Forge.left(all, Settings(dailyLimit = 0), now, zone))
    }

    @Test
    fun questsChangeEveryThreeDays() {
        val a = Quests.active(LocalDate.of(2026, 10, 15).toEpochDay())
        val days = (0 until 3).map { Quests.active(a.period * 3 + it).id }.toSet()
        assertEquals(1, days.size)
        assertNotEquals(a.id, Quests.active(a.period * 3 + 3).id)
        assertEquals(a.period * 3 + 2, a.endsDay)
    }

    @Test
    fun gnomesPayInOrder() {
        assertEquals(Material.rewards[0], Quests.nextReward(Settings()))
        val all = Settings(unlocked = Material.rewards.map { it.key }.toSet())
        assertNull(Quests.nextReward(all))
        assertEquals(7, Quests.metals(all).size)
    }

    @Test
    fun forgeAndChestsHideTreasure() {
        val now = ms(2026, 10, 17, 12)
        val forging = t(1, ms(2026, 10, 17, 10), reveal = ms(2026, 10, 18, 7), seen = false)
        val fresh = t(2, ms(2026, 10, 16, 10), reveal = ms(2026, 10, 17, 7), seen = false)
        val chest = t(3, ms(2026, 10, 10, 10), reveal = ms(2026, 10, 11, 7), sealed = ms(2027, 10, 10, 0))
        val due = t(4, ms(2025, 10, 10, 10), reveal = ms(2025, 10, 11, 7), sealed = ms(2026, 10, 1, 0))
        val st = HoardStats(listOf(forging, fresh, chest, due), now, zone)
        assertEquals(listOf(1L), st.inForge.map { it.id })
        assertEquals(listOf(3L), st.sealed.map { it.id })
        assertEquals(setOf(2L, 4L), st.visible.map { it.id }.toSet())
        assertEquals(listOf(4L, 2L), st.toUnveil.map { it.id })
        assertEquals(3, st.total)
    }

    @Test
    fun dragonGrowsWithTheHoard() {
        assertEquals(Stage.HATCHLING, Stage.of(0))
        assertEquals(Stage.YOUNG, Stage.of(8))
        assertEquals(Stage.ELDER, Stage.of(500))
        val now = ms(2026, 10, 17, 12)
        val st = HoardStats((1..12L).map { t(it, ms(2026, 9, 1, 12) + it) }, now, zone)
        assertTrue(Feature.STALACTITES in st.features)
        assertTrue(Feature.POOL !in st.features)
    }

    @Test
    fun theDragonRemembersAnniversaries() {
        val now = ms(2026, 10, 17, 12)
        val year = t(1, ms(2025, 10, 17, 9))
        val month = t(2, ms(2026, 9, 17, 9))
        val other = t(3, ms(2026, 8, 3, 9))
        val today = LocalDate.of(2026, 10, 17)
        assertEquals(1L, Roll.pick(listOf(other, month, year), now, zone, today.toEpochDay())?.id)
        assertEquals(2L, Roll.pick(listOf(other, month), now, zone, today.toEpochDay())?.id)
        assertEquals(1, Roll.years(year, zone, today))
        assertNull(Roll.pick(listOf(t(4, ms(2026, 10, 10, 9))), now, zone, today.toEpochDay()))
    }

    @Test
    fun remindersSpreadOverTheWindow() {
        val s = Settings(remindCount = 3, remindFrom = 12 * 60, remindTo = 20 * 60)
        assertEquals(listOf(720, 960, 1200), app.hoard.domain.Reminders.times(s))
        assertEquals(listOf(1200), app.hoard.domain.Reminders.times(s.copy(remindCount = 1)))
        assertTrue(app.hoard.domain.Reminders.times(s.copy(remindCount = 0)).isEmpty())
        assertEquals(LocalDateTime.of(2026, 10, 18, 16, 0), app.hoard.domain.Reminders.next(s, LocalDateTime.of(2026, 10, 18, 13, 5)))
        assertEquals(LocalDateTime.of(2026, 10, 19, 12, 0), app.hoard.domain.Reminders.next(s, LocalDateTime.of(2026, 10, 18, 20, 0)))
        // Only while today's forge has room.
        val now = ms(2026, 10, 18, 15)
        val two = listOf(t(1, ms(2026, 10, 18, 9)), t(2, ms(2026, 10, 18, 10)))
        assertTrue(app.hoard.domain.Reminders.due(two, s, now, zone))
        assertTrue(!app.hoard.domain.Reminders.due(two, s.copy(dailyLimit = 2), now, zone))
        assertTrue(!app.hoard.domain.Reminders.due(two, s.copy(dailyLimit = 0), now, zone))
    }

    @Test
    fun skyFromWeatherCodes() {
        assertEquals(Sky.CLEAR, Sky.ofWmo(0))
        assertEquals(Sky.RAIN, Sky.ofWmo(61))
        assertEquals(Sky.SNOW, Sky.ofWmo(73))
        assertEquals(Sky.STORM, Sky.ofWmo(95))
    }

    @Test
    fun backupRoundTrip() {
        val list = listOf(t(1, ms(2026, 10, 1, 9)).copy(title = "Bruno", note = "«Привет»", lat = 50.1, tempC = -3, quest = "9:red", favorite = true), t(2, ms(2026, 10, 2, 9), sealed = ms(2027, 1, 1, 0)))
        assertEquals(list, Backup.decode(Backup.encode(list)))
    }
}

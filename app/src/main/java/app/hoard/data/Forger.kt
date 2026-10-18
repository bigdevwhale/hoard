package app.hoard.data

import app.hoard.AppContainer
import app.hoard.domain.Cut
import app.hoard.domain.Forge
import app.hoard.domain.Hall
import app.hoard.domain.Kind
import app.hoard.domain.Material
import app.hoard.domain.Metal
import app.hoard.domain.Quests
import app.hoard.domain.RelicForm
import app.hoard.domain.Stone
import java.io.File
import java.time.ZoneId
import kotlin.random.Random

/** What the keeper chose on the forge screen. */
data class Order(
    val kind: Kind,
    val hall: Hall,
    val metal: Metal = Metal.GOLD,
    val stone: Stone = Stone.RUBY,
    val cut: Cut = Cut.ROUND,
    val relic: RelicForm = RelicForm.CROWN,
    val title: String = "",
    val note: String = "",
    val place: String = "",
    val runes: Runes = Runes(),
    /** The gnome's errand this answers, or null. */
    val quest: Quests.Active? = null,
    val sealedUntil: Long? = null,
    /** When the moment happened, if not now (a photo from the gallery). */
    val momentAt: Long? = null,
)

/** Puts a moment into the forge: keeps the file, writes the treasure, pays the gnome's reward. */
object Forger {
    /** Returns the new treasure and the material the gnomes paid, if any. */
    suspend fun forge(c: AppContainer, file: File, video: Boolean, o: Order, now: Long = System.currentTimeMillis()): Pair<Treasure, Material?> {
        val s = c.settings.now()
        val zone = ZoneId.systemDefault()
        val duration = if (video) c.vault.durationMs(file) else 0L
        // The full-size original goes to the gallery before the vault keeps its own, smaller copy.
        if (s.toGallery && !c.vault.fromGallery(file)) c.vault.toGallery(file, video, now)
        val media = c.vault.keep(file, video)
        val t = Treasure(
            kind = (if (video) Kind.MIRROR else o.kind).name,
            title = o.title.trim(),
            note = o.note.trim(),
            hall = o.hall.name,
            metal = o.metal.name,
            stone = o.stone.name,
            cut = o.cut.name,
            relic = o.relic.name,
            media = media,
            video = video,
            durationMs = duration,
            takenAt = now,
            momentAt = o.momentAt ?: now,
            revealAt = Forge.revealAt(now, s, zone),
            place = o.place.trim().ifEmpty { o.runes.place },
            lat = o.runes.lat,
            lon = o.runes.lon,
            sky = o.runes.sky.name,
            tempC = o.runes.tempC,
            quest = o.quest?.key.orEmpty(),
            sealedUntil = o.sealedUntil,
            seed = Random.nextInt(),
        )
        val id = c.db.dao().insert(t)
        var reward: Material? = null
        if (o.quest != null && o.quest.key !in s.claimed) {
            reward = Quests.nextReward(s)
            c.settings.update { it.copy(claimed = it.claimed + o.quest.key, unlocked = it.unlocked + listOfNotNull(reward?.key)) }
        }
        c.changed()
        app.hoard.notify.Alarms.clearReminder(c.context)
        return t.copy(id = id) to reward
    }

    suspend fun remove(c: AppContainer, t: Treasure) {
        c.db.dao().delete(t)
        c.vault.delete(t.media)
        c.changed()
    }
}

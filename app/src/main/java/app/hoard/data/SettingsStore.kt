package app.hoard.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.hoard.domain.Hue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

/** The forge cools overnight, or at once for the impatient. */
enum class ForgeMode { NIGHT, INSTANT }

data class Settings(
    val onboarded: Boolean = false,
    val dragonName: String = "",
    val hue: Hue = Hue.EMERALD,
    /** Treasures a day; 0 for no limit. */
    val dailyLimit: Int = 3,
    val forge: ForgeMode = ForgeMode.NIGHT,
    /** When the forge cools, minutes after midnight. */
    val forgeAt: Int = 7 * 60,
    /** Mark treasures with the place and the weather. */
    val runes: Boolean = true,
    /** A morning notice when the forge has cooled. */
    val notify: Boolean = true,
    /** Epoch day the dragon last rolled out an old treasure, and which one. */
    val rolledDay: Long = -1,
    val rolledId: Long = 0,
    /** Gnome errands done, "period:id". */
    val claimed: Set<String> = emptySet(),
    /** Materials paid by the gnomes, [app.hoard.domain.Material.key]. */
    val unlocked: Set<String> = emptySet(),
    /** Cave features and dragon stages already celebrated. */
    val seen: Set<String> = emptySet(),
    /** Also keep a copy of what the camera catches in the phone's gallery. */
    val toGallery: Boolean = true,
    /** The halls show the photos themselves rather than the treasure art. */
    val showPhotos: Boolean = false,
    /** Newest first in the halls. */
    val newestFirst: Boolean = true,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        dragonName = this[NAME] ?: "",
        hue = this[HUE]?.let { runCatching { Hue.valueOf(it) }.getOrNull() } ?: Hue.EMERALD,
        dailyLimit = this[LIMIT] ?: 3,
        forge = this[FORGE]?.let { runCatching { ForgeMode.valueOf(it) }.getOrNull() } ?: ForgeMode.NIGHT,
        forgeAt = this[FORGE_AT] ?: 7 * 60,
        runes = this[RUNES] ?: true,
        notify = this[NOTIFY] ?: true,
        rolledDay = this[ROLLED_DAY] ?: -1L,
        rolledId = this[ROLLED_ID] ?: 0L,
        claimed = this[CLAIMED] ?: emptySet(),
        unlocked = this[UNLOCKED] ?: emptySet(),
        seen = this[SEEN] ?: emptySet(),
        toGallery = this[TO_GALLERY] ?: true,
        showPhotos = this[SHOW_PHOTOS] ?: false,
        newestFirst = this[NEWEST_FIRST] ?: true,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.dragonName
            it[HUE] = s.hue.name
            it[LIMIT] = s.dailyLimit
            it[FORGE] = s.forge.name
            it[FORGE_AT] = s.forgeAt
            it[RUNES] = s.runes
            it[NOTIFY] = s.notify
            it[ROLLED_DAY] = s.rolledDay
            it[ROLLED_ID] = s.rolledId
            it[CLAIMED] = s.claimed
            it[UNLOCKED] = s.unlocked
            it[SEEN] = s.seen
            it[TO_GALLERY] = s.toGallery
            it[SHOW_PHOTOS] = s.showPhotos
            it[NEWEST_FIRST] = s.newestFirst
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("dragon_name")
        val HUE = stringPreferencesKey("hue")
        val LIMIT = intPreferencesKey("daily_limit")
        val FORGE = stringPreferencesKey("forge")
        val FORGE_AT = intPreferencesKey("forge_at")
        val RUNES = booleanPreferencesKey("runes")
        val NOTIFY = booleanPreferencesKey("notify")
        val ROLLED_DAY = longPreferencesKey("rolled_day")
        val ROLLED_ID = longPreferencesKey("rolled_id")
        val CLAIMED = stringSetPreferencesKey("claimed")
        val UNLOCKED = stringSetPreferencesKey("unlocked")
        val SEEN = stringSetPreferencesKey("seen")
        val TO_GALLERY = booleanPreferencesKey("to_gallery")
        val SHOW_PHOTOS = booleanPreferencesKey("show_photos")
        val NEWEST_FIRST = booleanPreferencesKey("newest_first")
    }
}

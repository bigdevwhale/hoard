package app.hoard.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import app.hoard.domain.Cut
import app.hoard.domain.Hall
import app.hoard.domain.Kind
import app.hoard.domain.Metal
import app.hoard.domain.RelicForm
import app.hoard.domain.Sky
import app.hoard.domain.Stone
import kotlinx.coroutines.flow.Flow

/**
 * One moment in the hoard: a photo or a clip, and what it was struck into.
 * [takenAt] is when it went into the forge (it counts toward that day's limit); [momentAt] is when the
 * moment itself happened (the same, or a photo's own date when it came from the gallery).
 */
@Entity(tableName = "treasures")
data class Treasure(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [Kind] name. */
    val kind: String,
    val title: String = "",
    val note: String = "",
    /** [Hall] name. */
    val hall: String,
    /** [Metal] name, for coins and relics. */
    val metal: String = Metal.GOLD.name,
    /** [Stone] and [Cut] names, for gems. */
    val stone: String = Stone.RUBY.name,
    val cut: String = Cut.ROUND.name,
    /** [RelicForm] name, for relics. */
    val relic: String = RelicForm.CROWN.name,
    /** File name in the vault (a .jpg or an .mp4). */
    val media: String,
    val video: Boolean = false,
    val durationMs: Long = 0,
    val takenAt: Long,
    val momentAt: Long,
    /** When it leaves the forge. */
    val revealAt: Long,
    /** The unveiling was shown. */
    val seen: Boolean = false,
    val place: String = "",
    val lat: Double? = null,
    val lon: Double? = null,
    /** [Sky] name. */
    val sky: String = Sky.UNKNOWN.name,
    val tempC: Int? = null,
    /** The gnome's errand this treasure answered, "period:id"; empty for none. */
    val quest: String = "",
    /** Locked in a chest until then. */
    val sealedUntil: Long? = null,
    /** The chest's opening was shown. */
    val opened: Boolean = false,
    /** Fixes where it lies on the pile and the small differences of its art. */
    val seed: Int,
    val favorite: Boolean = false,
) {
    val kindOf: Kind get() = runCatching { Kind.valueOf(kind) }.getOrDefault(Kind.COIN)
    val hallOf: Hall get() = runCatching { Hall.valueOf(hall) }.getOrDefault(Hall.WONDERS)
    val metalOf: Metal get() = runCatching { Metal.valueOf(metal) }.getOrDefault(Metal.GOLD)
    val stoneOf: Stone get() = runCatching { Stone.valueOf(stone) }.getOrDefault(Stone.RUBY)
    val cutOf: Cut get() = runCatching { Cut.valueOf(cut) }.getOrDefault(Cut.ROUND)
    val relicOf: RelicForm get() = runCatching { RelicForm.valueOf(relic) }.getOrDefault(RelicForm.CROWN)
    val skyOf: Sky get() = runCatching { Sky.valueOf(sky) }.getOrDefault(Sky.UNKNOWN)

    fun inForge(now: Long) = now < revealAt
    fun sealed(now: Long) = sealedUntil != null && now < sealedUntil

    /** Out of the forge and not locked away: it can be looked at. */
    fun visible(now: Long) = !inForge(now) && !sealed(now)
}

@Dao
abstract class HoardDao {
    @Query("SELECT * FROM treasures ORDER BY momentAt DESC")
    abstract fun observe(): Flow<List<Treasure>>

    @Query("SELECT * FROM treasures ORDER BY momentAt DESC")
    abstract suspend fun all(): List<Treasure>

    @Query("SELECT * FROM treasures WHERE id = :id")
    abstract suspend fun get(id: Long): Treasure?

    @Insert abstract suspend fun insert(t: Treasure): Long
    @Update abstract suspend fun update(t: Treasure)
    @Delete abstract suspend fun delete(t: Treasure)

    @Query("UPDATE treasures SET seen = 1 WHERE id IN (:ids)")
    abstract suspend fun markSeen(ids: List<Long>)

    @Query("UPDATE treasures SET opened = 1 WHERE id IN (:ids)")
    abstract suspend fun markOpened(ids: List<Long>)

    @Query("DELETE FROM treasures") protected abstract suspend fun clear()
    @Insert protected abstract suspend fun insertAll(l: List<Treasure>)

    @Transaction
    open suspend fun replaceAll(l: List<Treasure>) {
        clear()
        insertAll(l)
    }
}

@Database(entities = [Treasure::class], version = 1, exportSchema = true)
abstract class HoardDatabase : RoomDatabase() {
    abstract fun dao(): HoardDao

    companion object {
        fun create(context: Context): HoardDatabase =
            Room.databaseBuilder(context.applicationContext, HoardDatabase::class.java, "hoard.db").build()
    }
}

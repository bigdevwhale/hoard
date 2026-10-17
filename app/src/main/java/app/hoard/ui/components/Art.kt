package app.hoard.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.hoard.container
import app.hoard.data.Treasure
import app.hoard.domain.Daypart
import app.hoard.domain.Kind
import app.hoard.domain.Sky
import app.hoard.ui.pix.Glyphs
import app.hoard.ui.pix.Mint
import app.hoard.ui.pix.Px
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import kotlin.math.sin
import kotlin.random.Random

/** Turns treasures into pixel art. */
object Art {
    /** A treasure at about [size] pixels across: coin, gem, relic or mirror. */
    fun of(t: Treasure, still: Bitmap?, size: Int, f: Int = 0, tiltX: Float = 0f, tiltY: Float = 0f): Px = when (t.kindOf) {
        Kind.COIN -> Mint.coin(still, t.metalOf, size, f)
        Kind.GEM -> Mint.gem(still, t.stoneOf, t.cutOf, size, tiltX, tiltY, f)
        Kind.RELIC -> Mint.relic(still, t.relicOf, t.metalOf, (size * 1.15f).toInt(), f)
        Kind.MIRROR -> Mint.mirror(still, t.metalOf, (size * 0.78f).toInt(), size, f)
    }

    /** The small version that lies on the pile in the cave. */
    fun mini(t: Treasure, still: Bitmap?): Px = when (t.kindOf) {
        Kind.COIN -> Mint.coin(still, t.metalOf, 17)
        Kind.GEM -> Mint.gem(still, t.stoneOf, t.cutOf, 14)
        Kind.RELIC -> Mint.relic(still, t.relicOf, t.metalOf, 26)
        Kind.MIRROR -> Mint.mirror(still, t.metalOf, 14, 18)
    }

    fun back(t: Treasure, size: Int, zone: ZoneId = ZoneId.systemDefault()): Px =
        Mint.coinBack(t.metalOf, t.hallOf, runes(t, zone), Instant.ofEpochMilli(t.momentAt).atZone(zone).year, size)

    /** The marks of the day: its sky, its time, a place, a gnome's errand. */
    fun runes(t: Treasure, zone: ZoneId = ZoneId.systemDefault()): List<Array<String>> = buildList {
        val sky = t.skyOf
        if (sky != Sky.UNKNOWN) add(Glyphs.sky.getValue(sky))
        add(Glyphs.daypart.getValue(Daypart.of(Instant.ofEpochMilli(t.momentAt).atZone(zone).hour)))
        if (t.place.isNotBlank()) add(Glyphs.place)
        if (t.quest.isNotEmpty()) add(Glyphs.quest)
    }

    fun chest(size: Int, f: Int = 0, open: Boolean = false): Px = Mint.chest(app.hoard.domain.Metal.GOLD, size, (size * 0.8f).toInt() + 3, f, open)
}

/** Pixel art shown crisp at any size. */
@Composable
fun PixImage(px: Px?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val bmp: ImageBitmap? = remember(px) { px?.bitmap() }
    if (bmp != null) Image(bmp, null, modifier, contentScale = contentScale, filterQuality = FilterQuality.None)
}

@Composable
fun rememberStill(media: String?): Bitmap? {
    val vault = LocalContext.current.container.vault
    val b by produceState<Bitmap?>(null, media) { value = media?.let { withContext(Dispatchers.IO) { vault.still(it) } } }
    return b
}

/** A frame counter for idle animation, [fps] frames a second over [frames]. */
@Composable
fun rememberFrame(frames: Int, fps: Int = 6, running: Boolean = true): Int {
    var f by remember { mutableIntStateOf(0) }
    LaunchedEffect(running, frames, fps) {
        while (running) {
            delay(1000L / fps)
            f = (f + 1) % frames
        }
    }
    return f
}

/** A treasure's art, painted off the main thread. */
@Composable
fun TreasureArt(t: Treasure, modifier: Modifier = Modifier, size: Int = 64, f: Int = 0) {
    val still = rememberStill(t.media)
    val px by produceState<Px?>(null, t.id, t.kind, t.metal, t.stone, t.cut, t.relic, still, size, f) {
        value = withContext(Dispatchers.Default) { Art.of(t, still, size, f) }
    }
    PixImage(px, modifier)
}

/** Embers flying up once, square like the pixels around them. */
@Composable
fun Sparks(modifier: Modifier = Modifier, key: Any = Unit, color: Color = Color(0xFFFFD27A)) {
    val pieces = remember(key) {
        val rnd = Random(key.hashCode())
        List(60) { Ember(rnd.nextFloat(), 0.75f + rnd.nextFloat() * 0.35f, 0.25f + rnd.nextFloat() * 0.5f, rnd.nextFloat() * 6.28f, 2f + rnd.nextFloat() * 3f) }
    }
    var elapsed by remember(key) { mutableLongStateOf(0L) }
    LaunchedEffect(key) {
        val start = withFrameMillis { it }
        while (elapsed < 3200) elapsed = withFrameMillis { it } - start
    }
    if (elapsed >= 3200) return
    Canvas(modifier) {
        val t = elapsed / 1000f
        pieces.forEach { p ->
            val life = (t / (1.6f + p.speed * 2)).coerceIn(0f, 1f)
            if (life >= 1f) return@forEach
            val y = (p.y - p.speed * t * 0.55f) * size.height
            val x = (p.x + 0.03f * sin(t * 3 + p.phase)) * size.width
            val c = lerp(color, Color(0xFFFF7A3C), life)
            val s = p.r * (1f - life * 0.5f) * density
            drawRect(c.copy(alpha = 1f - life), Offset(x - s / 2, y - s / 2), Size(s, s))
        }
    }
}

private class Ember(val x: Float, val y: Float, val speed: Float, val phase: Float, val r: Float)

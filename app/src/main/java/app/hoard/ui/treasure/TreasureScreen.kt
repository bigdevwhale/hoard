package app.hoard.ui.treasure

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import app.hoard.R
import app.hoard.container
import app.hoard.data.Forger
import app.hoard.data.Treasure
import app.hoard.domain.Hall
import app.hoard.domain.Kind
import app.hoard.ui.Go
import app.hoard.ui.HoardData
import app.hoard.ui.T
import app.hoard.ui.components.Art
import app.hoard.ui.components.ConfirmDialog
import app.hoard.ui.components.PixImage
import app.hoard.ui.components.TopBar
import app.hoard.ui.components.UCard
import app.hoard.ui.components.UTextField
import app.hoard.ui.components.rememberFrame
import app.hoard.ui.components.rememberStill
import app.hoard.ui.forge.HallChip
import app.hoard.ui.pix.Glyphs
import app.hoard.ui.pix.Mint
import app.hoard.ui.pix.Px
import app.hoard.ui.pix.rgb
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

@Composable
fun TreasureScreen(id: Long, data: HoardData, go: Go) {
    val context = LocalContext.current
    val c = context.container
    val t = data.get(id)
    if (t == null) {
        LaunchedEffect(Unit) { go.back() }
        return
    }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar("", onBack = { go.back() }) {
            if (t.visible(data.now)) {
                IconButton(onClick = { c.write { db.dao().update(t.copy(favorite = !t.favorite)) } }) {
                    Icon(if (t.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, stringResource(R.string.treasure_favorite), tint = if (t.favorite) U.c.gold else U.c.muted)
                }
                IconButton(onClick = { share(context, t) }) { Icon(Icons.Rounded.Share, stringResource(R.string.treasure_share), tint = U.c.muted) }
                IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, stringResource(R.string.treasure_edit), tint = U.c.muted) }
            }
            IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.treasure_delete), tint = U.c.muted) }
        }
        when {
            t.inForge(data.now) -> Forging(t, stringResource(R.string.treasure_in_forge), stringResource(R.string.forge_cools_at, Instant.ofEpochMilli(t.revealAt).atZone(data.zone).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))))
            t.sealed(data.now) -> Locked(stringResource(R.string.treasure_sealed), stringResource(R.string.chest_opens, Instant.ofEpochMilli(t.sealedUntil!!).atZone(data.zone).toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))))
            else -> Details(t, data) { viewing = true }
        }
    }

    if (viewing && !t.video) FullPhoto(t) { viewing = false }
    if (editing) EditDialog(t, onDismiss = { editing = false }) { changed -> c.write { db.dao().update(changed) } }
    if (deleting) ConfirmDialog(
        stringResource(R.string.treasure_delete_title), stringResource(R.string.treasure_delete_text), stringResource(R.string.treasure_delete),
        onConfirm = { c.scope.launch { Forger.remove(c, t) }; go.back() }, onDismiss = { deleting = false }, danger = true,
    )
}

@Composable
private fun Locked(title: String, text: String) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val f = rememberFrame(8, 4)
        PixImage(remember(f) { Art.chest(60, f) }, Modifier.size(200.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = UType.Title.copy(textAlign = TextAlign.Center))
        Spacer(Modifier.height(8.dp))
        Text(text, style = UType.Sub.copy(textAlign = TextAlign.Center))
    }
}

/** Still in the forge: the moment itself can be seen, the treasure comes out in the morning. */
@Composable
private fun Forging(t: Treasure, title: String, text: String) {
    val still = rememberStill(t.media)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        still?.let {
            Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(it.width / it.height.toFloat().coerceAtLeast(1f)).clip(U.RLg), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = UType.Title.copy(textAlign = TextAlign.Center))
        Spacer(Modifier.height(6.dp))
        Text(text, style = UType.Sub.copy(textAlign = TextAlign.Center))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Details(t: Treasure, data: HoardData, onPhoto: () -> Unit) {
    val context = LocalContext.current
    val still = rememberStill(t.media)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            when (t.kindOf) {
                Kind.COIN -> FlipCoin(t, still)
                Kind.GEM -> TiltGem(t, still)
                Kind.MIRROR -> MirrorClip(t)
                Kind.RELIC -> {
                    val f = rememberFrame(8, 4)
                    val px by produceState<Px?>(null, still, f) { value = withContext(Dispatchers.Default) { Art.of(t, still, 72, f) } }
                    PixImage(px, Modifier.size(280.dp))
                }
            }
        }
        Text(T.title(context, t), style = UType.Display.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(T.matter(context, t), style = UType.Small.copy(color = U.c.gold, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text(T.legend(context, t), style = UType.Legend.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            HallChip(t.hallOf, true) {}
            Art.runes(t, data.zone).forEach { pat ->
                Box(Modifier.size(38.dp).clip(U.RSm).background(U.c.runeSoft), contentAlignment = Alignment.Center) {
                    PixImage(remember(pat) { Glyphs.icon(pat, rgb(127, 227, 240)) }, Modifier.size(20.dp))
                }
            }
        }
        if (t.quest.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.treasure_quest, T.quest(context, t.quest.substringAfter(':'))), style = UType.Muted.copy(color = U.c.rune, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        }
        if (t.note.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            UCard { Text(t.note, style = UType.Body) }
        }
        if (!t.video) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.treasure_moment).uppercase(), style = UType.Eyebrow, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            val full by produceState<Bitmap?>(null, t.media) { value = withContext(Dispatchers.IO) { context.container.vault.full(t.media, 1440) } }
            val b = full ?: still
            if (b != null) {
                Image(
                    b.asImageBitmap(), null,
                    Modifier.fillMaxWidth().aspectRatio(b.width / b.height.toFloat().coerceAtLeast(1f)).clip(U.RLg).clickable(onClick = onPhoto),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        if (t.momentAt != t.takenAt) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.treasure_forged_on, T.date(context, T.local(t.takenAt, data.zone).toLocalDate())), style = UType.Small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(40.dp))
    }
}

/** A coin that turns over when tapped: the moment on one side, the hall, runes and year on the other. */
@Composable
private fun FlipCoin(t: Treasure, still: Bitmap?) {
    val scope = rememberCoroutineScope()
    val rot = remember { Animatable(0f) }
    val front by produceState<Px?>(null, still, t.metal) { value = withContext(Dispatchers.Default) { Art.of(t, still, 72) } }
    val back = remember(t.metal, t.hall) { Art.back(t, 72) }
    Box(
        Modifier.size(260.dp).clickable {
            scope.launch { rot.animateTo(if (rot.value < 90f) 180f else 0f, tween(600)) }
        },
        contentAlignment = Alignment.Center,
    ) {
        val r = rot.value
        val density = LocalDensity.current.density
        Box(Modifier.fillMaxSize().graphicsLayer { rotationY = if (r <= 90f) r else r - 180f; cameraDistance = 14f * density }) {
            PixImage(if (r <= 90f) front else back, Modifier.fillMaxSize())
        }
    }
}

/** A gem that catches the light as the phone tilts. */
@Composable
private fun TiltGem(t: Treasure, still: Bitmap?) {
    val context = LocalContext.current
    var tx by remember { mutableFloatStateOf(0f) }
    var ty by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // Quantised, so the gem is repainted only when the light really moves.
                val nx = (-(e.values[0] / 6f)).coerceIn(-1f, 1f)
                val ny = ((e.values[1] - 6f) / 6f).coerceIn(-1f, 1f)
                val qx = (nx * 6).toInt() / 6f
                val qy = (ny * 6).toInt() / 6f
                if (abs(qx - tx) > 0.01f || abs(qy - ty) > 0.01f) { tx = qx; ty = qy }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    val f = rememberFrame(4, 3)
    val px by produceState<Px?>(null, still, tx, ty, f) { value = withContext(Dispatchers.Default) { Art.of(t, still, 72, f, tx, ty) } }
    PixImage(px, Modifier.size(260.dp))
}

/** The clip plays inside the mirror's glass. */
@Composable
private fun MirrorClip(t: Treasure) {
    val context = LocalContext.current
    val file = remember(t.media) { context.container.vault.file(t.media) }
    var muted by remember { mutableStateOf(true) }
    val f = rememberFrame(8, 5)
    val w = 56; val h = 72
    val frame = remember(t.metal, f) { Mint.mirror(null, t.metalOf, w, h, f, empty = true) }
    val win = remember { Mint.mirrorWindow(w, h) }
    BoxWithConstraints(Modifier.height(290.dp).aspectRatio(w / h.toFloat())) {
        PixImage(frame, Modifier.fillMaxSize())
        val cx = maxWidth * win[0]; val cy = maxHeight * win[1]
        val rx = maxWidth * win[2]; val ry = maxHeight * win[3]
        val oval = remember { GenericShape { size, _ -> addOval(Rect(0f, 0f, size.width, size.height)) } }
        if (file.exists()) ClipPlayer(
            file, muted,
            Modifier.offset(cx - rx, cy - ry).size(rx * 2, ry * 2).clip(oval).clickable { muted = !muted },
        )
        Box(Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape).background(Color(0x99000000)).clickable { muted = !muted }, contentAlignment = Alignment.Center) {
            Icon(if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FullPhoto(t: Treasure, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val b by produceState<Bitmap?>(null, t.media) { value = withContext(Dispatchers.IO) { context.container.vault.full(t.media) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            b?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditDialog(t: Treasure, onDismiss: () -> Unit, onSave: (Treasure) -> Unit) {
    var title by remember { mutableStateOf(t.title) }
    var note by remember { mutableStateOf(t.note) }
    var place by remember { mutableStateOf(t.place) }
    var hall by remember { mutableStateOf(t.hallOf) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        title = { Text(stringResource(R.string.treasure_edit), style = UType.Strong) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                UTextField(title, { title = it.take(60) }, placeholder = stringResource(R.string.mint_name_hint))
                Spacer(Modifier.height(8.dp))
                UTextField(note, { note = it.take(600) }, placeholder = stringResource(R.string.mint_note_hint), minLines = 3)
                Spacer(Modifier.height(8.dp))
                UTextField(place, { place = it.take(60) }, placeholder = stringResource(R.string.mint_place_hint))
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Hall.entries.forEach { h -> HallChip(h, hall == h) { hall = h } }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(t.copy(title = title.trim(), note = note.trim(), place = place.trim(), hall = hall.name)); onDismiss() }) {
                Text(stringResource(R.string.save), color = U.c.gold, style = UType.Button)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}

private fun share(context: Context, t: Treasure) {
    val f = context.container.vault.file(t.media)
    if (!f.exists()) return
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
    val send = Intent(Intent.ACTION_SEND)
        .setType(if (t.video) "video/mp4" else "image/jpeg")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, T.title(context, t) + "\n" + T.legend(context, t))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null))
}

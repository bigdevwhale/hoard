package app.hoard.ui.forge

import app.hoard.ui.theme.engraved
import app.hoard.ui.tips.HelpButton
import app.hoard.ui.tips.Tip
import app.hoard.ui.tips.TipHost
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hoard.R
import app.hoard.container
import app.hoard.data.Treasure
import app.hoard.ui.Go
import app.hoard.ui.HoardData
import app.hoard.ui.T
import app.hoard.ui.components.Art
import app.hoard.ui.components.PixImage
import app.hoard.ui.components.PrimaryButton
import app.hoard.ui.components.SoftButton
import app.hoard.ui.components.Sparks
import app.hoard.ui.components.rememberFrame
import app.hoard.ui.components.rememberStill
import app.hoard.ui.pix.Px
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** What one step of the unveiling shows. */
private enum class Show { TREASURE, SEALED, OPENED }

/**
 * The forge has cooled: the dragon breathes on it and the night's treasures come out one by one. A
 * sealed one comes out as a locked chest; a chest whose day has come opens.
 */
@Composable
fun UnveilScreen(data: HoardData, go: Go) {
    val context = LocalContext.current
    val c = context.container
    val queue = remember { data.stats.toUnveil }
    var i by rememberSaveable { mutableIntStateOf(0) }
    if (queue.isEmpty()) {
        LaunchedEffect(Unit) { go.back() }
        return
    }
    val t = queue[i.coerceIn(0, queue.size - 1)]
    val now = data.now
    val show = when {
        t.sealed(now) -> Show.SEALED
        t.sealedUntil != null && !t.opened -> Show.OPENED
        else -> Show.TREASURE
    }
    // Shown is seen: mark it as soon as it appears, so leaving halfway doesn't show it again.
    LaunchedEffect(t.id) {
        c.scope.launch {
            c.db.dao().markSeen(listOf(t.id))
            if (show == Show.OPENED) c.db.dao().markOpened(listOf(t.id))
        }
    }

    TipHost(Tip.UNVEIL)
    val appear = remember(t.id) { Animatable(0f) }
    LaunchedEffect(t.id) { appear.snapTo(0f); appear.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val f = rememberFrame(8, 5)

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF4A2414), U.c.bg), radius = 1400f),
        ),
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(
                    when (show) {
                        Show.TREASURE -> R.string.unveil_eyebrow
                        Show.SEALED -> R.string.unveil_sealed_eyebrow
                        Show.OPENED -> R.string.unveil_opened_eyebrow
                    }
                ).uppercase(),
                style = UType.Eyebrow.copy(color = U.c.ember),
                modifier = Modifier.padding(top = 12.dp),
            )
            if (queue.size > 1) Text("${i + 1} / ${queue.size}", style = UType.Small, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.weight(0.6f))
            Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
                // The dragon's breath sweeps over first, then the treasure cools into view.
                Breath(f, Modifier.fillMaxSize().alpha((1f - appear.value * 1.6f).coerceIn(0f, 1f)))
                when (show) {
                    Show.SEALED -> PixImage(remember(f) { Art.chest(60, f) }, Modifier.size(200.dp).scale(0.6f + 0.4f * appear.value).alpha(appear.value))
                    else -> Unveiled(t, f, Modifier.size(230.dp).scale(0.6f + 0.4f * appear.value).alpha(appear.value))
                }
                if (appear.value > 0.5f) Sparks(Modifier.matchParentSize(), key = t.id)
            }
            Spacer(Modifier.height(18.dp))
            when (show) {
                Show.SEALED -> {
                    Text(stringResource(R.string.unveil_sealed_title), style = UType.Title.copy(textAlign = TextAlign.Center))
                    Spacer(Modifier.height(8.dp))
                    val day = Instant.ofEpochMilli(t.sealedUntil!!).atZone(data.zone).toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                    Text(stringResource(R.string.unveil_sealed_text, day), style = UType.Sub.copy(textAlign = TextAlign.Center))
                }
                else -> {
                    Text(engraved(T.title(context, t)), style = UType.Title.copy(textAlign = TextAlign.Center))
                    Spacer(Modifier.height(4.dp))
                    Text(T.matter(context, t) + " · " + T.hall(context, t.hallOf), style = UType.Small.copy(color = U.c.gold))
                    Spacer(Modifier.height(10.dp))
                    Text(T.legend(context, t), style = UType.Legend.copy(textAlign = TextAlign.Center))
                    if (show == Show.OPENED) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.unveil_opened_text, T.date(context, T.local(t.takenAt, data.zone).toLocalDate())), style = UType.Muted.copy(textAlign = TextAlign.Center))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            val last = i >= queue.size - 1
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (show != Show.SEALED) SoftButton(stringResource(R.string.unveil_look), { go.back(); go.treasure(t.id) }, Modifier.weight(1f))
                PrimaryButton(
                    stringResource(if (last) R.string.unveil_done else R.string.unveil_next),
                    { if (last) go.back() else i++ },
                    Modifier.weight(1f), color = U.c.gold,
                )
            }
        }
    }
}

@Composable
private fun Unveiled(t: Treasure, f: Int, modifier: Modifier) {
    val still = rememberStill(t.media)
    val px by produceState<Px?>(null, t.id, still, f) { value = withContext(Dispatchers.Default) { Art.of(t, still, 72, f) } }
    PixImage(px, modifier)
}

/** A gout of dragon fire, painted in pixels: a swirl of flame filling the frame. */
@Composable
private fun Breath(f: Int, modifier: Modifier) {
    val px = remember(f) {
        val p = Px(48, 48)
        val flame = app.hoard.ui.pix.R.flame
        for (y in 0 until 48) for (x in 0 until 48) {
            val dx = (x - 24f) / 24f; val dy = (y - 24f) / 24f
            val d = kotlin.math.sqrt(dx * dx + dy * dy)
            if (d > 1f) continue
            val n = app.hoard.ui.pix.noise(x / 3, (y + f * 3) / 3, 17) + 0.5f
            val v = ((1f - d) * 1.3f + n * 0.5f - 0.35f).coerceIn(0f, 1f)
            if (v < 0.12f) continue
            p.set(x, y, flame[(v * (flame.size - 1) + app.hoard.ui.pix.bayer(x, y) * 0.6f).toInt().coerceIn(0, flame.size - 1)], (v * 255).toInt().coerceIn(0, 255))
        }
        p
    }
    PixImage(px, modifier)
}

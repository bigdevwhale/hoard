package app.hoard.ui.cave

import app.hoard.ui.theme.engraved
import app.hoard.ui.tips.HelpButton
import app.hoard.ui.tips.Tip
import app.hoard.ui.tips.TipHost
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hoard.R
import app.hoard.container
import app.hoard.data.Treasure
import app.hoard.domain.Feature
import app.hoard.domain.Kind
import app.hoard.domain.Quests
import app.hoard.domain.Roll
import app.hoard.ui.Go
import app.hoard.ui.HoardData
import app.hoard.ui.T
import app.hoard.ui.components.Art
import app.hoard.ui.components.CelebrateDialog
import app.hoard.ui.components.PixImage
import app.hoard.ui.components.PrimaryButton
import app.hoard.ui.components.SectionTitle
import app.hoard.ui.components.SoftButton
import app.hoard.ui.components.TreasureArt
import app.hoard.ui.components.UCard
import app.hoard.ui.components.rememberFrame
import app.hoard.ui.pix.Cave
import app.hoard.ui.pix.CaveLook
import app.hoard.ui.pix.Hit
import app.hoard.ui.pix.Mint
import app.hoard.ui.pix.PileItem
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private sealed interface Sheet {
    data object Forge : Sheet
    data object Chests : Sheet
    data object Quest : Sheet
    data class Rolled(val t: Treasure?) : Sheet
}

@Composable
fun CaveTab(data: HoardData, go: Go) {
    val context = LocalContext.current
    val c = context.container
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var awake by remember { mutableStateOf(false) }
    LaunchedEffect(awake) { if (awake) { delay(3500); awake = false } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(data.name.ifBlank { stringResource(R.string.dragon_default) }, style = UType.Title)
                Text(T.stage(context, data.stats.stage) + " · " + T.hue(context, data.settings.hue).lowercase(), style = UType.Muted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(data.stats.total.toString(), style = UType.Number.copy(color = U.c.gold))
                Text(stringResource(R.string.cave_count_label), style = UType.Small)
            }
            HelpButton(Tip.CAVE)
        }
        CaveScene(data, awake) { hit ->
            when (hit) {
                is Hit.Treasure -> go.treasure(hit.id)
                Hit.Dragon -> {
                    awake = true
                    sheet = Sheet.Rolled(roll(data, c))
                }
                Hit.Forge -> sheet = Sheet.Forge
                Hit.Chests -> sheet = Sheet.Chests
                Hit.Gnome -> sheet = Sheet.Quest
            }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(14.dp))
            val fresh = data.stats.toUnveil.size
            if (fresh > 0) {
                UCard(onClick = { go.unveil() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = U.c.ember, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.cave_cooled), style = UType.Strong)
                            Text(pluralStringResource(R.plurals.cave_cooled_text, fresh, fresh), style = UType.Muted)
                        }
                        Text(stringResource(R.string.cave_unveil), style = UType.Button.copy(color = U.c.gold))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            ForgeCard(data, go)
            if (data.stats.total == 0 && data.stats.inForge.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.cave_empty), style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(8.dp))
            }
            Spacer(Modifier.height(10.dp))
            QuestCard(data) { sheet = Sheet.Quest }
            val recent = data.all.filter { it.visible(data.now) }.take(12)
            if (recent.isNotEmpty()) {
                SectionTitle(stringResource(R.string.cave_recent))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    recent.forEach { t ->
                        Column(
                            Modifier.width(92.dp).clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).clickable { go.treasure(t.id) }.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TreasureArt(t, Modifier.size(64.dp), size = 48)
                            Spacer(Modifier.height(6.dp))
                            Text(T.title(context, t), style = UType.Small.copy(color = U.c.muted, textAlign = TextAlign.Center), maxLines = 2)
                        }
                    }
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    when (val s = sheet) {
        Sheet.Forge -> ForgeDialog(data, go) { sheet = null }
        Sheet.Chests -> ChestsDialog(data) { sheet = null }
        Sheet.Quest -> QuestDialog(data, go) { sheet = null }
        is Sheet.Rolled -> RolledDialog(s.t, data, go) { sheet = null }
        null -> { Celebrations(data); TipHost(Tip.CAVE) }
    }
}

/** A new stage of the dragon, a new thing in the cave: each celebrated once. */
@Composable
private fun Celebrations(data: HoardData) {
    val context = LocalContext.current
    val c = context.container
    val st = data.stats
    val seen = data.settings.seen
    val stageKey = "stage:" + st.stage.name
    val name = data.name.ifBlank { stringResource(R.string.dragon_default) }
    if (st.toUnveil.isNotEmpty()) return
    when {
        st.stage != app.hoard.domain.Stage.HATCHLING && stageKey !in seen -> CelebrateDialog(
            color = U.c.gold,
            eyebrow = stringResource(R.string.cel_stage),
            title = T.stage(context, st.stage),
            text = stringResource(R.string.cel_stage_text, name),
            onDismiss = { c.scope.launch { c.settings.update { it.copy(seen = it.seen + stageKey) } } },
        ) { PixImage(remember(st.stage) { app.hoard.ui.pix.Dragon.frame(data.settings.hue, st.stage, 0) }, Modifier.fillMaxSize()) }
        else -> {
            val f = st.features.firstOrNull { "feat:" + it.name !in seen } ?: return
            CelebrateDialog(
                color = U.c.rune,
                eyebrow = stringResource(R.string.cel_feature),
                title = T.feature(context, f),
                text = stringResource(R.string.cel_feature_text),
                onDismiss = { c.scope.launch { c.settings.update { it.copy(seen = it.seen + ("feat:" + f.name)) } } },
            ) { Icon(Icons.Rounded.AutoAwesome, null, tint = U.c.rune, modifier = Modifier.fillMaxSize(0.6f)) }
        }
    }
}

/** The dragon's pick for today: kept for the day, so tapping again shows the same one. */
private fun roll(data: HoardData, c: app.hoard.AppContainer): Treasure? {
    val day = data.today.toEpochDay()
    val s = data.settings
    if (s.rolledDay == day) return data.get(s.rolledId)
    val t = Roll.pick(data.all, data.now, data.zone, day)
    c.scope.launch { c.settings.update { it.copy(rolledDay = day, rolledId = t?.id ?: 0) } }
    return t
}

@Composable
private fun CaveScene(data: HoardData, awake: Boolean, onHit: (Hit) -> Unit) {
    val vault = LocalContext.current.container.vault
    val visible = remember(data.all, data.now) { data.all.filter { it.visible(data.now) } }
    val hour = data.today.let { java.time.LocalTime.now(data.zone).hour }
    val night = hour >= 20 || hour < 6
    val key = listOf(visible.take(10).map { it.id to it.kind + it.metal + it.stone + it.cut + it.relic }, data.stats.total, data.settings.hue, data.stats.inForge.size, data.stats.sealed.size, !data.questDone, night, awake)
    val cave by produceState<Cave?>(null, key) {
        value = withContext(Dispatchers.Default) {
            val items = visible.take(10).map { t -> PileItem(t.id, Art.mini(t, vault.still(t.media)), t.seed) }
            val look = CaveLook(
                hue = data.settings.hue,
                stage = data.stats.stage,
                count = data.stats.total,
                features = data.stats.features,
                items = items,
                metals = visible.filter { it.kindOf == Kind.COIN }.map { it.metalOf }.distinct(),
                stones = visible.filter { it.kindOf == Kind.GEM }.map { it.stoneOf.argb.toInt() }.distinct(),
                forging = data.stats.inForge.size,
                chests = data.stats.sealed.size,
                gnome = !data.questDone,
                night = night,
                awake = awake,
            )
            Cave(look)
        }
    }
    val frames by produceState<List<ImageBitmap>?>(null, cave) {
        val cv = cave ?: return@produceState
        value = withContext(Dispatchers.Default) { List(Cave.FRAMES) { cv.frame(it).bitmap() } }
    }
    val f = rememberFrame(Cave.FRAMES, 5)
    Box(Modifier.fillMaxWidth().aspectRatio(Cave.W / Cave.H.toFloat()).background(U.c.bg)) {
        val fr = frames
        if (fr != null) {
            Image(
                fr[f % fr.size], null,
                Modifier.fillMaxSize().pointerInput(cave) {
                    detectTapGestures { o ->
                        val cv = cave ?: return@detectTapGestures
                        val x = o.x / size.width * Cave.W
                        val y = o.y / size.height * Cave.H
                        cv.hits.firstOrNull { it.has(x, y) }?.let { onHit(it.hit) }
                    }
                },
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.None,
            )
        }
    }
}

@Composable
private fun ForgeCard(data: HoardData, go: Go) {
    val forging = data.stats.inForge
    if (forging.isEmpty()) return
    val at = forging.minOf { it.revealAt }
    val time = java.time.Instant.ofEpochMilli(at).atZone(data.zone).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    UCard(onClick = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalFireDepartment, null, tint = U.c.ember, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pluralStringResource(R.plurals.forge_in, forging.size, forging.size), style = UType.Strong)
                Text(stringResource(R.string.forge_cools_at, time), style = UType.Muted)
            }
        }
    }
}

@Composable
private fun QuestCard(data: HoardData, onClick: () -> Unit) {
    val context = LocalContext.current
    val reward = Quests.nextReward(data.settings)
    UCard(onClick = onClick) {
        Text(stringResource(R.string.quest_eyebrow).uppercase(), style = UType.Eyebrow.copy(color = U.c.rune))
        Spacer(Modifier.height(6.dp))
        Text(engraved(T.quest(context, data.quest.id)), style = UType.Name)
        Spacer(Modifier.height(4.dp))
        val daysLeft = (data.quest.endsDay - data.today.toEpochDay()).toInt() + 1
        Text(
            if (data.questDone) stringResource(R.string.quest_done)
            else pluralStringResource(R.plurals.quest_days, daysLeft, daysLeft) + (reward?.let { " · " + stringResource(R.string.quest_reward, T.material(context, it)) } ?: ""),
            style = UType.Muted.copy(color = if (data.questDone) U.c.leaf else U.c.muted),
        )
    }
}

@Composable
private fun ForgeDialog(data: HoardData, go: Go, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val forging = data.stats.inForge
    CelebrateDialog(
        color = U.c.ember,
        eyebrow = stringResource(R.string.forge_title),
        title = if (forging.isEmpty()) stringResource(R.string.forge_empty) else pluralStringResource(R.plurals.forge_in, forging.size, forging.size),
        text = if (forging.isEmpty()) stringResource(R.string.forge_empty_text)
        else forging.joinToString("\n") { T.kind(context, it.kindOf) + " · " + T.hall(context, it.hallOf) } + "\n\n" +
            stringResource(R.string.forge_cools_at, java.time.Instant.ofEpochMilli(forging.minOf { it.revealAt }).atZone(data.zone).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))),
        button = if (forging.isEmpty() && (data.left == null || data.left > 0)) stringResource(R.string.forge_put) else stringResource(R.string.nice),
        onDismiss = {
            onDismiss()
            if (forging.isEmpty() && (data.left == null || data.left > 0)) go.capture()
        },
        sparks = forging.isNotEmpty(),
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (forging.isEmpty()) U.c.faint else U.c.ember, modifier = Modifier.fillMaxSize(0.7f))
    }
}

@Composable
private fun ChestsDialog(data: HoardData, onDismiss: () -> Unit) {
    val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    val chests = data.stats.sealed.sortedBy { it.sealedUntil }
    val context = LocalContext.current
    CelebrateDialog(
        color = U.c.rune,
        eyebrow = stringResource(R.string.chests_title),
        title = pluralStringResource(R.plurals.chests_count, chests.size, chests.size),
        text = chests.joinToString("\n") { context.getString(R.string.chest_opens, java.time.Instant.ofEpochMilli(it.sealedUntil!!).atZone(data.zone).toLocalDate().format(fmt)) }
            + "\n\n" + stringResource(R.string.chests_text),
        onDismiss = onDismiss,
        sparks = false,
    ) {
        PixImage(remember { Art.chest(44) }, Modifier.fillMaxSize(0.8f))
    }
}

@Composable
private fun QuestDialog(data: HoardData, go: Go, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val reward = Quests.nextReward(data.settings)
    val canForge = !data.questDone && (data.left == null || data.left > 0)
    CelebrateDialog(
        color = U.c.rune,
        eyebrow = stringResource(R.string.quest_eyebrow),
        title = T.quest(context, data.quest.id),
        text = if (data.questDone) stringResource(R.string.quest_done_text)
        else stringResource(R.string.quest_text) + (reward?.let { "\n\n" + stringResource(R.string.quest_reward_text, T.material(context, it)) } ?: "\n\n" + stringResource(R.string.quest_reward_none)),
        button = if (canForge) stringResource(R.string.quest_go) else stringResource(R.string.nice),
        onDismiss = { onDismiss(); if (canForge) go.capture() },
        sparks = false,
    ) {
        PixImage(remember { app.hoard.ui.pix.Glyphs.icon(app.hoard.ui.pix.Glyphs.quest, app.hoard.ui.pix.rgb(232, 86, 66)) }, Modifier.fillMaxSize(0.7f))
    }
}

@Composable
private fun RolledDialog(t: Treasure?, data: HoardData, go: Go, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val name = data.name.ifBlank { stringResource(R.string.dragon_default) }
    if (t == null) {
        CelebrateDialog(
            color = U.c.gold,
            eyebrow = stringResource(R.string.roll_eyebrow),
            title = stringResource(R.string.roll_none_title, name),
            text = stringResource(R.string.roll_none_text),
            onDismiss = onDismiss,
            sparks = false,
        ) { PixImage(remember(data.settings.hue) { app.hoard.ui.pix.Dragon.frame(data.settings.hue, data.stats.stage, 2) }, Modifier.fillMaxSize()) }
        return
    }
    val years = Roll.years(t, data.zone, data.today)
    val ago = java.time.temporal.ChronoUnit.DAYS.between(T.local(t.momentAt, data.zone).toLocalDate(), data.today).toInt()
    CelebrateDialog(
        color = U.c.gold,
        eyebrow = stringResource(R.string.roll_eyebrow),
        title = T.title(context, t),
        text = (if (years > 0) pluralStringResource(R.plurals.roll_years, years, name, years) else pluralStringResource(R.plurals.roll_days, ago, name, ago)) + "\n\n" + T.legend(context, t),
        button = stringResource(R.string.roll_open),
        onDismiss = { onDismiss(); go.treasure(t.id) },
        onClose = onDismiss,
    ) { TreasureArt(t, Modifier.fillMaxSize(), size = 64) }
}

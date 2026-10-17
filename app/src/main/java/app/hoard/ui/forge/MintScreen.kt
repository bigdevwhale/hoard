package app.hoard.ui.forge

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hoard.R
import app.hoard.container
import app.hoard.data.ForgeMode
import app.hoard.data.Forger
import app.hoard.data.Order
import app.hoard.data.Places
import app.hoard.data.Runes
import app.hoard.data.Treasure
import app.hoard.domain.Cut
import app.hoard.domain.Daypart
import app.hoard.domain.Forge
import app.hoard.domain.Hall
import app.hoard.domain.Kind
import app.hoard.domain.Material
import app.hoard.domain.Metal
import app.hoard.domain.Quests
import app.hoard.domain.RelicForm
import app.hoard.domain.Sky
import app.hoard.domain.Stone
import app.hoard.ui.Go
import app.hoard.ui.HoardData
import app.hoard.ui.T
import app.hoard.ui.components.CelebrateDialog
import app.hoard.ui.components.Chip
import app.hoard.ui.components.PixImage
import app.hoard.ui.components.PrimaryButton
import app.hoard.ui.components.SectionTitle
import app.hoard.ui.components.SoftButton
import app.hoard.ui.components.SwitchRow
import app.hoard.ui.components.TopBar
import app.hoard.ui.components.UCard
import app.hoard.ui.components.UTextField
import app.hoard.ui.components.rememberFrame
import app.hoard.ui.locale
import app.hoard.ui.pix.Glyphs
import app.hoard.ui.pix.Mint
import app.hoard.ui.pix.Px
import app.hoard.ui.pix.rgb
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class Seal { NONE, MONTH, HALF, YEAR, DATE }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MintScreen(path: String, video: Boolean, data: HoardData, go: Go) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val file = remember(path) { File(path) }
    val s = data.settings
    val loc = locale()

    val still by produceState<Bitmap?>(null, path) { value = withContext(Dispatchers.IO) { c.vault.preview(file, video) } }
    val exifAt = remember(path) { if (video) null else c.vault.takenAt(file) }
    val exifPlace = remember(path) { if (video) null else c.vault.placeOf(file) }
    // A photo from the gallery keeps its own day; anything taken now, or "in the future", is now.
    val momentAt = remember(exifAt) { exifAt?.takeIf { it < data.now - 10 * 60_000 } }

    var kind by rememberSaveable { mutableStateOf(if (video) Kind.MIRROR else Kind.COIN) }
    var metal by rememberSaveable { mutableStateOf(Metal.GOLD) }
    var stone by rememberSaveable { mutableStateOf(Stone.RUBY) }
    var cut by rememberSaveable { mutableStateOf(Cut.ROUND) }
    var relic by rememberSaveable { mutableStateOf(RelicForm.CROWN) }
    var hall by rememberSaveable { mutableStateOf(Hall.WONDERS) }
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var forQuest by rememberSaveable { mutableStateOf(false) }
    var seal by rememberSaveable { mutableStateOf(Seal.NONE) }
    var sealDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickDate by remember { mutableStateOf(false) }
    var runes by remember { mutableStateOf(Runes()) }
    var hasLocation by remember { mutableStateOf(Places.allowed(context)) }
    var working by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<Pair<Treasure, Material?>?>(null) }

    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocation = it }
    LaunchedEffect(hasLocation, s.runes) {
        if (!s.runes) return@LaunchedEffect
        runes = if (exifPlace != null) Places.at(context, exifPlace.first, exifPlace.second, loc, withWeather = momentAt == null)
        else if (hasLocation && momentAt == null) Places.now(context, loc) else Runes()
        if (place.isBlank()) place = runes.place
    }

    val leave = {
        file.delete()
        go.back()
    }
    BackHandler(done == null) { leave() }

    val f = rememberFrame(8, 4)
    val art by produceState<Px?>(null, still, kind, metal, stone, cut, relic, f) {
        value = withContext(Dispatchers.Default) {
            when (kind) {
                Kind.COIN -> Mint.coin(still, metal, 64, f)
                Kind.GEM -> Mint.gem(still, stone, cut, 64, 0f, 0f, f)
                Kind.RELIC -> Mint.relic(still, relic, metal, 72, f)
                Kind.MIRROR -> Mint.mirror(still, metal, 56, 72, f)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.mint_title), onBack = { leave() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Box(Modifier.fillMaxWidth().height(220.dp).clip(U.RLg).background(U.c.surface), contentAlignment = Alignment.Center) {
                PixImage(art, Modifier.size(200.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(T.kindHint(context, kind), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())

            if (!video) {
                SectionTitle(stringResource(R.string.mint_kind))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Kind.COIN, Kind.GEM, Kind.RELIC).forEach { k -> Chip(T.kind(context, k), kind == k, { kind = k }) }
                }
            }

            when (kind) {
                Kind.GEM -> {
                    SectionTitle(stringResource(R.string.mint_stone))
                    Materials(Stone.entries, { it.starter || Material.S(it).key in s.unlocked }, stone, { stone = it }) { T.stone(context, it) }
                    SectionTitle(stringResource(R.string.mint_cut))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Cut.entries.forEach { k -> Chip(T.cut(context, k), cut == k, { cut = k }, U.c.gem) } }
                }
                Kind.RELIC -> {
                    SectionTitle(stringResource(R.string.mint_form))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { RelicForm.entries.forEach { r -> Chip(T.relic(context, r), relic == r, { relic = r }) } }
                    SectionTitle(stringResource(R.string.mint_metal))
                    Materials(Metal.entries, { it.starter || Material.M(it).key in s.unlocked }, metal, { metal = it }) { T.metal(context, it) }
                }
                else -> {
                    SectionTitle(stringResource(if (kind == Kind.MIRROR) R.string.mint_frame else R.string.mint_metal))
                    Materials(Metal.entries, { it.starter || Material.M(it).key in s.unlocked }, metal, { metal = it }) { T.metal(context, it) }
                }
            }

            SectionTitle(stringResource(R.string.mint_hall))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Hall.entries.forEach { h -> HallChip(h, hall == h) { hall = h } }
            }
            Text(T.hallHint(context, hall), style = UType.Muted, modifier = Modifier.padding(top = 8.dp, start = 4.dp))

            SectionTitle(stringResource(R.string.mint_name))
            UTextField(title, { title = it.take(60) }, placeholder = stringResource(R.string.mint_name_hint))
            Spacer(Modifier.height(10.dp))
            UTextField(note, { note = it.take(600) }, placeholder = stringResource(R.string.mint_note_hint), minLines = 3)

            SectionTitle(stringResource(R.string.mint_runes))
            UCard {
                RunesRow(runes, momentAt ?: data.now, data)
                Spacer(Modifier.height(10.dp))
                UTextField(place, { place = it.take(60) }, placeholder = stringResource(R.string.mint_place_hint))
                if (s.runes && !hasLocation && exifPlace == null && momentAt == null) {
                    Spacer(Modifier.height(10.dp))
                    SoftButton(stringResource(R.string.mint_allow_place), { askLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Place, color = U.c.rune, soft = U.c.runeSoft)
                }
                if (momentAt != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.mint_from_past, T.date(context, T.local(momentAt, data.zone).toLocalDate())), style = UType.Muted)
                }
            }

            if (!data.questDone) {
                SectionTitle(stringResource(R.string.quest_eyebrow))
                UCard {
                    SwitchRow(stringResource(R.string.mint_quest, T.quest(context, data.quest.id)), Quests.nextReward(s)?.let { stringResource(R.string.quest_reward, T.material(context, it)) }, forQuest, { forQuest = it }, U.c.rune)
                }
            }

            SectionTitle(stringResource(R.string.mint_seal))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.seal_none), seal == Seal.NONE, { seal = Seal.NONE }, U.c.rune)
                Chip(stringResource(R.string.seal_month), seal == Seal.MONTH, { seal = Seal.MONTH }, U.c.rune)
                Chip(stringResource(R.string.seal_half), seal == Seal.HALF, { seal = Seal.HALF }, U.c.rune)
                Chip(stringResource(R.string.seal_year), seal == Seal.YEAR, { seal = Seal.YEAR }, U.c.rune)
                Chip(
                    sealDate?.takeIf { seal == Seal.DATE }?.let { Instant.ofEpochMilli(it).atZone(data.zone).toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) } ?: stringResource(R.string.seal_date),
                    seal == Seal.DATE, { pickDate = true }, U.c.rune,
                )
            }
            if (seal != Seal.NONE) Text(stringResource(R.string.mint_seal_hint), style = UType.Muted, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.background(U.c.surface).navigationBarsPadding().padding(16.dp)) {
            val cools = Forge.revealAt(data.now, s, data.zone)
            Text(
                if (s.forge == ForgeMode.INSTANT) stringResource(R.string.mint_cools_now)
                else stringResource(R.string.mint_cools_at, Instant.ofEpochMilli(cools).atZone(data.zone).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))),
                style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                stringResource(R.string.mint_forge), {
                    if (working) return@PrimaryButton
                    working = true
                    val today = LocalDate.now(data.zone)
                    val until = when (seal) {
                        Seal.NONE -> null
                        Seal.MONTH -> today.plusMonths(1)
                        Seal.HALF -> today.plusMonths(6)
                        Seal.YEAR -> today.plusYears(1)
                        Seal.DATE -> sealDate?.let { Instant.ofEpochMilli(it).atZone(data.zone).toLocalDate() }
                    }?.atStartOfDay(data.zone)?.toInstant()?.toEpochMilli()
                    scope.launch {
                        val order = Order(kind, hall, metal, stone, cut, relic, title, note, place, runes, if (forQuest && !data.questDone) data.quest else null, until, momentAt)
                        done = withContext(Dispatchers.IO) { Forger.forge(c, file, video, order) }
                    }
                },
                Modifier.fillMaxWidth(), enabled = !working, icon = Icons.Rounded.LocalFireDepartment,
            )
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now(data.zone).plusMonths(3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.now(data.zone))
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        sealDate = d.atStartOfDay(data.zone).toInstant().toEpochMilli()
                        seal = Seal.DATE
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.save), color = U.c.gold) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel), color = U.c.muted) } },
        ) { DatePicker(state, showModeToggle = false) }
    }

    done?.let { (t, reward) ->
        val instant = !t.inForge(System.currentTimeMillis())
        CelebrateDialog(
            color = U.c.ember,
            eyebrow = stringResource(R.string.mint_done_eyebrow),
            title = stringResource(if (instant) R.string.mint_done_now else R.string.mint_done_title),
            text = (if (instant) stringResource(R.string.mint_done_now_text)
            else stringResource(R.string.mint_done_text, data.name.ifBlank { stringResource(R.string.dragon_default) }, Instant.ofEpochMilli(t.revealAt).atZone(data.zone).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))) +
                (reward?.let { "\n\n" + stringResource(R.string.mint_reward, T.material(context, it)) } ?: ""),
            onDismiss = { done = null; if (instant) { go.home(); go.unveil() } else go.home() },
        ) {
            androidx.compose.material3.Icon(Icons.Rounded.LocalFireDepartment, null, tint = U.c.ember, modifier = Modifier.fillMaxSize(0.75f))
        }
    }
}

/** A row of metals or stones; the locked ones show where they come from. */
@Composable
private fun <M> Materials(all: List<M>, open: (M) -> Boolean, selected: M, onSelect: (M) -> Unit, name: @Composable (M) -> String) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        all.forEach { m ->
            val ok = open(m)
            val on = m == selected
            val swatch = remember(m) {
                when (m) {
                    is Metal -> Mint.coin(null, m, 22)
                    is Stone -> Mint.gem(null, m, Cut.ROUND, 18)
                    else -> null
                }
            }
            Column(
                Modifier.width(78.dp).clip(U.RMd)
                    .background(if (on) U.c.goldSoft else U.c.surface)
                    .border(if (on) 1.5.dp else 1.dp, if (on) U.c.gold else U.c.line, U.RMd)
                    .clickable(enabled = ok) { onSelect(m) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PixImage(swatch, Modifier.size(36.dp))
                    if (!ok) Box(Modifier.size(36.dp).clip(U.RSm).background(Color(0xAA130F18)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Lock, null, tint = U.c.faint, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(name(m), style = UType.Small.copy(color = if (ok) U.c.ink else U.c.faint, textAlign = TextAlign.Center), maxLines = 2)
            }
        }
    }
    if (all.any { !open(it) }) Text(stringResource(R.string.mint_locked), style = UType.Small, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
}

@Composable
fun HallChip(h: Hall, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val color = Color(h.argb)
    val icon = remember(h) { Glyphs.icon(Glyphs.halls.getValue(h), h.argb.toInt()) }
    Row(
        Modifier.clip(U.RSm)
            .background(if (selected) color.copy(alpha = 0.16f) else U.c.surface)
            .border(1.dp, if (selected) color.copy(alpha = 0.6f) else U.c.line, U.RSm)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixImage(icon, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(T.hall(context, h), style = UType.Body.copy(color = if (selected) color else U.c.muted))
    }
}

/** The runes that will be struck: sky, time of day, place. */
@Composable
fun RunesRow(r: Runes, at: Long, data: HoardData) {
    val context = LocalContext.current
    val part = Daypart.of(T.local(at, data.zone).hour)
    val marks = buildList {
        if (r.sky != Sky.UNKNOWN) add(Glyphs.sky.getValue(r.sky) to (T.sky(context, r.sky) + (r.tempC?.let { ", " + (if (it > 0) "+" else "") + it + "°" } ?: "")))
        add(Glyphs.daypart.getValue(part) to T.daypart(context, part))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        marks.forEach { (pat, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixImage(remember(pat) { Glyphs.icon(pat, rgb(127, 227, 240)) }, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = UType.Body.copy(color = U.c.rune))
            }
        }
    }
}

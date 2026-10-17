package app.hoard.ui.chronicle

import app.hoard.ui.tips.HelpButton
import app.hoard.ui.tips.Tip
import app.hoard.ui.tips.TipHost
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hoard.R
import app.hoard.container
import app.hoard.domain.Feature
import app.hoard.domain.Kind
import app.hoard.domain.Stage
import app.hoard.export.Chronicle
import app.hoard.ui.Go
import app.hoard.ui.HoardData
import app.hoard.ui.T
import app.hoard.ui.components.Bar
import app.hoard.ui.components.SectionTitle
import app.hoard.ui.components.SoftButton
import app.hoard.ui.components.Stat
import app.hoard.ui.components.TreasureArt
import app.hoard.ui.components.UCard
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChronicleTab(data: HoardData, go: Go) {
    val context = LocalContext.current
    val c = context.container
    var year by rememberSaveable { mutableIntStateOf(data.today.year) }
    var status by remember { mutableStateOf<String?>(null) }
    val st = data.stats
    val years = remember(data.all) { (data.all.map { T.local(it.momentAt, data.zone).year } + data.today.year).toSortedSet() }
    val inYear = st.visible.filter { T.local(it.momentAt, data.zone).year == year }

    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        c.scope.launch {
            val ok = runCatching { withContext(Dispatchers.IO) { Chronicle.write(context, uri, year, data.all, data.settings, st.stage, c.vault, data.zone) } }.isSuccess
            status = context.getString(if (ok) R.string.pdf_done else R.string.pdf_failed)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.chronicle_title), style = UType.Title, modifier = Modifier.weight(1f).padding(start = 4.dp))
            HelpButton(Tip.CHRONICLE)
            IconButton(onClick = { years.lastOrNull { it < year }?.let { year = it } }, enabled = years.any { it < year }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = if (years.any { it < year }) U.c.ink else U.c.line)
            }
            Text(year.toString(), style = UType.Name)
            IconButton(onClick = { years.firstOrNull { it > year }?.let { year = it } }, enabled = years.any { it > year }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = if (years.any { it > year }) U.c.ink else U.c.line)
            }
        }
        Spacer(Modifier.height(8.dp))
        UCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Kind.entries.forEach { k -> Stat(inYear.count { it.kindOf == k }.toString(), T.kind(context, k), color = U.c.gold) }
            }
            Spacer(Modifier.height(12.dp))
            val days = inYear.map { T.local(it.momentAt, data.zone).toLocalDate() }.toSet().size
            Text(pluralStringResource(R.plurals.chronicle_days, days, days), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        }

        SectionTitle(stringResource(R.string.chronicle_dragon))
        UCard {
            val next = Stage.next(st.stage)
            Text(T.stage(context, st.stage), style = UType.Name)
            Spacer(Modifier.height(4.dp))
            if (next != null) {
                val need = next.from - st.total
                Text(pluralStringResource(R.plurals.chronicle_next_stage, need, need, T.stage(context, next).lowercase()), style = UType.Muted)
                Spacer(Modifier.height(10.dp))
                Bar((st.total - st.stage.from).toFloat() / (next.from - st.stage.from))
            } else Text(stringResource(R.string.chronicle_elder), style = UType.Muted)
        }

        SectionTitle(stringResource(R.string.chronicle_cave))
        UCard {
            Feature.entries.forEachIndexed { i, f ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                val p = f.progress(st)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(T.feature(context, f), style = UType.Body.copy(color = if (p >= 1f) U.c.ink else U.c.muted))
                        Text(T.featureGoal(context, f), style = UType.Small)
                    }
                    if (p >= 1f) Icon(Icons.Rounded.CheckCircle, null, tint = U.c.gold, modifier = Modifier.size(22.dp))
                    else Box(Modifier.width(72.dp)) { Bar(p, color = U.c.faint, track = U.c.surface2) }
                }
            }
        }

        SectionTitle(stringResource(R.string.chronicle_months))
        val byMonth = inYear.groupBy { T.local(it.momentAt, data.zone).monthValue }.toSortedMap(compareByDescending { it })
        if (byMonth.isEmpty()) Text(stringResource(R.string.chronicle_empty), style = UType.Sub, modifier = Modifier.padding(4.dp))
        byMonth.forEach { (m, list) ->
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                Text(T.month(context, m), style = UType.Name, modifier = Modifier.weight(1f))
                Text(pluralStringResource(R.plurals.treasures_count, list.size, list.size), style = UType.Small)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                list.sortedBy { it.momentAt }.forEach { t ->
                    Column(
                        Modifier.width(78.dp).clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).clickable { go.treasure(t.id) }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TreasureArt(t, Modifier.size(56.dp), size = 40)
                        Text(T.local(t.momentAt, data.zone).dayOfMonth.toString(), style = UType.Small)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(16.dp))
        SoftButton(stringResource(R.string.chronicle_pdf, year), { pdf.launch("Hoard-$year.pdf") }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PictureAsPdf)
        status?.let { Text(it, style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
        Spacer(Modifier.height(56.dp))
    }
    TipHost(Tip.CHRONICLE)
}

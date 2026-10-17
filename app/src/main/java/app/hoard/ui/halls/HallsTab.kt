package app.hoard.ui.halls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.hoard.data.Treasure
import app.hoard.domain.Hall
import app.hoard.ui.Go
import app.hoard.ui.HoardData
import app.hoard.ui.T
import app.hoard.ui.components.Art
import app.hoard.ui.components.Chip
import app.hoard.ui.components.PixImage
import app.hoard.ui.components.TreasureArt
import app.hoard.ui.forge.HallChip
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType

@Composable
fun HallsTab(data: HoardData, go: Go) {
    val context = LocalContext.current
    var hall by rememberSaveable { mutableStateOf<Hall?>(null) }
    var stars by rememberSaveable { mutableStateOf(false) }
    val kept = remember(data.all, data.now) { data.all.filter { !it.inForge(data.now) } }
    val shown = kept.filter { (hall == null || it.hallOf == hall) && (!stars || it.favorite) }

    LazyVerticalGrid(
        GridCells.Adaptive(104.dp),
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(stringResource(R.string.halls_title), style = UType.Title, modifier = Modifier.padding(top = 14.dp, start = 4.dp))
                Text(
                    hall?.let { T.hallHint(context, it) } ?: pluralStringResource(R.plurals.halls_sub, kept.size, kept.size),
                    style = UType.Muted, modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 12.dp),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip(stringResource(R.string.halls_all), hall == null && !stars, { hall = null; stars = false })
                    Chip("★", stars, { stars = !stars })
                    Hall.entries.forEach { h ->
                        val n = kept.count { it.hallOf == h }
                        if (n > 0 || hall == h) HallChip(h, hall == h) { hall = if (hall == h) null else h }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (shown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.halls_empty), style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(32.dp))
        }
        items(shown, key = { it.id }) { t -> Cell(t, data) { go.treasure(t.id) } }
    }
}

@Composable
private fun Cell(t: Treasure, data: HoardData, onClick: () -> Unit) {
    val context = LocalContext.current
    val sealed = t.sealed(data.now)
    Column(
        Modifier.clip(U.RMd).background(U.c.surface).border(1.dp, if (t.favorite) U.c.gold.copy(alpha = 0.5f) else U.c.line, U.RMd).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (sealed) PixImage(remember { Art.chest(44) }, Modifier.fillMaxWidth().aspectRatio(1f).padding(10.dp))
        else TreasureArt(t, Modifier.fillMaxWidth().aspectRatio(1f), size = 48)
        Spacer(Modifier.height(4.dp))
        Text(
            if (sealed) stringResource(R.string.treasure_sealed) else T.title(context, t),
            style = UType.Small.copy(color = U.c.ink, textAlign = TextAlign.Center), maxLines = 2, minLines = 2,
        )
        Text(T.day(context, T.local(t.momentAt, data.zone).toLocalDate()), style = UType.Small.copy(textAlign = TextAlign.Center), maxLines = 1)
    }
}

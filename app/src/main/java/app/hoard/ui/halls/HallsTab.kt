package app.hoard.ui.halls

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.hoard.container
import app.hoard.ui.components.Segmented
import app.hoard.ui.components.rememberStill
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
    val c = context.container
    val photos = data.settings.showPhotos
    val newest = data.settings.newestFirst
    val kept = remember(data.all, data.now) { data.all.filter { !it.inForge(data.now) } }
    // The photos view shows everything, fresh captures still in the forge included; the art waits for morning.
    val pool = if (photos) data.all else kept
    val shown = pool.filter { (hall == null || it.hallOf == hall) && (!stars || it.favorite) }
        .let { l -> if (newest) l.sortedByDescending { it.momentAt } else l.sortedBy { it.momentAt } }

    LazyVerticalGrid(
        GridCells.Adaptive(if (photos) 110.dp else 104.dp),
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
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Segmented(
                        listOf(false, true), photos,
                        { stringResource(if (it) R.string.halls_view_photos else R.string.halls_view_art) },
                        { v -> c.write { settings.update { s -> s.copy(showPhotos = v) } } },
                        Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(
                        Modifier.height(50.dp).clip(U.RSm).background(U.c.surface2).border(1.dp, U.c.line, U.RSm)
                            .clickable { c.write { settings.update { s -> s.copy(newestFirst = !s.newestFirst) } } }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (newest) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, null, tint = U.c.gold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(if (newest) R.string.halls_newest else R.string.halls_oldest), style = UType.Small.copy(color = U.c.ink))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (shown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.halls_empty), style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(32.dp))
        }
        items(shown, key = { it.id }) { t -> if (photos) PhotoCell(t, data) { go.treasure(t.id) } else Cell(t, data) { go.treasure(t.id) } }
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

/** The moment itself: the photo or the clip's first frame, square, with its day. */
@Composable
private fun PhotoCell(t: Treasure, data: HoardData, onClick: () -> Unit) {
    val context = LocalContext.current
    val sealed = t.sealed(data.now)
    val still = if (sealed) null else rememberStill(t.media)
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(U.RSm).background(U.c.surface).clickable(onClick = onClick)) {
        if (sealed) PixImage(remember { Art.chest(44) }, Modifier.fillMaxSize().padding(22.dp))
        else still?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))).padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(T.day(context, T.local(t.momentAt, data.zone).toLocalDate()), style = UType.Small.copy(color = Color.White), maxLines = 1)
        }
        if (t.video) Row(
            Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(50)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text("${(t.durationMs + 500) / 1000}s", style = UType.Small.copy(color = Color.White))
        }
        if (t.inForge(data.now)) Row(
            Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(50)).background(Color(0xCC2A140C)).padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, null, tint = U.c.ember, modifier = Modifier.size(14.dp))
            Text(java.time.Instant.ofEpochMilli(t.revealAt).atZone(data.zone).toLocalTime().format(java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)), style = UType.Small.copy(color = U.c.ember))
        }
        else if (t.favorite) Text("★", style = UType.Small.copy(color = U.c.gold), modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
    }
}

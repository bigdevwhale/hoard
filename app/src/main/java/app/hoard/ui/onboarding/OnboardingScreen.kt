package app.hoard.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hoard.R
import app.hoard.container
import app.hoard.data.ForgeMode
import app.hoard.domain.Hue
import app.hoard.domain.Stage
import app.hoard.ui.T
import app.hoard.ui.components.Chip
import app.hoard.ui.components.PixImage
import app.hoard.ui.components.PrimaryButton
import app.hoard.ui.components.SwitchRow
import app.hoard.ui.components.UCard
import app.hoard.ui.components.UTextField
import app.hoard.ui.components.rememberFrame
import app.hoard.ui.pix.Cave
import app.hoard.ui.pix.CaveLook
import app.hoard.ui.pix.Dragon
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var hue by rememberSaveable { mutableStateOf(Hue.EMERALD) }
    var limit by rememberSaveable { mutableIntStateOf(3) }
    var night by rememberSaveable { mutableStateOf(true) }
    val f = rememberFrame(Cave.FRAMES, 5)

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            when (step) {
                0 -> {
                    val frames by produceState<List<ImageBitmap>?>(null, hue) {
                        value = withContext(Dispatchers.Default) {
                            val cave = Cave(CaveLook(hue, Stage.HATCHLING, 3, emptySet(), forging = 1))
                            List(Cave.FRAMES) { cave.frame(it).bitmap() }
                        }
                    }
                    Box(Modifier.fillMaxWidth().aspectRatio(Cave.W / Cave.H.toFloat()).clip(U.RLg).background(U.c.surface)) {
                        frames?.let { androidx.compose.foundation.Image(it[f % it.size], null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, filterQuality = FilterQuality.None) }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(stringResource(R.string.onb_title), style = UType.Display)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.onb_text), style = UType.Sub)
                    Spacer(Modifier.height(14.dp))
                    listOf(R.string.onb_coin, R.string.onb_gem, R.string.onb_relic, R.string.onb_mirror).forEach {
                        Text("•  " + stringResource(it), style = UType.Body, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
                1 -> {
                    Text(stringResource(R.string.onb_name_title), style = UType.Display)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.onb_name_text), style = UType.Sub)
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(170.dp).clip(U.RLg).background(U.c.surface), contentAlignment = Alignment.Center) {
                        PixImage(remember(hue, f) { Dragon.frame(hue, Stage.YOUNG, f) }, Modifier.fillMaxWidth(0.8f).aspectRatio(Dragon.width(Stage.YOUNG) / Dragon.height(Stage.YOUNG).toFloat()))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Hue.entries.forEach { h ->
                            val col = androidx.compose.ui.graphics.Color(app.hoard.ui.pix.Dragons.of(h).scale[4])
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(col)
                                    .border(if (h == hue) 3.dp else 1.dp, if (h == hue) U.c.gold else U.c.line, CircleShape)
                                    .clickable { hue = h },
                            )
                        }
                    }
                    Text(T.hue(context, hue), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Spacer(Modifier.height(18.dp))
                    UTextField(name, { name = it.take(24) }, placeholder = stringResource(R.string.onb_name_hint))
                }
                else -> {
                    Text(stringResource(R.string.onb_rules_title), style = UType.Display)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.onb_rules_text), style = UType.Sub)
                    Spacer(Modifier.height(18.dp))
                    UCard {
                        Text(stringResource(R.string.set_limit), style = UType.Strong)
                        Text(stringResource(R.string.set_limit_hint), style = UType.Muted)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 5, 0).forEach { n -> Chip(if (n == 0) "∞" else n.toString(), limit == n, { limit = n }) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    UCard {
                        SwitchRow(stringResource(R.string.set_night), stringResource(R.string.set_night_hint), night, { night = it })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) {
                repeat(3) { i -> Box(Modifier.padding(end = 6.dp).size(if (i == step) 22.dp else 8.dp, 8.dp).clip(CircleShape).background(if (i == step) U.c.gold else U.c.line)) }
            }
            PrimaryButton(
                stringResource(if (step < 2) R.string.next else R.string.onb_start),
                {
                    if (step < 2) step++
                    else c.scope.launch {
                        c.settings.update {
                            it.copy(onboarded = true, dragonName = name.trim(), hue = hue, dailyLimit = limit, forge = if (night) ForgeMode.NIGHT else ForgeMode.INSTANT)
                        }
                        c.changed()
                        withContext(Dispatchers.Main) { onDone() }
                    }
                },
            )
        }
    }
}

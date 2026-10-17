package app.hoard.ui.tips

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hoard.R
import app.hoard.container
import app.hoard.domain.Stage
import app.hoard.ui.components.PixImage
import app.hoard.ui.components.PrimaryButton
import app.hoard.ui.components.SoftButton
import app.hoard.ui.components.rememberFrame
import app.hoard.ui.pix.Dragon
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One page of a hint: a short title and a few lines. */
class Page(@StringRes val title: Int, @StringRes val text: Int)

/**
 * The hints the dragon gives the first time a screen opens; "?" brings them back.
 * Seen hints are stored in [app.hoard.data.Settings.seen] as "tip:<name>".
 */
enum class Tip(val pages: List<Page>) {
    CAVE(
        listOf(
            Page(R.string.tip_cave_1_t, R.string.tip_cave_1),
            Page(R.string.tip_cave_2_t, R.string.tip_cave_2),
            Page(R.string.tip_cave_3_t, R.string.tip_cave_3),
            Page(R.string.tip_cave_4_t, R.string.tip_cave_4),
        ),
    ),
    CAPTURE(listOf(Page(R.string.tip_capture_t, R.string.tip_capture))),
    MINT(listOf(Page(R.string.tip_mint_1_t, R.string.tip_mint_1), Page(R.string.tip_mint_2_t, R.string.tip_mint_2))),
    UNVEIL(listOf(Page(R.string.tip_unveil_t, R.string.tip_unveil))),
    TREASURE(listOf(Page(R.string.tip_treasure_t, R.string.tip_treasure))),
    HALLS(listOf(Page(R.string.tip_halls_t, R.string.tip_halls))),
    CHRONICLE(listOf(Page(R.string.tip_chronicle_t, R.string.tip_chronicle))),
    LAIR(listOf(Page(R.string.tip_lair_t, R.string.tip_lair)));

    val key: String get() = "tip:$name"

    companion object {
        /** A hint reopened by its "?"; observed by [TipHost]. */
        internal var reopened by mutableStateOf<Tip?>(null)
    }
}

/** Shows [tip] the first time its screen opens, and again whenever its "?" is tapped. */
@Composable
fun TipHost(tip: Tip) {
    val container = LocalContext.current.container
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    val firstTime = s.onboarded && tip.key !in s.seen
    if (!firstTime && Tip.reopened != tip) return
    // Let the screen settle in before the hint comes up.
    var ready by remember { mutableStateOf(Tip.reopened == tip) }
    LaunchedEffect(Unit) { if (!ready) { delay(400); ready = true } }
    if (!ready) return
    fun close(all: Boolean) {
        Tip.reopened = null
        container.scope.launch {
            container.settings.update { st -> st.copy(seen = st.seen + if (all) Tip.entries.map { it.key } else listOf(tip.key)) }
        }
    }
    TipDialog(tip, s.hue, onDone = { close(false) }, onSkipAll = { close(true) })
}

/** The "?" that brings a screen's hint back. */
@Composable
fun HelpButton(tip: Tip) {
    IconButton(onClick = { Tip.reopened = tip }) {
        Icon(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.tip_help), tint = U.c.muted)
    }
}

@Composable
private fun TipDialog(tip: Tip, hue: app.hoard.domain.Hue, onDone: () -> Unit, onSkipAll: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val last = page == tip.pages.lastIndex
    val f = rememberFrame(Dragon.FRAMES, 4)
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 18.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .clip(U.RLg)
                    .background(U.c.surface)
                    .border(1.dp, U.c.gold.copy(alpha = 0.35f), U.RLg)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(78.dp, 52.dp).clip(U.RMd).background(U.c.surface2), contentAlignment = Alignment.Center) {
                        PixImage(remember(hue, f) { Dragon.frame(hue, Stage.YOUNG, f, awake = true) }, Modifier.size(72.dp, 44.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.tip_speaker).uppercase(), style = UType.Eyebrow.copy(color = U.c.gold))
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(tip.pages[page].title), style = UType.Name)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(tip.pages[page].text), style = UType.Body)
                Spacer(Modifier.height(16.dp))
                if (tip.pages.size > 1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        tip.pages.indices.forEach { i ->
                            Box(Modifier.padding(horizontal = 4.dp).size(if (i == page) 9.dp else 6.dp).clip(CircleShape).background(if (i == page) U.c.gold else U.c.faint))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (page > 0) SoftButton(stringResource(R.string.back), { page-- })
                    else SoftButton(stringResource(R.string.tip_skip_all), onSkipAll, color = U.c.muted, soft = U.c.surface2)
                    Spacer(Modifier.width(10.dp))
                    PrimaryButton(stringResource(if (last) R.string.tip_got_it else R.string.next), { if (last) onDone() else page++ }, Modifier.weight(1f))
                }
            }
        }
    }
}

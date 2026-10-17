package app.hoard.ui.me

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hoard.BuildConfig
import app.hoard.R
import app.hoard.container
import app.hoard.data.Backup
import app.hoard.data.ForgeMode
import app.hoard.data.Settings
import app.hoard.domain.Hue
import app.hoard.ui.Go
import app.hoard.ui.HoardData
import app.hoard.ui.Language
import app.hoard.ui.T
import app.hoard.ui.components.Chip
import app.hoard.ui.components.ConfirmDialog
import app.hoard.ui.components.InfoRow
import app.hoard.ui.components.SectionTitle
import app.hoard.ui.components.SwitchRow
import app.hoard.ui.components.TimeDialog
import app.hoard.ui.components.TopBar
import app.hoard.ui.components.UCard
import app.hoard.ui.components.UTextField
import app.hoard.ui.pix.Dragons
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(data: HoardData, go: Go) {
    val context = LocalContext.current
    val c = context.container
    val s = data.settings
    var name by remember { mutableStateOf(s.dragonName) }
    var timeOpen by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    fun set(change: (Settings) -> Settings) = c.write { settings.update(change) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        c.scope.launch {
            val ok = runCatching { withContext(Dispatchers.IO) { Backup.write(context, uri, c.db.dao().all(), c.vault) } }.isSuccess
            status = context.getString(if (ok) R.string.backup_done else R.string.backup_failed)
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings_title), onBack = { go.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle(stringResource(R.string.set_dragon))
            UCard {
                UTextField(name, { name = it.take(24); set { st -> st.copy(dragonName = name.trim()) } }, placeholder = stringResource(R.string.onb_name_hint))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Hue.entries.forEach { h ->
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(Color(Dragons.of(h).scale[4]))
                                .border(if (h == s.hue) 3.dp else 1.dp, if (h == s.hue) U.c.gold else U.c.line, CircleShape)
                                .clickable { set { st -> st.copy(hue = h) } },
                        )
                    }
                }
                Text(T.hue(context, s.hue), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }

            SectionTitle(stringResource(R.string.set_forge))
            UCard {
                Text(stringResource(R.string.set_limit), style = UType.Body)
                Text(stringResource(R.string.set_limit_hint), style = UType.Muted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5, 0).forEach { n -> Chip(if (n == 0) "∞" else n.toString(), s.dailyLimit == n, { set { st -> st.copy(dailyLimit = n) } }) }
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow(stringResource(R.string.set_night), stringResource(R.string.set_night_hint), s.forge == ForgeMode.NIGHT, { on -> set { st -> st.copy(forge = if (on) ForgeMode.NIGHT else ForgeMode.INSTANT) } })
                if (s.forge == ForgeMode.NIGHT) InfoRow(Icons.Rounded.Schedule, stringResource(R.string.set_forge_at), "%02d:%02d".format(s.forgeAt / 60, s.forgeAt % 60), onClick = { timeOpen = true })
                SwitchRow(stringResource(R.string.set_notify), stringResource(R.string.set_notify_hint), s.notify, { on -> set { st -> st.copy(notify = on) } })
                SwitchRow(stringResource(R.string.set_gallery), stringResource(R.string.set_gallery_hint), s.toGallery, { on -> set { st -> st.copy(toGallery = on) } })
                SwitchRow(stringResource(R.string.set_runes), stringResource(R.string.set_runes_hint), s.runes, { on -> set { st -> st.copy(runes = on) } }, U.c.rune)
            }

            SectionTitle(stringResource(R.string.set_language))
            UCard {
                val cur = Language.current()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.lang_system), cur == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                    Chip("English", cur == Language.EN, { Language.set(Language.EN) })
                    Chip("Русский", cur == Language.RU, { Language.set(Language.RU) })
                }
            }

            SectionTitle(stringResource(R.string.set_backup))
            UCard {
                Text(stringResource(R.string.set_backup_hint), style = UType.Muted)
                InfoRow(Icons.Rounded.Upload, stringResource(R.string.backup_export), stringResource(R.string.backup_export_hint), onClick = { export.launch("Hoard.zip") })
                InfoRow(Icons.Rounded.Download, stringResource(R.string.backup_import), stringResource(R.string.backup_import_hint), onClick = { import.launch(arrayOf("application/zip", "application/octet-stream")) })
                status?.let { Text(it, style = UType.Muted.copy(color = U.c.gold)) }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.about, BuildConfig.VERSION_NAME), style = UType.Small.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(40.dp))
        }
    }

    if (timeOpen) TimeDialog(s.forgeAt, onDismiss = { timeOpen = false }) { m -> set { st -> st.copy(forgeAt = m) } }
    restoreUri?.let { uri ->
        ConfirmDialog(
            stringResource(R.string.backup_import), stringResource(R.string.backup_import_confirm), stringResource(R.string.backup_import_do),
            onConfirm = {
                c.scope.launch {
                    val ok = runCatching {
                        withContext(Dispatchers.IO) {
                            val (list, dir) = Backup.read(context, uri)
                            c.vault.wipe()
                            c.vault.adopt(dir)
                            c.db.dao().replaceAll(list)
                        }
                    }.isSuccess
                    c.settings.update { it.copy(onboarded = true) }
                    c.changed()
                    status = context.getString(if (ok) R.string.backup_restored else R.string.backup_bad)
                }
            },
            onDismiss = { restoreUri = null }, danger = true,
        )
    }
}

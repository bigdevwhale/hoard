package app.hoard.ui.capture

import app.hoard.ui.tips.HelpButton
import app.hoard.ui.tips.Tip
import app.hoard.ui.tips.TipHost
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.hoard.R
import app.hoard.container
import app.hoard.ui.Go
import app.hoard.ui.HoardData
import app.hoard.ui.T
import app.hoard.ui.components.IconDisc
import app.hoard.ui.components.PrimaryButton
import app.hoard.ui.components.SoftButton
import app.hoard.ui.components.TopBar
import app.hoard.ui.components.UCard
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** How long a mirror can hold: a clip shot here, and the longest one taken from the gallery. */
const val CLIP_MS = 5_000L
const val CLIP_MAX_IMPORT_MS = 30_000L

/** The phone's own video camera, asked to stop at a mirror's length (not every camera app listens). */
private class CaptureClip : ActivityResultContracts.CaptureVideo() {
    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).putExtra(MediaStore.EXTRA_DURATION_LIMIT, (CLIP_MS / 1000).toInt())
}

@Composable
fun CaptureScreen(data: HoardData, go: Go) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    // The file the camera app is writing into; it survives the app being put away meanwhile.
    var pending by rememberSaveable { mutableStateOf<String?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val video = context.contentResolver.getType(uri)?.startsWith("video") == true
            val f = withContext(Dispatchers.IO) { c.vault.importToScratch(uri, video) }
            if (f == null) { message = context.getString(R.string.capture_import_failed); return@launch }
            if (video && withContext(Dispatchers.IO) { c.vault.durationMs(f) } > CLIP_MAX_IMPORT_MS) {
                f.delete()
                message = context.getString(R.string.capture_clip_long, (CLIP_MAX_IMPORT_MS / 1000).toInt())
                return@launch
            }
            go.mint(f.path, video)
        }
    }

    /** What came back from the camera app: nothing (put away), too long a clip, or a moment for the forge. */
    fun caught(ok: Boolean, video: Boolean) {
        val f = pending?.let(::File) ?: return
        pending = null
        if (!ok || !f.exists() || f.length() == 0L) { f.delete(); return }
        if (!video) { go.mint(f.path, false); return }
        scope.launch {
            if (withContext(Dispatchers.IO) { c.vault.durationMs(f) } > CLIP_MAX_IMPORT_MS) {
                f.delete()
                message = context.getString(R.string.capture_clip_long, (CLIP_MAX_IMPORT_MS / 1000).toInt())
            } else go.mint(f.path, true)
        }
    }

    val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { caught(it, false) }
    val clip = rememberLauncherForActivityResult(CaptureClip()) { caught(it, true) }

    fun shoot(video: Boolean) {
        val f = c.vault.scratch(if (video) "mp4" else "jpg")
        val uri = runCatching { FileProvider.getUriForFile(context, context.packageName + ".files", f) }.getOrNull()
            ?: run { message = context.getString(R.string.capture_failed); return }
        pending = f.path
        try {
            if (video) clip.launch(uri) else photo.launch(uri)
        } catch (_: ActivityNotFoundException) {
            pending = null
            f.delete()
            message = context.getString(R.string.capture_no_camera)
        }
    }

    val full = data.left == 0
    if (full) {
        Column(
            Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.capture_full_title), style = UType.Title.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.capture_full_text, data.settings.dailyLimit), style = UType.Sub.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(24.dp))
            PrimaryButton(stringResource(R.string.back), { go.back() }, Modifier.fillMaxWidth())
        }
        return
    }

    // Copies into the gallery need this on Android 9 and older.
    val askStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT < 29 && data.settings.toGallery &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) askStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(stringResource(R.string.forge_title), onBack = { go.back() }) {
                HelpButton(Tip.CAPTURE)
                Text(
                    data.left?.let { stringResource(R.string.forge_left, it, data.settings.dailyLimit) } ?: stringResource(R.string.forge_unlimited),
                    style = UType.Small.copy(color = U.c.gold),
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(U.c.goldSoft).padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                if (!data.questDone) {
                    Text(
                        stringResource(R.string.capture_quest, T.quest(context, data.quest.id)),
                        style = UType.Small.copy(color = U.c.rune, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(U.c.runeSoft).padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Choice(Icons.Rounded.PhotoCamera, stringResource(R.string.capture_photo), stringResource(R.string.capture_photo_sub), U.c.gold, U.c.goldSoft) { shoot(false) }
                Spacer(Modifier.height(12.dp))
                Choice(
                    Icons.Rounded.Videocam, stringResource(R.string.capture_clip, (CLIP_MS / 1000).toInt()), stringResource(R.string.capture_clip_sub),
                    U.c.gem, U.c.gemSoft,
                ) { shoot(true) }
                Spacer(Modifier.height(20.dp))
                SoftButton(
                    stringResource(R.string.capture_gallery),
                    { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoLibrary,
                )
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.capture_hint), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
        }
        TipHost(Tip.CAPTURE)
        message?.let {
            LaunchedEffect(it) { delay(3500); message = null }
            Text(
                it, style = UType.Body.copy(color = Color.White, textAlign = TextAlign.Center),
                modifier = Modifier.align(Alignment.Center).padding(28.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xCC000000)).padding(16.dp),
            )
        }
    }
}

/** One big way to catch a moment: an icon, what it is, and a line on what happens. */
@Composable
private fun Choice(icon: ImageVector, title: String, sub: String, color: Color, soft: Color, onClick: () -> Unit) {
    UCard(onClick = onClick, padding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconDisc(icon, 56.dp, color, soft)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = UType.Strong.copy(color = color))
                Spacer(Modifier.height(2.dp))
                Text(sub, style = UType.Muted)
            }
        }
    }
}

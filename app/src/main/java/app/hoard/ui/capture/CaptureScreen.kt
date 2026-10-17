package app.hoard.ui.capture

import app.hoard.ui.tips.HelpButton
import app.hoard.ui.tips.Tip
import app.hoard.ui.tips.TipHost
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.hoard.R
import app.hoard.container
import app.hoard.ui.Go
import app.hoard.ui.HoardData
import app.hoard.ui.T
import app.hoard.ui.components.PrimaryButton
import app.hoard.ui.components.SoftButton
import app.hoard.ui.theme.U
import app.hoard.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** How long a mirror can hold: a clip recorded here, and the longest one taken from the gallery. */
const val CLIP_MS = 5_000L
const val CLIP_MAX_IMPORT_MS = 30_000L

@Composable
fun CaptureScreen(data: HoardData, go: Go) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

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

    val perms = remember {
        listOfNotNull(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, if (android.os.Build.VERSION.SDK_INT < 29 && data.settings.toGallery) Manifest.permission.WRITE_EXTERNAL_STORAGE else null).toTypedArray()
    }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        granted = r[Manifest.permission.CAMERA] == true
    }
    LaunchedEffect(Unit) {
        if (!granted) ask.launch(perms)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val gallery = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
        if (granted) Camera(go, gallery, onError = { message = it })
        else Column(
            Modifier.align(Alignment.Center).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.capture_no_camera), style = UType.Sub.copy(textAlign = TextAlign.Center, color = Color.White))
            Spacer(Modifier.height(16.dp))
            SoftButton(stringResource(R.string.capture_allow), { ask.launch(perms) })
            Spacer(Modifier.height(10.dp))
            SoftButton(stringResource(R.string.capture_gallery), gallery, icon = Icons.Rounded.PhotoLibrary)
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { go.back() }) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel), tint = Color.White) }
            Spacer(Modifier.weight(1f))
            HelpButton(Tip.CAPTURE)
            Text(
                data.left?.let { stringResource(R.string.forge_left, it, data.settings.dailyLimit) } ?: stringResource(R.string.forge_unlimited),
                style = UType.Small.copy(color = Color.White),
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x66000000)).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        if (!data.questDone) {
            Text(
                stringResource(R.string.capture_quest, T.quest(context, data.quest.id)),
                style = UType.Small.copy(color = U.c.rune, textAlign = TextAlign.Center),
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0x88000000)).padding(horizontal = 12.dp, vertical = 6.dp),
            )
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

@SuppressLint("MissingPermission")
@Composable
private fun Camera(go: Go, onGallery: () -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val vault = context.container.vault
    val scope = rememberCoroutineScope()
    var back by remember { mutableStateOf(true) }
    val preview = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var image by remember { mutableStateOf<ImageCapture?>(null) }
    var video by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var busy by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    var stopper by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(back) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val selector = if (back) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            val p = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }
            val ic = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            val vc = VideoCapture.withOutput(Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD, androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))).build())
            provider.unbindAll()
            // Some phones can't run photo and video together; then clips are left out.
            val withVideo = runCatching { provider.bindToLifecycle(owner, selector, p, ic, vc) }.isSuccess
            if (!withVideo) runCatching { provider.bindToLifecycle(owner, selector, p, ic) }
            image = ic
            video = if (withVideo) vc else null
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            recording?.stop()
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    fun photo() {
        val ic = image ?: return
        if (busy) return
        busy = true
        val f = vault.scratch("jpg")
        ic.takePicture(ImageCapture.OutputFileOptions.Builder(f).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) { busy = false; go.mint(f.path, false) }
            override fun onError(exception: ImageCaptureException) { busy = false; onError(context.getString(R.string.capture_failed)) }
        })
    }

    fun stop() {
        stopper?.cancel()
        recording?.stop()
        recording = null
    }

    fun record() {
        val vc = video ?: run { onError(context.getString(R.string.capture_no_video)); return }
        if (busy || recording != null) return
        val f = vault.scratch("mp4")
        val started = System.currentTimeMillis()
        val audio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val pending = vc.output.prepareRecording(context, FileOutputOptions.Builder(f).build()).let { if (audio) it.withAudioEnabled() else it }
        recording = pending.start(ContextCompat.getMainExecutor(context)) { e ->
            if (e is VideoRecordEvent.Finalize) {
                scope.launch { progress.snapTo(0f) }
                if (!e.hasError() || e.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED) {
                    if (System.currentTimeMillis() - started < 700) { f.delete(); onError(context.getString(R.string.capture_hold)) }
                    else go.mint(f.path, true)
                } else { f.delete(); onError(context.getString(R.string.capture_failed)) }
            }
        }
        stopper = scope.launch {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(CLIP_MS.toInt(), easing = LinearEasing))
            stop()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView({ preview }, Modifier.fillMaxSize())
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0x55000000)).clickable { if (recording == null) onGallery() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PhotoLibrary, stringResource(R.string.capture_gallery), tint = Color.White)
            }
            Spacer(Modifier.width(40.dp))
            Box(
                Modifier.size(84.dp).pointerInput(image, video) {
                    detectTapGestures(
                        onTap = { photo() },
                        onLongPress = { record() },
                        onPress = {
                            tryAwaitRelease()
                            if (recording != null) stop()
                        },
                    )
                },
                contentAlignment = Alignment.Center,
            ) {
                val rec = recording != null
                Canvas(Modifier.fillMaxSize()) {
                    val w = 5.dp.toPx()
                    drawCircle(Color.White, size.minDimension / 2 - w / 2, style = Stroke(w))
                    if (rec) drawArc(Color(0xFFFF5A4E), -90f, 360f * progress.value, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
                }
                Box(Modifier.size(if (rec) 34.dp else 64.dp).clip(if (rec) RoundedCornerShape(8.dp) else CircleShape).background(if (rec) Color(0xFFFF5A4E) else Color(0xFFFFE2A0)).border(1.dp, Color(0x33000000), CircleShape))
            }
            Spacer(Modifier.width(40.dp))
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0x55000000)).clickable { if (recording == null) back = !back }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Flip, stringResource(R.string.capture_flip), tint = Color.White)
            }
        }
        Text(
            stringResource(R.string.capture_hint),
            style = UType.Small.copy(color = Color(0xCCFFFFFF), textAlign = TextAlign.Center),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp),
        )
    }
}

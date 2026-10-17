package app.hoard.ui.treasure

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * A clip playing on a loop, cropped to fill its box. A TextureView rather than a SurfaceView, so the
 * picture can be clipped to the oval of a mirror.
 */
@OptIn(UnstableApi::class)
@Composable
fun ClipPlayer(file: File, muted: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    player.volume = if (muted) 0f else 1f
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                fun crop(vs: VideoSize) {
                    if (vs.width == 0 || vs.height == 0 || tv.width == 0 || tv.height == 0) return
                    val vw = vs.width * vs.pixelWidthHeightRatio
                    val vh = vs.height.toFloat()
                    val (w, h) = tv.width.toFloat() to tv.height.toFloat()
                    // The view stretches the video to fill; undo that, then scale up to cover.
                    val k = maxOf(w / vw, h / vh)
                    val m = Matrix()
                    m.setScale(vw * k / w, vh * k / h, w / 2f, h / 2f)
                    tv.setTransform(m)
                }
                player.setVideoTextureView(tv)
                player.addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) = crop(videoSize)
                })
                tv.surfaceTextureListener?.let { inner ->
                    tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener by inner {
                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                            inner.onSurfaceTextureSizeChanged(st, width, height)
                            crop(player.videoSize)
                        }
                    }
                }
                tv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> crop(player.videoSize) }
            }
        },
        modifier = modifier,
    )
}

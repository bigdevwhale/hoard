package app.hoard

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.hoard.data.DemoSeed
import app.hoard.data.Settings
import app.hoard.ui.HoardRoot
import app.hoard.ui.theme.HoardTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the main screens with the demo hoard to PNGs (run with `-Prender`): a look without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class RenderScreens {
    @get:Rule
    val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }
    private val photos = File(System.getProperty("render.photos") ?: "src/demo/assets/demo")

    private fun settle(ms: Long = 3000) {
        rule.mainClock.autoAdvance = false
        var t = 0L
        while (t < ms) {
            Thread.sleep(60)
            rule.mainClock.advanceTimeBy(100)
            t += 100
        }
    }

    private fun shot(name: String) {
        // Draw the window by hand: capturing waits for a frame, which a paused clock never gives.
        val root = rule.activity.window.decorView
        val b = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(b))
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tap(text: String) {
        rule.onAllNodesWithText(text, substring = true)[0].performClick()
        settle(2500)
    }

    @Test
    fun screens() {
        val app = ApplicationProvider.getApplicationContext<HoardApp>()
        runBlocking { DemoSeed.seedIfEmpty(app, app.container) { name -> File(photos, name.removePrefix("demo/")).inputStream() } }
        rule.setContent {
            val s: Settings? by app.container.settings.settings.collectAsState(initial = null)
            HoardTheme { s?.let { HoardRoot(it) } }
        }
        settle(5000)
        shot("1-cave")
        tap("Bike ride to Roztoky")
        shot("2-coin")
        rule.onAllNodesWithContentDescription("Back")[0].performClick()
        settle(1500)
        tap("The last strawberries")
        shot("3-gem")
        rule.onAllNodesWithContentDescription("Back")[0].performClick()
        settle(1500)
        tap("Halls")
        shot("4-halls")
        tap("Chronicle")
        shot("5-chronicle")
    }
}

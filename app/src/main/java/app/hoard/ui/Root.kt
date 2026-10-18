package app.hoard.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Castle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.hoard.BuildConfig
import app.hoard.R
import app.hoard.container
import app.hoard.data.Settings
import app.hoard.data.Treasure
import app.hoard.domain.Forge
import app.hoard.domain.HoardStats
import app.hoard.domain.Quests
import app.hoard.notify.Alarms
import app.hoard.ui.capture.CaptureScreen
import app.hoard.ui.cave.CaveTab
import app.hoard.ui.chronicle.ChronicleTab
import app.hoard.ui.components.rememberNow
import app.hoard.ui.forge.MintScreen
import app.hoard.ui.forge.UnveilScreen
import app.hoard.ui.halls.HallsTab
import app.hoard.ui.me.SettingsScreen
import app.hoard.ui.onboarding.OnboardingScreen
import app.hoard.ui.theme.Nunito
import app.hoard.ui.theme.U
import app.hoard.ui.treasure.TreasureScreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { CAVE, HALLS, CHRONICLE }

/** Everything stored, plus what is derived from it, recomputed once a minute or on change. */
@Immutable
class HoardData(val all: List<Treasure>, val settings: Settings, val now: Long) {
    val zone: ZoneId = ZoneId.systemDefault()
    val today: LocalDate = Forge.day(now, zone)
    val stats = HoardStats(all, now, zone)
    val left: Int? = Forge.left(all, settings, now, zone)
    val quest = Quests.active(today.toEpochDay())
    val questDone = quest.key in settings.claimed
    val name: String get() = settings.dragonName
    fun get(id: Long) = all.firstOrNull { it.id == id }
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun capture() = nav.navigate("capture") { launchSingleTop = true }
    fun mint(path: String, video: Boolean) = nav.navigate("mint/" + Uri.encode(path) + "/" + video) { popUpTo("main") }
    fun unveil() = nav.navigate("unveil") { launchSingleTop = true }
    fun treasure(id: Long) = nav.navigate("treasure/$id")
    fun settings() = nav.navigate("settings")
    fun home() = nav.popBackStack("main", false)
    fun back() = nav.popBackStack()
}

@Composable
fun HoardRoot(settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) }
    var tab by rememberSaveable { mutableStateOf(Tab.CAVE) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val all by container.db.dao().observe().collectAsStateWithLifecycle(initialValue = null)
    val minute = rememberNow(30_000)
    val data = remember(all, settings, minute) { HoardData(all.orEmpty(), settings, System.currentTimeMillis()) }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    // Onboarded elsewhere (a restored backup, the demo seed): leave the welcome screens.
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }
    // Fresh treasure waiting when the app opens: go straight to the unveiling, once.
    var autoUnveiled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(all != null, route) {
        if (all != null && route == "main" && settings.onboarded && !autoUnveiled && data.stats.toUnveil.isNotEmpty()) {
            autoUnveiled = true
            go.unveil()
        }
    }
    LaunchedEffect(route) { if (route == "main") Alarms.clear(context) }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && (settings.notify || settings.remindCount > 0) && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.background(U.c.bg),
        enterTransition = { fadeIn(tween(240)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable("welcome") {
            OnboardingScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } }
        }
        composable("main") { MainTabs(tab, { tab = it }, go, data) }
        composable("capture") { CaptureScreen(data, go) }
        composable(
            "mint/{path}/{video}",
            arguments = listOf(navArgument("path") { type = NavType.StringType }, navArgument("video") { type = NavType.BoolType }),
        ) { e ->
            MintScreen(e.arguments?.getString("path").orEmpty(), e.arguments?.getBoolean("video") ?: false, data, go)
        }
        composable("unveil") { UnveilScreen(data, go) }
        composable("treasure/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            TreasureScreen(e.arguments?.getLong("id") ?: 0L, data, go)
        }
        composable("settings") { SettingsScreen(data, go) }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, data: HoardData) {
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.CAVE -> CaveTab(data, go)
                Tab.HALLS -> HallsTab(data, go)
                Tab.CHRONICLE -> ChronicleTab(data, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Box(Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavItem(Icons.Rounded.Castle, stringResource(R.string.tab_cave), tab == Tab.CAVE) { onTab(Tab.CAVE) }
                NavItem(Icons.Rounded.AutoAwesome, stringResource(R.string.tab_halls), tab == Tab.HALLS) { onTab(Tab.HALLS) }
                Spacer(Modifier.weight(1f))
                NavItem(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.tab_chronicle), tab == Tab.CHRONICLE) { onTab(Tab.CHRONICLE) }
                NavItem(Icons.Rounded.Settings, stringResource(R.string.tab_settings), false) { go.settings() }
            }
            ForgeButton(data, go, Modifier.align(Alignment.TopCenter).offset(y = (-18).dp))
        }
    }
}

/** The way into the forge: lit while there's room today, cold once the day's treasures are in. */
@Composable
private fun ForgeButton(data: HoardData, go: Go, modifier: Modifier) {
    val open = data.left == null || data.left > 0
    val glow = if (open) Brush.radialGradient(listOf(Color(0xFFFFD27A), Color(0xFFE0862E))) else Brush.radialGradient(listOf(U.c.surface2, U.c.surface2))
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .shadow(if (open) 14.dp else 0.dp, CircleShape, ambientColor = U.c.ember, spotColor = U.c.ember)
                .clip(CircleShape)
                .background(glow)
                .border(2.dp, if (open) Color(0xFFFFE7AE) else U.c.line, CircleShape)
                .clickable { go.capture() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, stringResource(R.string.forge_open), Modifier.size(32.dp), tint = if (open) U.c.onGold else U.c.faint)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            data.left?.let { stringResource(R.string.forge_left, it, data.settings.dailyLimit) } ?: stringResource(R.string.forge_unlimited),
            fontFamily = Nunito, fontSize = 11.sp, color = if (open) U.c.gold else U.c.faint,
        )
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.gold else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.goldSoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Nunito, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}

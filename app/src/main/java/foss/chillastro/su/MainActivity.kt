@file:OptIn(ExperimentalMaterial3Api::class)

package foss.chillastro.su

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import foss.chillastro.su.ui.theme.FOSSRootCheckerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isSystemDark = isSystemInDarkTheme()
            var manualDarkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
            var useDynamic by rememberSaveable { mutableStateOf(true) }

            val currentTheme = manualDarkOverride ?: isSystemDark

            FOSSRootCheckerTheme(darkTheme = currentTheme, dynamicColor = useDynamic) {
                FOSSRootApp(
                    dark = currentTheme,
                    onDark = { manualDarkOverride = it },
                    dyn = useDynamic,
                    onDyn = { useDynamic = it }
                )
            }
        }
    }
}

@Composable
fun FOSSRootApp(dark: Boolean, onDark: (Boolean) -> Unit, dyn: Boolean, onDyn: (Boolean) -> Unit) {
    var dest by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var showHistory by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val config = LocalConfiguration.current

    val navType = if (config.screenWidthDp >= 600) NavigationSuiteType.NavigationRail else NavigationSuiteType.NavigationBar

    BackHandler(dest != AppDestinations.HOME) { dest = AppDestinations.HOME }

    NavigationSuiteScaffold(
        layoutType = navType,
        navigationSuiteItems = {
            AppDestinations.entries.forEach { item ->
                item(
                    icon = { Icon(item.icon, null) },
                    label = { Text(item.label) },
                    selected = dest == item,
                    onClick = { dest = item }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = buildAnnotatedString {
                                append("ROOT CHECKER ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("[ FOSS ]")
                                }
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    actions = {
                        IconButton(onClick = { showHistory = true }) {
                            Icon(Icons.Rounded.History, "History")
                        }
                    }
                )
            }
        ) { padding ->
            AnimatedContent(
                targetState = dest,
                modifier = Modifier.padding(padding),
                transitionSpec = {
                    val spec = spring<IntOffset>(stiffness = Spring.StiffnessLow)
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally(spec) { it } + fadeIn() togetherWith slideOutHorizontally(spec) { -it } + fadeOut()
                    } else {
                        slideInHorizontally(spec) { -it } + fadeIn() togetherWith slideOutHorizontally(spec) { it } + fadeOut()
                    }
                }, label = "PageTransition"
            ) { target ->
                when (target) {
                    AppDestinations.HOME -> CheckerScreen()
                    AppDestinations.GUIDE -> GuideScreen()
                    AppDestinations.SETTINGS -> SettingsScreen(dark, onDark, dyn, onDyn)
                }
            }

            if (showHistory) {
                HistorySheet(onDismiss = { showHistory = false }, state = sheetState)
            }
        }
    }
}

@Composable
fun CheckerScreen() {
    var checkState by rememberSaveable { mutableIntStateOf(0) }
    var isRooted by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val config = LocalConfiguration.current

    val circleScale by animateFloatAsState(
        targetValue = if (checkState == 1) 1.15f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "CircleScale"
    )

    val containerColor by animateColorAsState(
        targetValue = when(checkState) {
            2 -> if (isRooted) Color(0xFF4CAF50) else Color(0xFFB00020) // Consistent across themes
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(600),
        label = "ColorTransition"
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = if (config.screenWidthDp >= 600) Modifier.fillMaxWidth(0.6f) else Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE}", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(64.dp))

            Box(contentAlignment = Alignment.Center) {
                if (checkState == 1) CircularProgressIndicator(Modifier.size(240.dp), strokeWidth = 6.dp, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)

                Surface(
                    Modifier.size(180.dp).graphicsLayer(scaleX = circleScale, scaleY = circleScale),
                    shape = CircleShape, color = containerColor, tonalElevation = 8.dp
                ) {
                    Crossfade(checkState, label = "IconCrossfade") { s ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            when(s) {
                                2 -> Icon(
                                    imageVector = if (isRooted) Icons.Rounded.Check else Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = Color.White
                                )
                                else -> Icon(
                                    painterResource(R.drawable.root_hash),
                                    null,
                                    Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = when(checkState) {
                    1 -> "Interrogating SU Binaries..."
                    2 -> if (isRooted) "Your Device is Rooted" else "Root Access not Available"
                    else -> "Ready to verify?"
                },
                modifier = Modifier.padding(top = 32.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    checkState = 1
                    scope.launch(Dispatchers.IO) {
                        delay(1500)
                        val result = checkRoot()
                        withContext(Dispatchers.Main) {
                            isRooted = result
                            checkState = 2
                            saveLog(context, result)
                        }
                    }
                },
                modifier = Modifier.height(64.dp).fillMaxWidth(0.7f),
                shape = RoundedCornerShape(32.dp),
                enabled = checkState != 1
            ) {
                Text("Verify ROOT")
            }
        }
    }
}

@Composable
fun GuideScreen() {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer), // Theme-aware red
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("SECURITY WARNING", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text("Never trust 'One-Click Root' apps. Only use official open-source binaries like Magisk or KernelSU to avoid malware.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("INSTRUCTIONS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        GuideCard("1. Developer Options", "Enable 'OEM Unlocking' and 'USB Debugging' in your device settings.", Icons.Rounded.Settings)
        GuideCard("2. Unlock Bootloader", "Run 'fastboot flashing unlock' or 'fastboot oem unlock' via PC.", Icons.Rounded.LockOpen)
        GuideCard("3. Flash Root", "Patch your boot image via Magisk App and flash it via fastboot.", Icons.Rounded.FlashOn)

        Text("TRUSTED SOURCES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        LinkCard("Magisk", "https://github.com/topjohnwu/Magisk")
        LinkCard("KernelSU", "https://github.com/tiann/KernelSU")
        LinkCard("APatch", "https://github.com/bmax121/APatch")
    }
}

@Composable
fun GuideCard(t: String, d: String, i: ImageVector) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(i, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(t, fontWeight = FontWeight.Bold)
                Text(d, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SettingsScreen(dark: Boolean, onDark: (Boolean) -> Unit, dyn: Boolean, onDyn: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val appVersion = BuildConfig.VERSION_NAME

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(R.drawable.root_hash), null, Modifier.size(80.dp), MaterialTheme.colorScheme.primary)

        Row(Modifier.padding(top = 12.dp)) {
            Text("Developer: ", fontWeight = FontWeight.ExtraBold)
            Text("Chill-Astro Software", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }

        Row(Modifier.padding(top = 4.dp)) {
            Text("Version: ", fontWeight = FontWeight.ExtraBold)
            Text(appVersion, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }

        TextButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/Chill-Astro/FOSS-Root-Checker".toUri())) }
        ) {
            Icon(Icons.Rounded.Code, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("GitHub Repo")
        }

        HorizontalDivider(Modifier.padding(vertical = 24.dp))

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PREFERENCES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            ListItem(
                headlineContent = { Text("App Theme") },
                leadingContent = { Icon(Icons.Rounded.Brush, null) },
                trailingContent = { Switch(checked = dark, onCheckedChange = { onDark(it) }) }
            )
            if (Build.VERSION.SDK_INT >= 31) {
                ListItem(
                    headlineContent = { Text("System Colours") },
                    leadingContent = { Icon(Icons.Rounded.Palette, null) },
                    trailingContent = { Switch(dyn, onDyn) }
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text("FOSS ROOT CHECKER", modifier = Modifier.alpha(0.4f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun HistorySheet(onDismiss: () -> Unit, state: SheetState) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(getLogs(context)) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
                if (logs.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { clearLogs(context); logs = emptyList() },
                        colors = ButtonDefaults.filledTonalButtonColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, null, Modifier.size(20.dp))
                        Text(" Clear")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            if (logs.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Clean Slate", modifier = Modifier.alpha(0.4f))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(logs) { log ->
                        val p = log.split("|")
                        if (p.size >= 5) HistoryItem(p[0] == "OK", p[1], p[2], p[3], p[4])
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(ok: Boolean, time: String, model: String, ver: String, id: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), shape = CircleShape, color = if (ok) Color(0xFF4CAF50) else Color(0xFFB00020)) {
                Icon(if (ok) Icons.Rounded.Check else Icons.Rounded.Close, null, Modifier.padding(8.dp), Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(if (ok) "Root Verified" else "Root Not Found", fontWeight = FontWeight.Bold)
                Text("$time • $model (Android $ver)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun LinkCard(t: String, url: String) {
    val ctx = LocalContext.current
    OutlinedCard(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t, Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp))
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Checker", Icons.Rounded.Tag),
    GUIDE("Guide", Icons.AutoMirrored.Rounded.MenuBook),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

fun checkRoot(): Boolean = try {
    Runtime.getRuntime().exec(arrayOf("su", "-c", "id")).inputStream.bufferedReader().readLine()?.contains("uid=0") == true
} catch (_: Exception) {
    arrayOf("/system/xbin/su", "/system/bin/su", "/sbin/su").any { java.io.File(it).exists() }
}

fun saveLog(c: Context, r: Boolean) {
    val p = c.getSharedPreferences("su_logs", Context.MODE_PRIVATE)
    val t = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
    val entry = "${if (r) "OK" else "NO"}|$t|${Build.MODEL}|${Build.VERSION.RELEASE}|${Build.ID}"
    val set = p.getStringSet("logs", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    set.add("${System.currentTimeMillis()}_$entry")
    p.edit { putStringSet("logs", set) }
}

fun getLogs(c: Context): List<String> = c.getSharedPreferences("su_logs", Context.MODE_PRIVATE)
    .getStringSet("logs", emptySet())?.toList()?.sortedByDescending { it.substringBefore("_") }?.map { it.substringAfter("_") } ?: emptyList()

fun clearLogs(c: Context) = c.getSharedPreferences("su_logs", Context.MODE_PRIVATE).edit { remove("logs") }
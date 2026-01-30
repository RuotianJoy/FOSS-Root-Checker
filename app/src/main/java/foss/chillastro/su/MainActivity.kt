@file:OptIn(ExperimentalMaterial3Api::class)

package foss.chillastro.su

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import foss.chillastro.su.ui.theme.FOSSRootCheckerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemTheme = isSystemInDarkTheme()
            var isDarkMode by rememberSaveable { mutableStateOf(systemTheme) }
            // NEW: Use Dynamic Color State (Material You)
            var useDynamicColors by rememberSaveable { mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) }

            // Ensure the theme uses dynamic colors if enabled
            FOSSRootCheckerTheme(
                darkTheme = isDarkMode,
                dynamicColor = useDynamicColors
            ) {
                FOSSRootCheckerApp(
                    isDarkMode = isDarkMode,
                    onThemeChange = { isDarkMode = it },
                    useDynamicColors = useDynamicColors,
                    onDynamicChange = { useDynamicColors = it }
                )
            }
        }
    }
}

@Composable
fun FOSSRootCheckerApp(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    useDynamicColors: Boolean,
    onDynamicChange: (Boolean) -> Unit
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    BackHandler(enabled = currentDestination != AppDestinations.HOME) {
        currentDestination = AppDestinations.HOME
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        when (val icon = destination.iconRes) {
                            is ImageVector -> Icon(icon, contentDescription = null)
                            is Int -> Icon(painterResource(id = icon), contentDescription = null)
                        }
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader()
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxHeight()) {
                        when (currentDestination) {
                            AppDestinations.HOME -> CheckerScreen()
                            AppDestinations.GUIDE -> GuideScreen()
                            AppDestinations.SETTINGS -> SettingsScreen(isDarkMode, onThemeChange, useDynamicColors, onDynamicChange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppHeader() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                buildAnnotatedString {
                    append("ROOT CHECKER ")
                    withStyle(style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )) {
                        append("[ FOSS ]")
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 2.sp
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun CheckerScreen() {
    var checkState by remember { mutableIntStateOf(0) }
    var isRooted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            if (checkState == 1) {
                CircularProgressIndicator(
                    modifier = Modifier.size(240.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
            }

            Surface(
                modifier = Modifier.size(180.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 6.dp
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.root_hash),
                    contentDescription = null,
                    modifier = Modifier.padding(45.dp),
                    tint = when {
                        checkState == 2 && isRooted -> Color(0xFF4CAF50)
                        checkState == 2 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = when(checkState) {
                1 -> "Interrogating Binary..."
                2 -> if (isRooted) "DEVICE IS ROOTED" else "ACCESS DENIED"
                else -> "Ready to scan"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = if (checkState == 2 && isRooted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                checkState = 1
                scope.launch(Dispatchers.IO) {
                    delay(1200)
                    val result = checkSuBinary()
                    withContext(Dispatchers.Main) {
                        isRooted = result
                        checkState = 2
                    }
                }
            },
            modifier = Modifier.height(56.dp).width(220.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = checkState != 1
        ) {
            Text("SCAN SYSTEM", letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(1.2f))
    }
}

@Composable
fun GuideScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SAFE ROOTING METHODS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

        GuideCard("Magisk", "Systemless root patching your boot image. Compatible with most Android 5.0+ devices.", MaterialTheme.colorScheme.primaryContainer)
        GuideCard("KernelSU", "Root at the kernel level. Stealthy, efficient, and bypasses many app detections.", MaterialTheme.colorScheme.secondaryContainer)
        GuideCard("APatch", "Modern alternative that patches the kernel without requiring GKI support.", MaterialTheme.colorScheme.tertiaryContainer)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // WARNING SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("CRITICAL WARNINGS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(Modifier.height(8.dp))
                Text("• NEVER use 'One-Click Root' apps like KingRoot or KingoRoot. They often contain spyware and use unpatched vulnerabilities.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.height(4.dp))
                Text("• Avoid unauthorized sources. Only download binaries from official GitHub repositories of Magisk, KernelSU, or APatch.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
fun GuideCard(title: String, desc: String, color: Color) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    useDynamicColors: Boolean,
    onDynamicChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("PREFERENCES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        ListItem(
            headlineContent = { Text("Dark Theme") },
            trailingContent = { Switch(checked = isDarkMode, onCheckedChange = onThemeChange) }
        )
        // NEW: Dynamic Color Toggle (only shows on Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ListItem(
                headlineContent = { Text("Use System Colors") },
                supportingContent = { Text("Match app colors with your wallpaper") },
                trailingContent = { Switch(checked = useDynamicColors, onCheckedChange = onDynamicChange) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("ABOUT DEVELOPER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Developer: Chill-Astro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                val annotatedLink = buildAnnotatedString {
                    append("Visit the Project: ")
                    pushStringAnnotation(tag = "URL", annotation = "https://github.com/chill-astro/FOSS-Root-Checker")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append("GitHub Repository")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedLink,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    onClick = { offset ->
                        annotatedLink.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}

enum class AppDestinations(val label: String, val iconRes: Any) {
    HOME("Checker", R.drawable.root_hash),
    GUIDE("Guide", R.drawable.root_guide),
    SETTINGS("Settings", Icons.Default.Settings)
}

fun checkSuBinary(): Boolean {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        val output = process.inputStream.bufferedReader().readLine()
        output != null && output.contains("uid=0")
    } catch (_: Exception) {
        val paths = arrayOf("/system/xbin/su", "/system/bin/su", "/sbin/su", "/data/local/xbin/su", "/su/bin/su")
        paths.any { java.io.File(it).exists() }
    }
}
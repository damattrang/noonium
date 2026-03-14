package ncatt.noonium

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ncatt.noonium.ui.theme.nooniumTheme
import ncatt.noonium.ui.theme.Theme
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sharedPref = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            val theme = remember { mutableStateOf(Theme.valueOf(sharedPref.getString("theme", "SYSTEM") ?: "SYSTEM")) }
            val language = remember { mutableStateOf(sharedPref.getString("language", "en") ?: "en") }
            var rootGranted by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                rootGranted = checkRootAccess()
            }

            CompositionLocalProvider(LocalContext provides createLocaleContext(language.value)) {
                nooniumTheme(theme = theme.value) {
                    nooniumApp(theme, language, rootGranted)
                }
            }
        }
    }

    private suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su -c id")
            val output = process.inputStream.bufferedReader().use { it.readLine() }
            process.waitFor()
            output != null && output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
fun createLocaleContext(language: String): Context {
    val context = LocalContext.current
    val locale = Locale(language)
    Locale.setDefault(locale)
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(locale)
    return context.createConfigurationContext(configuration)
}

@Composable
fun nooniumApp(theme: MutableState<Theme>, language: MutableState<String>, isRootGranted: Boolean) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val isDark = isDark(theme.value)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Floating Bottom Navigation Bar (One UI 8 Style Concept)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = if (isDark) Color(0xFF2D2F31).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(72.dp)
                    ) {
                        AppDestinations.entries.forEach { destination ->
                            if (!destination.requiresRoot || isRootGranted) {
                                val isSelected = destination == currentDestination
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentDestination = destination },
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = stringResource(id = destination.label),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(id = destination.label),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = Color.Transparent, // Clean floating look
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(theme.value)
                AppDestinations.TWEAKS -> TweaksScreen(theme.value)
                AppDestinations.MORE -> MoreScreen(theme.value)
                AppDestinations.SETTINGS -> SettingsScreen(theme = theme, language = language)
            }
        }
    }
}

@Composable
fun BrandingTitle() {
    Text(
        text = "noonium",
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun HomeScreen(theme: Theme) {
    val isDark = isDark(theme)
    val cardContainerColor = if (isDark) Color(0xFF1A1C1E) else Color(0xFFE8EAF6)
    val headerIconBoxColor = if (isDark) Color(0xFF303440) else Color(0xFFC5CAE9)
    val headerIconColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF3F51B5)
    val textColor = if (isDark) Color.White else Color.Black
    val innerCardColor = if (isDark) Color(0xFF24292E) else Color.White
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.4f)
    val versionTextColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BrandingTitle()
        Text(stringResource(id = R.string.home), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(id = R.string.system_information), fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = cardContainerColor)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(headerIconBoxColor), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.GridView, contentDescription = null, tint = headerIconColor, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(id = R.string.rom_information), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("${Build.MANUFACTURER} ${Build.MODEL}", fontSize = 15.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
                    }
                }
                Row(modifier = Modifier.padding(start = 24.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = versionTextColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.android_version, Build.VERSION.RELEASE), color = versionTextColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("  |  ", color = versionTextColor.copy(alpha = 0.4f), fontSize = 15.sp)
                    Text(stringResource(id = R.string.api_level, Build.VERSION.SDK_INT), color = versionTextColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = innerCardColor)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        DeviceInfoItem(Icons.Outlined.Verified, stringResource(id = R.string.system_version), Build.DISPLAY, if (isDark) Color(0xFF4FC3F7) else Color(0xFF2196F3), textColor)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 56.dp), color = dividerColor)
                        DeviceInfoItem(Icons.Outlined.History, stringResource(id = R.string.rom_version), Build.ID, if (isDark) Color(0xFFB39DDB) else Color(0xFF7E57C2), textColor)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 56.dp), color = dividerColor)
                        DeviceInfoItem(Icons.Outlined.Fingerprint, stringResource(id = R.string.fingerprint), Build.FINGERPRINT, if (isDark) Color(0xFF9FA8DA) else Color(0xFF5C6BC0), textColor)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 56.dp), color = dividerColor)
                        DeviceInfoItem(Icons.Outlined.Code, stringResource(id = R.string.kernel_version_label), System.getProperty("os.version") ?: "unknown", if (isDark) Color(0xFFB39DDB) else Color(0xFF9575CD), textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun TweaksScreen(theme: Theme) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("tweaks_prefs", Context.MODE_PRIVATE) }
    
    var gpuGovernor by remember { mutableStateOf(sharedPref.getString("gpu_gov", "msm-adreno-tz") ?: "msm-adreno-tz") }
    var cpuOverclock by remember { mutableStateOf(sharedPref.getBoolean("cpu_oc", false)) }
    var gpuOverclock by remember { mutableStateOf(sharedPref.getBoolean("gpu_oc", false)) }
    var revancedYoutube by remember { mutableStateOf(sharedPref.getBoolean("revanced_yt", false)) }
    var revancedYtMusic by remember { mutableStateOf(sharedPref.getBoolean("revanced_ytm", false)) }
    
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 80.dp)) {
        BrandingTitle()
        Text(stringResource(id = R.string.tweaks), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(id = R.string.gpu_governor), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box {
                    Row(modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(gpuGovernor, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("performance", "powersave", "msm-adreno-tz", "simple_ondemand").forEach { gov ->
                            DropdownMenuItem(text = { Text(gov) }, onClick = { 
                                gpuGovernor = gov
                                sharedPref.edit().putString("gpu_gov", gov).apply()
                                expanded = false 
                            })
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TweakSwitch(Icons.Outlined.Speed, stringResource(id = R.string.overclock_cpu), cpuOverclock, MaterialTheme.colorScheme.primary) { 
                    cpuOverclock = it
                    sharedPref.edit().putBoolean("cpu_oc", it).apply()
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TweakSwitch(Icons.Outlined.Speed, stringResource(id = R.string.overclock_gpu), gpuOverclock, MaterialTheme.colorScheme.secondary) {
                    gpuOverclock = it
                    sharedPref.edit().putBoolean("gpu_oc", it).apply()
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TweakSwitch(Icons.Outlined.PlayCircleOutline, stringResource(id = R.string.revanced_youtube), revancedYoutube, Color(0xFFFF0000)) {
                    revancedYoutube = it
                    sharedPref.edit().putBoolean("revanced_yt", it).apply()
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TweakSwitch(Icons.Outlined.MusicNote, stringResource(id = R.string.revanced_yt_music), revancedYtMusic, Color(0xFFFF0000)) {
                    revancedYtMusic = it
                    sharedPref.edit().putBoolean("revanced_ytm", it).apply()
                }
            }
        }
    }
}

@Composable
fun TweakSwitch(icon: ImageVector, label: String, checked: Boolean, tint: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun MoreScreen(theme: Theme) {
    val context = LocalContext.current
    val isDark = isDark(theme)
    val textColor = if (isDark) Color.White else Color.Black
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.4f)
    
    var uptimeMillis by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var deepSleepMillis by remember { mutableLongStateOf(SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            uptimeMillis = SystemClock.elapsedRealtime()
            deepSleepMillis = SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()
            delay(1000)
        }
    }
    
    val uptime = String.format(Locale.getDefault(), "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(uptimeMillis),
        TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60,
        TimeUnit.MILLISECONDS.toSeconds(uptimeMillis) % 60)
    
    val deepSleep = String.format(Locale.getDefault(), "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(deepSleepMillis),
        TimeUnit.MILLISECONDS.toMinutes(deepSleepMillis) % 60,
        TimeUnit.MILLISECONDS.toSeconds(deepSleepMillis) % 60)

    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val appsCount = context.packageManager.getInstalledApplications(0).size

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 80.dp)) {
        BrandingTitle()
        Text(stringResource(id = R.string.more), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                MoreInfoItem(Icons.Outlined.BatteryFull, stringResource(id = R.string.battery_info), "$batteryLevel%", Color(0xFF4CAF50), textColor)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)
                MoreInfoItem(Icons.Outlined.Layers, stringResource(id = R.string.apps_count), appsCount.toString(), Color(0xFF2196F3), textColor)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)
                MoreInfoItem(Icons.Outlined.Memory, stringResource(id = R.string.display_chipset), Build.HARDWARE.uppercase(Locale.getDefault()), Color(0xFFFF9800), textColor)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)
                MoreInfoItem(Icons.Outlined.Devices, stringResource(id = R.string.device_name_label), Build.MODEL, Color(0xFF9C27B0), textColor)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)
                MoreInfoItem(Icons.Outlined.Timer, stringResource(id = R.string.uptime), uptime, Color(0xFF00BCD4), textColor)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)
                MoreInfoItem(Icons.Outlined.Timer, stringResource(id = R.string.deep_sleep), deepSleep, Color(0xFF607D8B), textColor)
            }
        }
    }
}

@Composable
fun MoreInfoItem(icon: ImageVector, label: String, value: String, iconColor: Color, textColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconColor)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, fontSize = 13.sp, color = Color.Gray)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun DeviceInfoItem(icon: ImageVector, label: String, value: String, iconColor: Color, textColor: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconColor)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, fontSize = 13.sp, color = Color.Gray)
            Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, theme: MutableState<Theme>, language: MutableState<String>) {
    val context = LocalContext.current
    val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    var themeExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    val sharedPref = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    Column(modifier = modifier.fillMaxSize().padding(24.dp).padding(bottom = 80.dp)) {
        BrandingTitle()
        Text(text = stringResource(id = R.string.settings), fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(id = R.string.theme), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box {
                    Row(modifier = Modifier.fillMaxWidth().clickable { themeExpanded = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ThemeName(theme.value), modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                    DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                        Theme.entries.forEach { themeValue ->
                            DropdownMenuItem(text = { Text(ThemeName(themeValue)) }, onClick = { 
                                theme.value = themeValue
                                sharedPref.edit().putString("theme", themeValue.name).apply()
                                themeExpanded = false
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.language), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box {
                    Row(modifier = Modifier.fillMaxWidth().clickable { languageExpanded = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (language.value == "en") "English" else "Tiếng Việt", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                    DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
                        DropdownMenuItem(text = { Text("English") }, onClick = { 
                            language.value = "en"
                            sharedPref.edit().putString("language", "en").apply()
                            languageExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Tiếng Việt") }, onClick = { 
                            language.value = "vi"
                            sharedPref.edit().putString("language", "vi").apply()
                            languageExpanded = false
                        })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().clickable { 
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/V61jZBMFjUs?si=mGFYyU4Y8XE1kZ4K"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }, shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "noonium", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "ver. $versionName", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun isDark(theme: Theme): Boolean {
    return when (theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemInDarkTheme()
    }
}

@Composable
fun ThemeName(theme: Theme): String {
    return when (theme) {
        Theme.LIGHT -> stringResource(id = R.string.theme_light)
        Theme.DARK -> stringResource(id = R.string.theme_dark)
        Theme.SYSTEM -> stringResource(id = R.string.theme_system)
    }
}

enum class AppDestinations(
    @StringRes val label: Int,
    val icon: ImageVector,
    val requiresRoot: Boolean = false
) {
    HOME(R.string.home, Icons.Outlined.Home),
    TWEAKS(R.string.tweaks, Icons.Outlined.Tune, requiresRoot = true),
    MORE(R.string.more, Icons.Outlined.MoreHoriz),
    SETTINGS(R.string.settings, Icons.Outlined.ManageAccounts),
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    nooniumTheme {
        HomeScreen(Theme.SYSTEM)
    }
}

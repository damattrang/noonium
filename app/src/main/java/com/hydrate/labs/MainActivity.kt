package com.hydrate.labs

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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Shield
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.hydrate.labs.ui.theme.HydrateTheme
import com.hydrate.labs.ui.theme.Theme
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
                HydrateTheme(theme = theme.value) {
                    HydrateApp(theme, language, rootGranted)
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
    val locale = Locale.forLanguageTag(language)
    Locale.setDefault(locale)
    val configuration = Configuration(context.resources.configuration).apply {
        setLocale(locale)
    }
    return context.createConfigurationContext(configuration)
}

@Composable
fun HydrateApp(theme: MutableState<Theme>, language: MutableState<String>, isRootGranted: Boolean) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var showRebootMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Material 3 Expressive Floating Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppDestinations.entries.forEach { destination ->
                            if (!destination.requiresRoot || isRootGranted) {
                                val isSelected = destination == currentDestination
                                
                                if (destination == AppDestinations.REBOOT) {
                                    Box {
                                        NavBarItem(
                                            destination = destination,
                                            isSelected = false,
                                            onClick = { showRebootMenu = true }
                                        )
                                        RebootDropdownMenu(
                                            expanded = showRebootMenu,
                                            onDismissRequest = { showRebootMenu = false }
                                        )
                                    }
                                } else {
                                    NavBarItem(
                                        destination = destination,
                                        isSelected = isSelected,
                                        onClick = { currentDestination = destination }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(500, delayMillis = 100)) + 
                     slideInVertically(initialOffsetY = { it / 10 }, animationSpec = spring(stiffness = Spring.StiffnessLow)))
                    .togetherWith(fadeOut(animationSpec = tween(300)))
                },
                label = "ScreenTransition"
            ) { targetDestination ->
                when (targetDestination) {
                    AppDestinations.HOME -> HomeScreen()
                    AppDestinations.TWEAKS -> TweaksScreen()
                    AppDestinations.MORE -> MoreScreen()
                    AppDestinations.SETTINGS -> SettingsScreen(theme = theme, language = language)
                    AppDestinations.REBOOT -> HomeScreen() // Fallback
                }
            }
        }
    }
}

@Composable
fun NavBarItem(destination: AppDestinations, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "BgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "ContentColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "Scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = if (isSelected) 24.dp else 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = stringResource(id = destination.label),
                modifier = Modifier.size(26.dp),
                tint = contentColor
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f)
            ) {
                Row {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = destination.label),
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun RebootDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraLarge = RoundedCornerShape(32.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(8.dp)
        ) {
            val options = listOf(
                Pair(R.string.reboot_system, ""),
                Pair(R.string.reboot_recovery, "recovery"),
                Pair(R.string.reboot_bootloader, "bootloader"),
                Pair(R.string.reboot_fastboot, "fastboot"),
                Pair(R.string.reboot_edl, "edl"),
                Pair(R.string.reboot_download, "download")
            )
            
            options.forEach { (labelRes, command) ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            stringResource(id = labelRes), 
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ) 
                    },
                    onClick = {
                        rebootDevice(command)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}

private fun rebootDevice(reason: String) {
    try {
        val cmd = if (reason.isEmpty()) "reboot" else "reboot $reason"
        Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
    } catch (e: Exception) {
        // Handle error
    }
}

@Composable
fun BrandingTitle() {
    Text(
        text = "HYDRATE LABS",
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 4.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun HomeScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BrandingTitle()
        Text(
            text = stringResource(id = R.string.home),
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 52.sp
        )
        Text(
            text = stringResource(id = R.string.system_information),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(40.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.GridView,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column {
                        Text(
                            stringResource(id = R.string.rom_information),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${Build.MANUFACTURER} ${Build.MODEL}",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.android_version, Build.VERSION.RELEASE), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            stringResource(id = R.string.api_level, Build.VERSION.SDK_INT),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        DeviceInfoItemExpressive(Icons.Outlined.Verified, stringResource(id = R.string.system_version), Build.DISPLAY, MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DeviceInfoItemExpressive(Icons.Outlined.History, stringResource(id = R.string.rom_version), Build.ID, MaterialTheme.colorScheme.secondary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DeviceInfoItemExpressive(Icons.Outlined.Fingerprint, stringResource(id = R.string.fingerprint), Build.FINGERPRINT, MaterialTheme.colorScheme.tertiary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DeviceInfoItemExpressive(Icons.Outlined.Code, stringResource(id = R.string.kernel_version_label), System.getProperty("os.version") ?: "unknown", MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun TweaksScreen() {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("tweaks_prefs", Context.MODE_PRIVATE) }
    
    var gpuGovernor by remember { mutableStateOf(sharedPref.getString("gpu_gov", "msm-adreno-tz") ?: "msm-adreno-tz") }
    var cpuOverclock by remember { mutableStateOf(sharedPref.getBoolean("cpu_oc", false)) }
    var gpuOverclock by remember { mutableStateOf(sharedPref.getBoolean("gpu_oc", false)) }
    var revancedYoutube by remember { mutableStateOf(sharedPref.getBoolean("revanced_yt", false)) }
    var revancedYtMusic by remember { mutableStateOf(sharedPref.getBoolean("revanced_ytm", false)) }
    
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 120.dp)) {
        BrandingTitle()
        Text(stringResource(id = R.string.tweaks), fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(40.dp)) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(stringResource(id = R.string.gpu_governor), fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(gpuGovernor, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("performance", "powersave", "msm-adreno-tz", "simple_ondemand").forEach { gov ->
                            DropdownMenuItem(text = { Text(gov, fontWeight = FontWeight.Medium) }, onClick = { 
                                gpuGovernor = gov
                                sharedPref.edit { putString("gpu_gov", gov) }
                                expanded = false 
                            })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                TweakSwitchExpressive(Icons.Outlined.Speed, stringResource(id = R.string.overclock_cpu), cpuOverclock) { 
                    cpuOverclock = it
                    sharedPref.edit { putBoolean("cpu_oc", it) }
                }
                TweakSwitchExpressive(Icons.Outlined.Speed, stringResource(id = R.string.overclock_gpu), gpuOverclock) {
                    gpuOverclock = it
                    sharedPref.edit { putBoolean("gpu_oc", it) }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("APP TWEAKS", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                // ReVanced tweaks with improved layout (Switch below for long text)
                TweakSwitchTile(Icons.Outlined.PlayCircleOutline, stringResource(id = R.string.revanced_youtube), revancedYoutube) {
                    revancedYoutube = it
                    sharedPref.edit { putBoolean("revanced_yt", it) }
                }
                TweakSwitchTile(Icons.Outlined.MusicNote, stringResource(id = R.string.revanced_yt_music), revancedYtMusic) {
                    revancedYtMusic = it
                    sharedPref.edit { putBoolean("revanced_ytm", it) }
                }
            }
        }
    }
}

@Composable
fun TweakSwitchExpressive(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun TweakSwitchTile(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
        }
    }
}

@Composable
fun MoreScreen() {
    val context = LocalContext.current
    
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

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 120.dp)) {
        BrandingTitle()
        Text(stringResource(id = R.string.more), fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(40.dp)) {
            Column(modifier = Modifier.padding(28.dp)) {
                MoreInfoItemExpressive(Icons.Outlined.BatteryFull, stringResource(id = R.string.battery_info), "$batteryLevel%", MaterialTheme.colorScheme.primary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MoreInfoItemExpressive(Icons.Outlined.Layers, stringResource(id = R.string.apps_count), appsCount.toString(), MaterialTheme.colorScheme.secondary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MoreInfoItemExpressive(Icons.Outlined.Memory, stringResource(id = R.string.display_chipset), Build.HARDWARE.uppercase(Locale.getDefault()), MaterialTheme.colorScheme.tertiary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MoreInfoItemExpressive(Icons.Outlined.Devices, stringResource(id = R.string.device_name_label), Build.MODEL, MaterialTheme.colorScheme.error)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MoreInfoItemExpressive(Icons.Outlined.Timer, stringResource(id = R.string.uptime), uptime, MaterialTheme.colorScheme.primary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MoreInfoItemExpressive(Icons.Outlined.Timer, stringResource(id = R.string.deep_sleep), deepSleep, MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun MoreInfoItemExpressive(icon: ImageVector, label: String, value: String, accentColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(18.dp),
            color = accentColor.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = accentColor)
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun DeviceInfoItemExpressive(icon: ImageVector, label: String, value: String, accentColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = accentColor)
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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

    Column(modifier = modifier.fillMaxSize().padding(24.dp).padding(bottom = 120.dp)) {
        BrandingTitle()
        Text(text = stringResource(id = R.string.settings), fontSize = 48.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 20.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(40.dp)) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(stringResource(id = R.string.theme), fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    onClick = { themeExpanded = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(themeName(theme.value), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                    DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                        Theme.entries.forEach { themeValue ->
                            DropdownMenuItem(text = { Text(themeName(themeValue), fontWeight = FontWeight.Medium) }, onClick = { 
                                theme.value = themeValue
                                sharedPref.edit { putString("theme", themeValue.name) }
                                themeExpanded = false
                            })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Text(stringResource(id = R.string.language), fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    onClick = { languageExpanded = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (language.value == "en") "English" else "Tiếng Việt", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                    DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
                        DropdownMenuItem(text = { Text("English", fontWeight = FontWeight.Medium) }, onClick = { 
                            language.value = "en"
                            sharedPref.edit { putString("language", "en") }
                            languageExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Tiếng Việt", fontWeight = FontWeight.Medium) }, onClick = { 
                            language.value = "vi"
                            sharedPref.edit { putString("language", "vi") }
                            languageExpanded = false
                        })
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().clickable { 
                val intent = Intent(Intent.ACTION_VIEW, "https://youtu.be/V61jZBMFjUs?si=mGFYyU4Y8XE1kZ4K".toUri())
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }, 
            shape = RoundedCornerShape(40.dp), 
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.padding(28.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text(text = "HydrateLabs", fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = "Version $versionName", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun themeName(theme: Theme): String {
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
    TWEAKS(R.string.tweaks, Icons.Outlined.Extension, requiresRoot = true),
    REBOOT(R.string.reboot, Icons.Outlined.RestartAlt, requiresRoot = true),
    MORE(R.string.more, Icons.Outlined.Shield),
    SETTINGS(R.string.settings, Icons.Outlined.Tune),
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HydrateTheme {
        HomeScreen()
    }
}

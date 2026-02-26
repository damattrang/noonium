package ncatt.noonium

import android.content.Context
import android.content.Context.TELEPHONY_SERVICE
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ncatt.noonium.ui.theme.nooniumTheme
import ncatt.noonium.ui.theme.Theme
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sharedPref = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            val theme = remember { mutableStateOf(Theme.valueOf(sharedPref.getString("theme", "SYSTEM") ?: "SYSTEM")) }
            val language = remember { mutableStateOf(sharedPref.getString("language", "en") ?: "en") }

            CompositionLocalProvider(LocalContext provides createLocaleContext(language.value)) {
                nooniumTheme(theme = theme.value) {
                    nooniumApp(theme, language)
                }
            }
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
fun nooniumApp(theme: MutableState<Theme>, language: MutableState<String>) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = { Icon(destination.icon, contentDescription = stringResource(id = destination.label)) },
                    label = { Text(stringResource(id = destination.label)) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.SETTINGS -> SettingsScreen(modifier = Modifier.padding(innerPadding), theme, language)
            }
        }
    }
}

@Composable
fun KernelStatus() {
    val kernelVersion = System.getProperty("os.version") ?: "universe"

    val (text, color, textColor) = when {
        kernelVersion.contains("moonstone", ignoreCase = true) -> Triple("moonstone", Color.DarkGray, Color.White)
        kernelVersion.contains("rc1-vigus", ignoreCase = true) -> Triple("vigus", Color.DarkGray, Color.White)
        kernelVersion.contains("yuki", ignoreCase = true) -> Triple("yuki", Color.DarkGray, Color.White)
        kernelVersion.contains("vampire", ignoreCase = true) -> Triple("vampire", Color.DarkGray, Color.White)
        kernelVersion.contains("moonlight", ignoreCase = true) -> Triple("Moonlight", Color.DarkGray, Color.White)
        kernelVersion.contains("helium", ignoreCase = true) -> Triple("h*", Color(0xFF42A5F5), Color.White)
        kernelVersion.contains("experience", ignoreCase = true) -> Triple("exp", Color(0xFFFFCA28), Color.Black)
        else -> Triple("universe", Color.DarkGray, Color.White)
    }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.kernel_status), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
    val simProvider = telephonyManager.simOperatorName

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "noonium",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Left
        )
        KernelStatus()
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.device_information),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DeviceInfoRow(stringResource(id = R.string.device_name), Build.DEVICE)
                DeviceInfoRow(stringResource(id = R.string.android_version), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                DeviceInfoRow(stringResource(id = R.string.model), Build.MODEL)
                DeviceInfoRow(stringResource(id = R.string.manufacturer), Build.MANUFACTURER)
                DeviceInfoRow(stringResource(id = R.string.build_number), Build.DISPLAY)
                DeviceInfoRow(stringResource(id = R.string.security_patch), Build.VERSION.SECURITY_PATCH)
                DeviceInfoRow(stringResource(id = R.string.kernel_version), System.getProperty("os.version"))
                DeviceInfoRow(stringResource(id = R.string.esim_provider), simProvider)
                DeviceInfoRow(stringResource(id = R.string.fingerprint), Build.FINGERPRINT)
            }
        }
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String?) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 16.dp))
        Text(text = value ?: "Unknown", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
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

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, theme: MutableState<Theme>, language: MutableState<String>) {
    val context = LocalContext.current
    val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    var themeExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    val sharedPref = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(
            text = stringResource(id = R.string.settings),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(id = R.string.theme), fontWeight = FontWeight.Bold)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themeExpanded = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                Text(stringResource(id = R.string.language), fontWeight = FontWeight.Bold)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { languageExpanded = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/V61jZBMFjUs?si=mGFYyU4Y8XE1kZ4K"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "noonium", fontWeight = FontWeight.Bold)
                Text(text = "ver. $versionName", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


enum class AppDestinations(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.home, Icons.Outlined.Home),
    SETTINGS(R.string.settings, Icons.Outlined.Settings),
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    nooniumTheme {
        HomeScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val theme = remember { mutableStateOf(Theme.SYSTEM) }
    val language = remember { mutableStateOf("en") }
    nooniumTheme(theme = theme.value) {
        SettingsScreen(theme = theme, language = language)
    }
}

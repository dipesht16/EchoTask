package com.example.smartvoicemanager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartvoicemanager.ui.home.HomeScreen
import com.example.smartvoicemanager.ui.settings.SettingsScreen
import com.example.smartvoicemanager.ui.settings.SettingsViewModel
import com.example.smartvoicemanager.ui.task.AddEditTaskScreen
import com.example.smartvoicemanager.ui.tasks.TasksScreen
import com.example.smartvoicemanager.ui.theme.SmartVoiceManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        // Track if the splash screen has been shown in the current process
        private var hasShownSplash = false
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        
        // If recreating (e.g. language change), immediately hide the system splash
        if (savedInstanceState != null || hasShownSplash) {
            splashScreen.setKeepOnScreenCondition { false }
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestRequiredPermissions()
        checkBatteryOptimizations()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isDarkThemePref by settingsViewModel.isDarkTheme.collectAsState()
            val language by settingsViewModel.language.collectAsState()
            
            var showSplash by remember { mutableStateOf(!hasShownSplash) }
            var isChangingLanguage by remember { mutableStateOf(false) }
            
            // Flag to track if the initial language sync has already happened
            var isInitialized by remember { mutableStateOf(false) }

            // Splash Screen Timer (Only runs on cold start)
            if (!hasShownSplash) {
                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                    hasShownSplash = true
                }
            }

            // Reliable Language Change Observer
            LaunchedEffect(language) {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
                if (AppCompatDelegate.getApplicationLocales() != appLocale) {
                    // Only show loading UI if it's a user action (not the very first app launch)
                    if (isInitialized && hasShownSplash) {
                        isChangingLanguage = true
                        delay(800) // Give user time to see the circle
                    }
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
                isInitialized = true
            }

            SmartVoiceManagerTheme(darkTheme = isDarkThemePref) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        FullScreenSplash()
                    } else {
                        MainAppContent()
                    }

                    // Loading overlay for language switching
                    if (isChangingLanguage && !showSplash) {
                        LanguageLoadingOverlay()
                    }
                }
            }
        }
    }

    @Composable
    fun LanguageLoadingOverlay() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Updating Language...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    @Composable
    fun FullScreenSplash() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FF)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }

    @Composable
    fun MainAppContent() {
        val navController = rememberNavController()
        val items = listOf(Screen.Home, Screen.Tasks, Screen.Settings)
        
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                if (items.any { it.route == currentDestination?.route }) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(stringResource(screen.resourceId)) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController, 
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(onAddTaskClick = { navController.navigate("add_task") })
                }
                composable(Screen.Tasks.route) { TasksScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable("add_task") {
                    AddEditTaskScreen(
                        onBackClick = { navController.popBackStack() },
                        onSaveSuccess = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun checkBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                // startActivity(intent)
            }
        }
    }
}

sealed class Screen(val route: String, val resourceId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", R.string.home, Icons.Default.Home)
    object Tasks : Screen("tasks", R.string.tasks, Icons.Default.List)
    object Settings : Screen("settings", R.string.settings, Icons.Default.Settings)
}

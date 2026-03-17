package com.gesturecontrol

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gesturecontrol.GestureActionMap
import com.gesturecontrol.ui.screens.GestureActionMappingScreen
import com.gesturecontrol.ui.screens.GestureRecorderScreen
import com.gesturecontrol.ui.screens.HomeScreen
import com.gesturecontrol.ui.screens.SettingsScreen
import com.gesturecontrol.ui.screens.TestScreen
import com.gesturecontrol.ui.theme.*

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        GestureActionMap.init(this)

        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())

        setContent {
            GestureControlTheme {
                GestureControlApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String) {
    object Home     : Screen("home",     "Home")
    object Test     : Screen("test",     "Test")
    object Record   : Screen("record",   "Record")
    object Mapping  : Screen("mapping",  "Actions")
    object Settings : Screen("settings", "Settings")
}

@Composable
fun GestureControlApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(Screen.Home, Screen.Test, Screen.Record, Screen.Mapping, Screen.Settings)
    val icons = mapOf(
        Screen.Home     to Icons.Default.Home,
        Screen.Test     to Icons.Default.Visibility,
        Screen.Record   to Icons.Default.FitnessCenter,
        Screen.Mapping  to Icons.Default.Gamepad,
        Screen.Settings to Icons.Default.Settings
    )

    Scaffold(
        containerColor = NavyDeep,
        bottomBar = {
            NavigationBar(
                containerColor = NavyDark,
                tonalElevation = 0.dp
            ) {
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(icons[screen]!!, contentDescription = screen.label,
                                tint = if (selected) CyanBright else TextMuted)
                        },
                        label = {
                            Text(screen.label,
                                color = if (selected) CyanBright else TextMuted,
                                style = MaterialTheme.typography.labelSmall)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = CyanDim.copy(alpha = 0.25f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            }
        ) {
            composable(Screen.Home.route)     { HomeScreen() }
            composable(Screen.Test.route)     { TestScreen() }
            composable(Screen.Record.route)   { GestureRecorderScreen() }
            composable(Screen.Mapping.route)  { GestureActionMappingScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

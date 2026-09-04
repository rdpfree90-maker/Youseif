package com.youseif.playerpro.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.youseif.playerpro.R
import com.youseif.playerpro.data.model.Source
import com.youseif.playerpro.ui.screens.LibraryScreen
import com.youseif.playerpro.ui.screens.PlayerScreen
import com.youseif.playerpro.ui.screens.SettingsScreen
import com.youseif.playerpro.ui.screens.QuickPlayScreen

object Routes {
    const val LIBRARY = "library"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
    const val QUICK_PLAY = "quick_play"
}

@Composable
fun AppNavigation(
    onLanguageChanged: (String) -> Unit
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.LIBRARY

    // Hold pending source/url for player
    var pendingSource by remember { mutableStateOf<Source?>(null) }
    var pendingUrl by remember { mutableStateOf<String?>(null) }

    val showBottomBar = currentRoute in listOf(Routes.LIBRARY, Routes.QUICK_PLAY, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                // Fixed order: Library | Player | Settings — NEVER flip with Arabic RTL
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.LIBRARY,
                        onClick = {
                            navController.navigate(Routes.LIBRARY) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == Routes.LIBRARY) Icons.Filled.LibraryMusic
                                else Icons.Outlined.LibraryMusic,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.library)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.QUICK_PLAY || currentRoute == Routes.PLAYER,
                        onClick = {
                            navController.navigate(Routes.QUICK_PLAY) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == Routes.QUICK_PLAY || currentRoute == Routes.PLAYER)
                                    Icons.Filled.PlayCircle
                                else Icons.Outlined.PlayCircle,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.player)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == Routes.SETTINGS) Icons.Filled.Settings
                                else Icons.Outlined.Settings,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(R.string.settings)) }
                    )
                }
                } // end LTR CompositionLocalProvider for bottom nav
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onPlay = { source ->
                        pendingSource = source
                        pendingUrl = null
                        navController.navigate(Routes.PLAYER)
                    },
                    onOpenUrl = { url ->
                        pendingUrl = url
                        pendingSource = null
                        navController.navigate(Routes.PLAYER)
                    }
                )
            }
            composable(Routes.QUICK_PLAY) {
                QuickPlayScreen(
                    onPlayUrl = { url ->
                        pendingUrl = url
                        pendingSource = null
                        navController.navigate(Routes.PLAYER)
                    }
                )
            }
            composable(Routes.PLAYER) {
                PlayerScreen(
                    initialUrl = pendingUrl,
                    initialSource = pendingSource,
                    onBack = {
                        pendingSource = null
                        pendingUrl = null
                        navController.popBackStack()
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onLanguageChanged = onLanguageChanged)
            }
        }
    }
}

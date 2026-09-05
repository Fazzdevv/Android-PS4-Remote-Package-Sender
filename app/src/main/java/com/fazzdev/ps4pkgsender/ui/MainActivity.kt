package com.fazzdev.ps4pkgsender.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fazzdev.ps4pkgsender.R
import com.fazzdev.ps4pkgsender.ui.components.ConnectionStatusBadge
import com.fazzdev.ps4pkgsender.ui.navigation.Screen
import com.fazzdev.ps4pkgsender.ui.screens.files.FilesScreen
import com.fazzdev.ps4pkgsender.ui.screens.files.FilesViewModel
import com.fazzdev.ps4pkgsender.ui.screens.queue.QueueScreen
import com.fazzdev.ps4pkgsender.ui.screens.queue.QueueViewModel
import com.fazzdev.ps4pkgsender.ui.screens.settings.SettingsScreen
import com.fazzdev.ps4pkgsender.ui.screens.settings.SettingsViewModel
import androidx.compose.runtime.CompositionLocalProvider
import com.fazzdev.ps4pkgsender.PkgSenderApp
import com.fazzdev.ps4pkgsender.ui.i18n.EnglishStrings
import com.fazzdev.ps4pkgsender.ui.i18n.IndonesianStrings
import com.fazzdev.ps4pkgsender.ui.i18n.LocalAppStrings
import com.fazzdev.ps4pkgsender.ui.theme.AppTheme
import com.fazzdev.ps4pkgsender.ui.theme.PS4PkgSenderTheme
import com.fazzdev.ps4pkgsender.ui.theme.Typography

class MainActivity : ComponentActivity() {

    private val filesViewModel: FilesViewModel by viewModels()
    private val queueViewModel: QueueViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+ (PRD 6.4)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val settingsManager = (application as PkgSenderApp).settingsManager

        setContent {
            val appTheme by settingsManager.appTheme.collectAsState()
            val appLanguage by settingsManager.appLanguage.collectAsState()
            val strings = if (appLanguage.equals("id", ignoreCase = true)) IndonesianStrings else EnglishStrings

            PS4PkgSenderTheme(themeMode = appTheme) {
                CompositionLocalProvider(LocalAppStrings provides strings) {
                    MainAppContent(
                        filesViewModel = filesViewModel,
                        queueViewModel = queueViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    filesViewModel: FilesViewModel,
    queueViewModel: QueueViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val settingsState by settingsViewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.appTitle,
                            style = Typography.titleLarge,
                            color = AppTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    ConnectionStatusBadge(
                        connectionResult = settingsState.testResult,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.background,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.textPrimary
            ) {
                Screen.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    val title = when (screen) {
                        Screen.Files -> strings.navFiles
                        Screen.Queue -> strings.navQueue
                        Screen.Settings -> strings.navSettings
                    }
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = title) },
                        label = { Text(title, style = Typography.labelSmall) },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppTheme.colors.surface,
                            selectedTextColor = AppTheme.colors.cyanAccent,
                            indicatorColor = AppTheme.colors.cyanAccent,
                            unselectedIconColor = AppTheme.colors.textMuted,
                            unselectedTextColor = AppTheme.colors.textMuted
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Files.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Files.route) {
                FilesScreen(
                    viewModel = filesViewModel,
                    onAddToQueue = { selectedFiles ->
                        queueViewModel.addFilesToQueue(selectedFiles)
                        navController.navigate(Screen.Queue.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Queue.route) {
                QueueScreen(viewModel = queueViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}

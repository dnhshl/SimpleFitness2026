package com.example.main

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.main.statemgmt.AppViewModel
import com.example.main.ui.components.PermissionWrapper
import com.example.main.ui.screens.*

/**
 * REVERSE ENGINEERING LERNEINHEIT: MAIN ACTIVITY
 *
 * Hauptaktivität der App mit BottomBar-Navigation (Anleitung & Hand-Goniometer).
 */
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PermissionWrapper(viewModel = viewModel) {
                    MainAppScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AppViewModel) {
    val currentScreen = viewModel.backStack.lastOrNull() ?: Screen.Home

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler(enabled = viewModel.backStack.size > 1) {
        viewModel.popBackStack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = { Text(stringResource(currentScreen.titleRes)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        bottomBar = {
            if (!isLandscape && currentScreen.showBottomBar) {
                NavigationBar {
                    MainTabs.forEach { config ->
                        NavigationBarItem(
                            selected = currentScreen == config.screen,
                            onClick = { viewModel.navigateTo(config.screen) },
                            label = { config.label?.let { Text(stringResource(it)) } },
                            icon = {
                                val icon = if (currentScreen == config.screen) config.activeIcon else config.inactiveIcon
                                if (icon != null) {
                                    Icon(imageVector = icon, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavDisplay(
                backStack = viewModel.backStack,
                onBack = { viewModel.popBackStack() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                )
            ) { screen ->
                NavEntry(screen) {
                    when (screen) {
                        is Screen.Home -> HomeScreen(viewModel)
                        is Screen.HandGoniometer -> HandGoniometerScreen(viewModel)
                    }
                }
            }
        }
    }
}

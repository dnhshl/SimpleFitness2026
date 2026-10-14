package com.example.main

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.main.ui.screens.*
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AppViewModel) {
    val currentScreen = viewModel.backStack.lastOrNull() ?: Screen.Home

    // --- ORIENTATION HANDLING ---
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler(enabled = viewModel.backStack.size > 1) {
        viewModel.popBackStack()
    }

    // LEARNING POINT: The Adaptive Layout Wrapper
    // To support a Navigation Rail (side bar) in landscape, we wrap our 
    // whole screen in a 'Row'.
    Row(modifier = Modifier.fillMaxSize()) {
        
        // 1. THE NAVIGATION RAIL (Landscape only)
        // This bar appears on the left when the phone is rotated.
        // It automatically uses the 'MainTabs' defined in AppConfig.kt.
        if (isLandscape && currentScreen.showBottomBar) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxHeight()
            ) {
                MainTabs.forEach { config ->
                    NavigationRailItem(
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

        // 2. THE MAIN CONTENT AREA
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                // We hide the TopBar in landscape to save space for the content.
                if (!isLandscape) {
                    TopAppBar(
                        title = { Text(stringResource(currentScreen.titleRes)) },
                        navigationIcon = {
                            if (!currentScreen.showBottomBar) {
                                IconButton(onClick = { viewModel.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back_description)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            },
            bottomBar = {
                // THE BOTTOM NAVIGATION (Portrait only)
                // In landscape, we use the Rail (see above) instead.
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
                            is Screen.Speedometer -> SpeedometerScreen(viewModel)
                            is Screen.Graph -> GraphScreen(viewModel)
                            is Screen.History -> HistoryScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

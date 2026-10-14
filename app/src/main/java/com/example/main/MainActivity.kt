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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
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
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbarMessage()
        }
    }

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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                // We hide the TopBar in landscape to save space for the content.
                if (!isLandscape) {
                    val uiState by viewModel.uiState.collectAsState()
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
                        actions = {
                            val isLoggedIn = !uiState.currentUserEmail.isNullOrEmpty()
                            if (currentScreen != Screen.Login) {
                                var menuExpanded by remember { mutableStateOf(false) }
                                var showNicknameDialog by remember { mutableStateOf(false) }
                                var nicknameInput by remember(uiState.currentUserNickname) { mutableStateOf(uiState.currentUserNickname ?: "") }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!uiState.currentUserNickname.isNullOrEmpty()) {
                                        Text(
                                            text = uiState.currentUserNickname!!,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Box {
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = stringResource(R.string.desc_account_menu),
                                                tint = if (isLoggedIn) Color(0xFF4CAF50) else LocalContentColor.current
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        if (!uiState.currentUserNickname.isNullOrEmpty()) {
                                                            Text(stringResource(R.string.label_name_prefix, uiState.currentUserNickname!!), style = MaterialTheme.typography.bodyMedium)
                                                        }
                                                        Text(
                                                            if (isLoggedIn) 
                                                                stringResource(R.string.label_user_prefix, uiState.currentUserEmail!!) 
                                                            else 
                                                                stringResource(R.string.label_guest_mode),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                },
                                                onClick = { },
                                                enabled = false
                                            )
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.menu_edit_nickname)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    nicknameInput = uiState.currentUserNickname ?: ""
                                                    showNicknameDialog = true
                                                },
                                                enabled = isLoggedIn
                                            )
                                            if (isLoggedIn) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_logout)) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        viewModel.signOut()
                                                    }
                                                )
                                            } else {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.menu_login_register)) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        viewModel.navigateTo(Screen.Login)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (showNicknameDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showNicknameDialog = false },
                                        title = { Text(stringResource(R.string.dialog_edit_nickname_title)) },
                                        text = {
                                            OutlinedTextField(
                                                value = nicknameInput,
                                                onValueChange = { nicknameInput = it },
                                                label = { Text(stringResource(R.string.dialog_nickname_label)) },
                                                singleLine = true
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    viewModel.setNickname(nicknameInput)
                                                    showNicknameDialog = false
                                                }
                                            ) {
                                                Text(stringResource(R.string.dialog_save))
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showNicknameDialog = false }) {
                                                Text(stringResource(R.string.dialog_cancel))
                                            }
                                        }
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
                            is Screen.Login -> LoginScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

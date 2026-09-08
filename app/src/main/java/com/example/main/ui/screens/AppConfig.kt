package com.example.main.ui.screens

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.example.main.R
import kotlinx.serialization.Serializable

/**
 * 1. THE SCREEN IDs
 */
@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Home : Screen
    @Serializable data object Speedometer : Screen
    @Serializable data object Graph : Screen
}

/**
 * 2. THE CENTRAL CONFIGURATION
 */
data class ScreenConfig(
    val screen: Screen,
    @StringRes val titleRes: Int,
    val showBottomBar: Boolean = true,
    @StringRes val label: Int? = null,
    val activeIcon: ImageVector? = null,
    val inactiveIcon: ImageVector? = null
)

val AppScreenConfigs = listOf(
    ScreenConfig(
        screen = Screen.Home,
        titleRes = R.string.title_home,
        label = R.string.tab_home,
        activeIcon = Icons.Filled.Home,
        inactiveIcon = Icons.Outlined.Home
    ),
    ScreenConfig(
        screen = Screen.Speedometer,
        titleRes = R.string.title_speedometer,
        label = R.string.tab_speedometer,
        activeIcon = Icons.Filled.Dashboard,
        inactiveIcon = Icons.Outlined.Dashboard
    ),
    ScreenConfig(
        screen = Screen.Graph,
        titleRes = R.string.title_graph,
        label = R.string.tab_graph,
        activeIcon = Icons.Filled.Assessment,
        inactiveIcon = Icons.Outlined.Assessment
    )
)

val Screen.config: ScreenConfig
    get() = AppScreenConfigs.first { it.screen == this }

val Screen.titleRes: Int get() = config.titleRes
val Screen.showBottomBar: Boolean get() = config.showBottomBar

val MainTabs = AppScreenConfigs.filter { it.label != null }

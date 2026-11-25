package com.example.main.ui.screens

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.example.main.R
import kotlinx.serialization.Serializable

/**
 * REVERSE ENGINEERING LERNEINHEIT: SCREEN-KONFIGURATION
 *
 * Zentrale Verwalterklasse aller Screens in der App.
 */
@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Home : Screen
    @Serializable data object HandGoniometer : Screen
}

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
        showBottomBar = true,
        label = R.string.tab_home,
        activeIcon = Icons.Filled.MenuBook,
        inactiveIcon = Icons.Outlined.MenuBook
    ),
    ScreenConfig(
        screen = Screen.HandGoniometer,
        titleRes = R.string.title_goniometer,
        showBottomBar = true,
        label = R.string.tab_goniometer,
        activeIcon = Icons.Filled.BackHand,
        inactiveIcon = Icons.Outlined.BackHand
    )
)

val Screen.config: ScreenConfig
    get() = AppScreenConfigs.first { it.screen == this }

val Screen.titleRes: Int get() = config.titleRes
val Screen.showBottomBar: Boolean get() = config.showBottomBar

val MainTabs = AppScreenConfigs.filter { it.label != null }

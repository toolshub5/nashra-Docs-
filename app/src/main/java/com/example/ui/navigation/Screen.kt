package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val route: String) {
    HOME("home"),
    LIBRARY("library"),
    READER("reader"),
    TOOLS("tools"),
    SETTINGS("settings");

    fun getIcon(): ImageVector = when (this) {
        HOME -> Icons.Default.Home
        LIBRARY -> Icons.Default.CollectionsBookmark
        READER -> Icons.Default.MenuBook
        TOOLS -> Icons.Default.Build
        SETTINGS -> Icons.Default.Settings
    }
}

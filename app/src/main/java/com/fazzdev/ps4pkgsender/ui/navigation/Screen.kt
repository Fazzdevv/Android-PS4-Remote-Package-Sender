package com.fazzdev.ps4pkgsender.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Files : Screen("files", "Berkas PKG", Icons.Default.Folder)
    object Queue : Screen("queue", "Antrean", Icons.Default.PlayArrow)
    object Settings : Screen("settings", "Koneksi", Icons.Default.Settings)

    companion object {
        val items = listOf(Files, Queue, Settings)
    }
}

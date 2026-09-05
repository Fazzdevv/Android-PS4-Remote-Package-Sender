package com.fazzdev.ps4pkgsender.ui.screens.files

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fazzdev.ps4pkgsender.data.model.PkgFile
import com.fazzdev.ps4pkgsender.ui.components.OemWarningBanner
import com.fazzdev.ps4pkgsender.ui.components.PkgListItem
import com.fazzdev.ps4pkgsender.ui.i18n.LocalAppStrings
import com.fazzdev.ps4pkgsender.ui.theme.AppTheme
import com.fazzdev.ps4pkgsender.ui.theme.Typography
import com.fazzdev.ps4pkgsender.util.OemBatteryHelper

@Composable
fun FilesScreen(
    viewModel: FilesViewModel,
    onAddToQueue: (List<PkgFile>) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current
    val colors = AppTheme.colors

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBatteryStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // SAF Document Tree Folder Picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            viewModel.onFolderSelected(uri)
        }
    }

    val selectedCount = uiState.files.count { it.isSelected }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        floatingActionButton = {
            if (selectedCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val selected = viewModel.getSelectedFiles()
                        onAddToQueue(selected)
                        viewModel.clearSelection()
                    },
                    containerColor = colors.cyanAccent,
                    contentColor = colors.navyDark,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(strings.sendFiles(selectedCount), style = Typography.titleMedium) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // OEM Battery Warning Banner if applicable
            if (uiState.showOemWarning) {
                OemWarningBanner(
                    oemVendor = uiState.oemVendor,
                    onConfigureClick = {
                        OemBatteryHelper.openOemBatterySettings(context)
                    },
                    onDismissClick = {
                        viewModel.dismissOemWarning()
                    }
                )
            }

            // Top action row: Folder picker button + Search bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onPrimary)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(strings.selectFolder, style = Typography.bodyLarge, color = colors.onPrimary)
                }

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text(strings.searchPlaceholder, style = Typography.bodyMedium, color = colors.textMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textMuted) },
                    singleLine = true,
                    textStyle = Typography.bodyLarge.copy(color = colors.textPrimary),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.cyanAccent,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.cyanAccent
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Multi-selection bar
            if (uiState.files.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCount > 0) "$selectedCount / ${uiState.files.size} selected" else "${uiState.files.size} files",
                        style = Typography.bodyMedium,
                        color = colors.textMuted
                    )

                    Row {
                        if (selectedCount < uiState.files.size) {
                            TextButton(onClick = { viewModel.selectAll() }) {
                                Text(strings.selectAll, color = colors.cyanAccent)
                            }
                        }
                        if (selectedCount > 0) {
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text(strings.deselectAll, color = colors.textMuted)
                            }
                        }
                    }
                }
            }

            // Scanning progress indicator or list
            if (uiState.isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = colors.cyanAccent)
                        Text(
                            text = uiState.statusMessage ?: strings.scanningFiles,
                            style = Typography.bodyMedium,
                            color = colors.textPrimary
                        )
                    }
                }
            } else if (uiState.files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = colors.border,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = strings.noFilesFound,
                            style = Typography.titleMedium,
                            color = colors.textMuted
                        )
                        Text(
                            text = strings.selectFolderPrompt,
                            style = Typography.bodyMedium,
                            color = colors.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.files, key = { it.id }) { pkgFile ->
                        PkgListItem(
                            pkgFile = pkgFile,
                            isSelected = pkgFile.isSelected,
                            onToggleSelect = { viewModel.toggleFileSelection(it) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

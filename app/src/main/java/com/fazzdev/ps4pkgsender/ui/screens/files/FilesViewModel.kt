package com.fazzdev.ps4pkgsender.ui.screens.files

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazzdev.ps4pkgsender.PkgSenderApp
import com.fazzdev.ps4pkgsender.data.model.PkgFile
import com.fazzdev.ps4pkgsender.util.ByteFormatter
import com.fazzdev.ps4pkgsender.util.OemBatteryHelper
import com.fazzdev.ps4pkgsender.util.OemVendor
import com.fazzdev.ps4pkgsender.util.StorageScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FilesUiState(
    val files: List<PkgFile> = emptyList(),
    val isScanning: Boolean = false,
    val searchQuery: String = "",
    val selectedFolderUri: Uri? = null,
    val oemVendor: OemVendor = OemVendor.GENERIC,
    val showOemWarning: Boolean = false,
    val statusMessage: String? = null
)

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PkgSenderApp
    private val db = app.database
    private val settingsManager = app.settingsManager

    private val _isScanning = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolderUri = MutableStateFlow<Uri?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _batteryOptimizationIgnored = MutableStateFlow(
        OemBatteryHelper.isIgnoringBatteryOptimizations(application)
    )

    private val oem = OemBatteryHelper.getCurrentOem()

    private val oemWarningVisibilityFlow = combine(
        settingsManager.isOemWarningDismissed,
        _batteryOptimizationIgnored
    ) { isDismissed, isIgnored ->
        oem != OemVendor.GENERIC && !isDismissed && !isIgnored
    }

    private val selectionFlow = combine(_searchQuery, _selectedIds, _isScanning) { query, selectedIds, isScanning ->
        Triple(query, selectedIds, isScanning)
    }

    val uiState: StateFlow<FilesUiState> = combine(
        db.pkgDao().getAllPkgFilesFlow(),
        selectionFlow,
        _selectedFolderUri,
        _statusMessage,
        oemWarningVisibilityFlow
    ) { entities, selection, folderUri, status, showWarning ->
        val (query, selectedIds, scanning) = selection
        val models = entities.map { entity ->
            PkgFile(
                id = entity.id,
                uri = Uri.parse(entity.uriString),
                name = entity.name,
                sizeBytes = entity.sizeBytes,
                formattedSize = ByteFormatter.formatBytes(entity.sizeBytes),
                lastModified = entity.lastModified,
                isSelected = selectedIds.contains(entity.id),
                titleId = entity.titleId
            )
        }.filter {
            query.isBlank() || it.name.contains(query as CharSequence, ignoreCase = true)
        }

        FilesUiState(
            files = models,
            isScanning = scanning,
            searchQuery = query,
            selectedFolderUri = folderUri,
            oemVendor = oem,
            showOemWarning = showWarning,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilesUiState()
    )

    fun refreshBatteryStatus() {
        _batteryOptimizationIgnored.value = OemBatteryHelper.isIgnoringBatteryOptimizations(getApplication())
    }

    fun dismissOemWarning() {
        settingsManager.setOemWarningDismissed(true)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleFileSelection(file: PkgFile) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(file.id)) {
            current.remove(file.id)
        } else {
            current.add(file.id)
        }
        _selectedIds.value = current
    }

    fun selectAll() {
        val allIds = uiState.value.files.map { it.id }.toSet()
        _selectedIds.value = allIds
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun onFolderSelected(treeUri: Uri) {
        _selectedFolderUri.value = treeUri
        scanFolder(treeUri)
    }

    fun scanFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            _statusMessage.value = "Memindai folder untuk file .pkg..."
            try {
                val scannedEntities = StorageScanner.scanDocumentTree(getApplication(), treeUri)
                db.pkgDao().insertAll(scannedEntities)
                _statusMessage.value = "Ditemukan ${scannedEntities.size} berkas PKG"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal memindai folder: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun getSelectedFiles(): List<PkgFile> {
        return uiState.value.files.filter { it.isSelected }
    }
}

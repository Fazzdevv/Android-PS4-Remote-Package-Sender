package com.fazzdev.ps4pkgsender.ui.screens.tools

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazzdev.ps4pkgsender.PkgSenderApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ToolsUiState(
    val titleIdInput: String = "CUSA02299",
    val isExecuting: Boolean = false,
    val resultMessage: String? = null,
    val logs: List<String> = emptyList(),
    val ps4Ip: String = "192.168.1.100",
    val ps4Port: Int = 12800
)

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PkgSenderApp
    private val settingsManager = app.settingsManager
    private val rpiClient = app.rpiClient

    private val _uiState = MutableStateFlow(
        ToolsUiState(
            ps4Ip = settingsManager.getPs4Ip(),
            ps4Port = settingsManager.getPs4Port()
        )
    )
    val uiState: StateFlow<ToolsUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        viewModelScope.launch {
            settingsManager.ps4Ip.collect { ip ->
                _uiState.update { it.copy(ps4Ip = ip) }
            }
        }
        viewModelScope.launch {
            settingsManager.ps4Port.collect { port ->
                _uiState.update { it.copy(ps4Port = port) }
            }
        }
        addLog("Tools & Log Monitor siap. Target: ${settingsManager.getPs4Ip()}:${settingsManager.getPs4Port()}")
    }

    fun onTitleIdChanged(id: String) {
        _uiState.update { it.copy(titleIdInput = id.trim().uppercase()) }
    }

    fun addLog(message: String) {
        val timestamp = timeFormat.format(Date())
        _uiState.update { it.copy(logs = listOf("[$timestamp] $message") + it.logs.take(99)) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun checkTitleInstalled() {
        val titleId = _uiState.value.titleIdInput
        if (titleId.isBlank()) return

        val ps4Ip = settingsManager.getPs4Ip()
        val ps4Port = settingsManager.getPs4Port()

        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, resultMessage = null) }
            addLog("Mengecek title $titleId ke http://$ps4Ip:$ps4Port/api/is_exists")
            try {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        resultMessage = "Permintaan is_exists dikirim ke $ps4Ip:$ps4Port untuk ID: $titleId"
                    )
                }
            } catch (e: Exception) {
                addLog("Error: ${e.message}")
                _uiState.update { it.copy(isExecuting = false, resultMessage = "Gagal: ${e.message}") }
            }
        }
    }

    fun uninstallGame() {
        val titleId = _uiState.value.titleIdInput
        if (titleId.isBlank()) return

        val ps4Ip = settingsManager.getPs4Ip()
        val ps4Port = settingsManager.getPs4Port()

        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, resultMessage = null) }
            addLog("Meminta uninstall game $titleId ke http://$ps4Ip:$ps4Port/api/uninstall_game")
            val result = rpiClient.uninstallGame(ps4Ip, ps4Port, titleId)
            result.onSuccess {
                addLog("Uninstall game berhasil diperintahkan ke $ps4Ip:$ps4Port.")
                _uiState.update { it.copy(isExecuting = false, resultMessage = "Uninstall game berhasil dikirim ke PS4") }
            }.onFailure { error ->
                addLog("Uninstall game gagal ($ps4Ip:$ps4Port): ${error.message}")
                _uiState.update { it.copy(isExecuting = false, resultMessage = "Gagal: ${error.message}") }
            }
        }
    }

    fun uninstallPatch() {
        val titleId = _uiState.value.titleIdInput
        if (titleId.isBlank()) return

        val ps4Ip = settingsManager.getPs4Ip()
        val ps4Port = settingsManager.getPs4Port()

        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, resultMessage = null) }
            addLog("Meminta uninstall patch $titleId ke http://$ps4Ip:$ps4Port/api/uninstall_patch")
            val result = rpiClient.uninstallPatch(ps4Ip, ps4Port, titleId)
            result.onSuccess {
                addLog("Uninstall patch berhasil diperintahkan ke $ps4Ip:$ps4Port.")
                _uiState.update { it.copy(isExecuting = false, resultMessage = "Uninstall patch berhasil dikirim ke PS4") }
            }.onFailure { error ->
                addLog("Uninstall patch gagal ($ps4Ip:$ps4Port): ${error.message}")
                _uiState.update { it.copy(isExecuting = false, resultMessage = "Gagal: ${error.message}") }
            }
        }
    }
}

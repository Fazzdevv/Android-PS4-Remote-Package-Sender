package com.fazzdev.ps4pkgsender.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazzdev.ps4pkgsender.PkgSenderApp
import com.fazzdev.ps4pkgsender.data.model.ConnectionTestResult
import com.fazzdev.ps4pkgsender.data.model.TestState
import com.fazzdev.ps4pkgsender.data.network.NetworkUtils
import com.fazzdev.ps4pkgsender.service.TransferForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val ps4Ip: String = "192.168.1.100",
    val ps4Port: String = "12800",
    val localPort: String = "8080",
    val localIp: String? = null,
    val testResult: ConnectionTestResult = ConnectionTestResult(),
    val isTesting: Boolean = false,
    val appLanguage: String = "en",
    val appTheme: String = "dark"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PkgSenderApp
    val settingsManager = app.settingsManager
    private val rpiClient = app.rpiClient

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            ps4Ip = settingsManager.getPs4Ip(),
            ps4Port = settingsManager.getPs4Port().toString(),
            localPort = settingsManager.getLocalPort().toString(),
            appLanguage = settingsManager.getAppLanguage(),
            appTheme = settingsManager.getAppTheme()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshLocalIp()
        viewModelScope.launch {
            settingsManager.ps4Ip.collect { ip ->
                _uiState.update { it.copy(ps4Ip = ip) }
            }
        }
        viewModelScope.launch {
            settingsManager.ps4Port.collect { port ->
                _uiState.update { it.copy(ps4Port = port.toString()) }
            }
        }
        viewModelScope.launch {
            settingsManager.localPort.collect { port ->
                _uiState.update { it.copy(localPort = port.toString()) }
            }
        }
        viewModelScope.launch {
            settingsManager.appLanguage.collect { lang ->
                _uiState.update { it.copy(appLanguage = lang) }
            }
        }
        viewModelScope.launch {
            settingsManager.appTheme.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
    }

    fun onLanguageChanged(lang: String) {
        settingsManager.setAppLanguage(lang)
    }

    fun onThemeChanged(theme: String) {
        settingsManager.setAppTheme(theme)
    }

    fun refreshLocalIp() {
        val ip = NetworkUtils.getLocalIpAddress(getApplication())
        _uiState.update { it.copy(localIp = ip) }
    }

    fun onPs4IpChanged(ip: String) {
        val clean = ip.trim()
        _uiState.update { it.copy(ps4Ip = clean) }
        settingsManager.setPs4Ip(clean)
    }

    fun onPs4PortChanged(port: String) {
        val cleanDigits = port.filter { it.isDigit() }
        _uiState.update { it.copy(ps4Port = cleanDigits) }
        cleanDigits.toIntOrNull()?.let { validPort ->
            if (validPort in 1..65535) {
                settingsManager.setPs4Port(validPort)
            }
        }
    }

    fun onLocalPortChanged(port: String) {
        val cleanDigits = port.filter { it.isDigit() }
        _uiState.update { it.copy(localPort = cleanDigits) }
        cleanDigits.toIntOrNull()?.let { validPort ->
            if (validPort in 1..65535) {
                settingsManager.setLocalPort(validPort)
            }
        }
    }

    /**
     * Executes 2-layer test connection per PRD 6.6
     */
    fun runConnectionTest() {
        val currentState = _uiState.value
        val ps4PortInt = currentState.ps4Port.toIntOrNull() ?: settingsManager.getPs4Port()
        val localPortInt = currentState.localPort.toIntOrNull() ?: settingsManager.getLocalPort()
        val ps4Ip = currentState.ps4Ip.ifBlank { settingsManager.getPs4Ip() }

        // Commit latest values to SettingsManager
        settingsManager.setPs4Ip(ps4Ip)
        settingsManager.setPs4Port(ps4PortInt)
        settingsManager.setLocalPort(localPortInt)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTesting = true,
                    testResult = ConnectionTestResult(
                        localServerState = TestState.Loading,
                        ps4RpiState = TestState.Loading,
                        localPort = localPortInt,
                        ps4Port = ps4PortInt
                    )
                )
            }

            // Ensure local server is up for loopback test
            TransferForegroundService.start(getApplication(), localPortInt)

            val result = rpiClient.testConnection(
                localIp = currentState.localIp,
                localPort = localPortInt,
                ps4Ip = ps4Ip,
                ps4Port = ps4PortInt
            )

            _uiState.update {
                it.copy(
                    isTesting = false,
                    testResult = result
                )
            }
        }
    }
}

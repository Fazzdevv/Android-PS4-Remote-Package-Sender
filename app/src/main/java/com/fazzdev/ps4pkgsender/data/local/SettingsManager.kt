package com.fazzdev.ps4pkgsender.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ps4_sender_prefs", Context.MODE_PRIVATE)

    private val _ps4Ip = MutableStateFlow(prefs.getString("ps4_ip", "192.168.1.100") ?: "192.168.1.100")
    val ps4Ip: StateFlow<String> = _ps4Ip.asStateFlow()

    private val _ps4Port = MutableStateFlow(prefs.getInt("ps4_port", 12800))
    val ps4Port: StateFlow<Int> = _ps4Port.asStateFlow()

    private val _localPort = MutableStateFlow(prefs.getInt("local_port", 8080))
    val localPort: StateFlow<Int> = _localPort.asStateFlow()

    private val _isOemWarningDismissed = MutableStateFlow(prefs.getBoolean("oem_warning_dismissed", false))
    val isOemWarningDismissed: StateFlow<Boolean> = _isOemWarningDismissed.asStateFlow()

    // Language: "en" (English - Default) or "id" (Indonesian)
    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "en") ?: "en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    // Theme: "dark" (Default), "light", "system"
    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "dark") ?: "dark")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    fun getPs4Ip(): String = _ps4Ip.value
    fun getPs4Port(): Int = _ps4Port.value
    fun getLocalPort(): Int = _localPort.value
    fun isOemWarningDismissed(): Boolean = _isOemWarningDismissed.value
    fun getAppLanguage(): String = _appLanguage.value
    fun getAppTheme(): String = _appTheme.value

    fun setOemWarningDismissed(dismissed: Boolean) {
        _isOemWarningDismissed.value = dismissed
        prefs.edit().putBoolean("oem_warning_dismissed", dismissed).apply()
    }

    fun setAppLanguage(lang: String) {
        val clean = if (lang.equals("id", ignoreCase = true)) "id" else "en"
        _appLanguage.value = clean
        prefs.edit().putString("app_language", clean).apply()
    }

    fun setAppTheme(theme: String) {
        val clean = when (theme.lowercase()) {
            "light" -> "light"
            "system" -> "system"
            else -> "dark"
        }
        _appTheme.value = clean
        prefs.edit().putString("app_theme", clean).apply()
    }

    fun setPs4Ip(ip: String) {
        val cleanIp = ip.trim()
        _ps4Ip.value = cleanIp
        prefs.edit().putString("ps4_ip", cleanIp).apply()
    }

    fun setPs4Port(port: Int) {
        if (port in 1..65535) {
            _ps4Port.value = port
            prefs.edit().putInt("ps4_port", port).apply()
        }
    }

    fun setLocalPort(port: Int) {
        if (port in 1..65535) {
            _localPort.value = port
            prefs.edit().putInt("local_port", port).apply()
        }
    }
}

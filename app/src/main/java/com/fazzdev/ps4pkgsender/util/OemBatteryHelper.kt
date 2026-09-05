package com.fazzdev.ps4pkgsender.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

enum class OemVendor(val displayName: String) {
    XIAOMI("Xiaomi / Redmi / POCO"),
    OPPO_REALME("OPPO / Realme / OnePlus"),
    HUAWEI("Huawei / Honor"),
    SAMSUNG("Samsung"),
    GENERIC("Android Standar")
}

object OemBatteryHelper {

    private const val TAG = "OemBatteryHelper"

    fun getCurrentOem(): OemVendor {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> OemVendor.XIAOMI
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> OemVendor.OPPO_REALME
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> OemVendor.HUAWEI
            manufacturer.contains("samsung") -> OemVendor.SAMSUNG
            else -> OemVendor.GENERIC
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * Attempts to open OEM-specific battery restriction or autostart settings.
     * Falls back to standard Android app settings or battery optimization prompt.
     */
    fun openOemBatterySettings(context: Context) {
        val oem = getCurrentOem()
        val intentsToTry = mutableListOf<Intent>()

        when (oem) {
            OemVendor.XIAOMI -> {
                // MIUI / HyperOS autostart & battery saver
                intentsToTry.add(Intent().apply {
                    component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                })
                intentsToTry.add(Intent().apply {
                    component = ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                    putExtra("package_name", context.packageName)
                    putExtra("package_label", "PS4 PKG Sender")
                })
            }
            OemVendor.OPPO_REALME -> {
                // ColorOS / Realme UI
                intentsToTry.add(Intent().apply {
                    component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                })
                intentsToTry.add(Intent().apply {
                    component = ComponentName("com.oplus.battery", "com.oplus.battery.settings.AppBatterySettingsActivity")
                })
            }
            OemVendor.HUAWEI -> {
                // EMUI / MagicOS
                intentsToTry.add(Intent().apply {
                    component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
                })
            }
            OemVendor.SAMSUNG -> {
                // Samsung Device Care
                intentsToTry.add(Intent().apply {
                    component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
                })
            }
            OemVendor.GENERIC -> {
                // Generic AOSP handled below
            }
        }

        // Standard AOSP Fallbacks
        intentsToTry.add(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        })
        intentsToTry.add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        intentsToTry.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        })

        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.i(TAG, "Successfully launched intent: ${intent.component ?: intent.action}")
                return
            } catch (e: Exception) {
                Log.d(TAG, "Failed launching intent ${intent.component ?: intent.action}: ${e.message}")
            }
        }
    }

    fun getOemExplanation(oem: OemVendor): String {
        return when (oem) {
            OemVendor.XIAOMI -> "Perangkat Xiaomi/HyperOS/MIUI secara agresif mematikan server latar belakang. Buka 'Autostart' dan atur Penghemat Baterai ke 'Tidak ada pembatasan' agar pengiriman PKG tidak terputus."
            OemVendor.OPPO_REALME -> "ColorOS/Realme UI membatasi aplikasi berjalan saat layar mati. Aktifkan 'Izinkan aktivitas latar belakang' di Pengaturan Baterai."
            OemVendor.HUAWEI -> "EMUI mematikan server jika layar terkunci. Atur peluncuran aplikasi ke 'Kelola secara manual' dan izinkan berjalan di latar belakang."
            OemVendor.SAMSUNG -> "One UI dapat menidurkan aplikasi. Tambahkan PS4 PKG Sender ke daftar 'Aplikasi yang tidak pernah tidur' (Never sleeping apps)."
            OemVendor.GENERIC -> "Pastikan optimasi baterai dinonaktifkan agar transfer file berukuran besar tetap stabil saat layar HP dimatikan."
        }
    }
}

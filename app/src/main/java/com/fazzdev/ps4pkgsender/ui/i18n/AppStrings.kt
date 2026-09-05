package com.fazzdev.ps4pkgsender.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

interface AppStrings {
    // Navigation
    val navFiles: String
    val navQueue: String
    val navSettings: String

    // Files Screen
    val appTitle: String
    val selectFolder: String
    val searchPlaceholder: String
    val selectAll: String
    val deselectAll: String
    val sendFiles: (Int) -> String
    val noFilesFound: String
    val selectFolderPrompt: String
    val scanningFiles: String

    // OEM Banner
    val oemDetected: (String) -> String
    val oemConfigure: String
    val oemDismiss: String
    val oemClose: String
    val oemExplanationXiaomi: String
    val oemExplanationOppo: String
    val oemExplanationHuawei: String
    val oemExplanationSamsung: String
    val oemExplanationGeneric: String

    // Queue Screen
    val queueTitle: String
    val tabActive: String
    val tabCompleted: String
    val clearQueue: String
    val emptyActiveQueue: String
    val emptyCompletedQueue: String
    val statusQueued: String
    val statusSending: String
    val statusInstalling: String
    val statusSuccess: String
    val statusFailed: String
    val statusPaused: String
    val statusCancelled: String
    val pause: String
    val resume: String
    val cancel: String
    val speed: String
    val eta: String
    val batchProgress: String

    // Settings Screen
    val settingsTitle: String
    val ps4Destination: String
    val ps4IpLabel: String
    val ps4PortLabel: String
    val localServer: String
    val localPortLabel: String
    val localIpLabel: (String) -> String
    val testConnection: String
    val testingConnection: String
    val testResultTitle: String
    val testLayerServer: String
    val testLayerRpi: String
    val testNotTested: String
    val testChecking: String
    val troubleshootingTips: String

    // Language & Theme
    val languageTitle: String
    val languageEnglish: String
    val languageEnglishDesc: String
    val languageIndonesian: String
    val languageIndonesianDesc: String
    val themeTitle: String
    val themeDark: String
    val themeDarkDesc: String
    val themeLight: String
    val themeLightDesc: String
    val themeSystem: String
    val themeSystemDesc: String

    // Battery & Background
    val batteryOptTitle: String
    val openSystemSettings: String

    // Credits
    val creditsTitle: String
    val creditsDeveloper: String
    val creditsDeveloperName: String
    val creditsApi: String
    val creditsVersion: String
    val creditsNote: String
}

object EnglishStrings : AppStrings {
    override val navFiles = "PKG Files"
    override val navQueue = "Queue"
    override val navSettings = "Connection"

    override val appTitle = "PS4 PKG Sender"
    override val selectFolder = "Select Folder"
    override val searchPlaceholder = "Search .pkg..."
    override val selectAll = "Select All"
    override val deselectAll = "Deselect"
    override val sendFiles = { count: Int -> "Send ($count Files)" }
    override val noFilesFound = "No .pkg files found in this folder"
    override val selectFolderPrompt = "Please select a folder containing your PS4 PKG files"
    override val scanningFiles = "Scanning folder for .pkg files..."

    override val oemDetected = { vendor: String -> "Device Detected: $vendor" }
    override val oemConfigure = "Open Background Settings"
    override val oemDismiss = "Already Configured"
    override val oemClose = "Close"
    override val oemExplanationXiaomi = "Xiaomi/HyperOS/MIUI aggressively terminates background servers. Please enable 'Autostart' and set Battery Saver to 'No restrictions' to ensure uninterrupted PKG transfers."
    override val oemExplanationOppo = "ColorOS/Realme UI restricts background apps when locked. Enable 'Allow background activity' in Battery Settings."
    override val oemExplanationHuawei = "EMUI terminates background servers when locked. Set app launch to 'Manage manually' and allow running in background."
    override val oemExplanationSamsung = "One UI may put background apps to sleep. Add PS4 PKG Sender to 'Never sleeping apps' in Device Care."
    override val oemExplanationGeneric = "Ensure battery optimization is disabled so large file transfers remain stable when the screen turns off."

    override val queueTitle = "Queue & Progress"
    override val tabActive = "Active Queue"
    override val tabCompleted = "History"
    override val clearQueue = "Clear All"
    override val emptyActiveQueue = "No active tasks in queue"
    override val emptyCompletedQueue = "No completed transfers yet"
    override val statusQueued = "Queued"
    override val statusSending = "Sending..."
    override val statusInstalling = "Installing on PS4..."
    override val statusSuccess = "Completed"
    override val statusFailed = "Failed"
    override val statusPaused = "Paused"
    override val statusCancelled = "Cancelled"
    override val pause = "Pause"
    override val resume = "Resume"
    override val cancel = "Cancel"
    override val speed = "Speed"
    override val eta = "ETA"
    override val batchProgress = "Overall Batch Progress"

    override val settingsTitle = "Network & Configuration"
    override val ps4Destination = "PS4 Destination Target"
    override val ps4IpLabel = "PS4 IP Address (e.g. 192.168.1.100)"
    override val ps4PortLabel = "RPI Port (Default: 12800)"
    override val localServer = "Local HTTP Server (Phone)"
    override val localPortLabel = "Local Server Port (Default: 8080)"
    override val localIpLabel = { ip: String -> "Phone IP Address: $ip" }
    override val testConnection = "Test Connection"
    override val testingConnection = "Testing Connection..."
    override val testResultTitle = "Connection Test Results (2-Layer)"
    override val testLayerServer = "1. Local HTTP Server (Phone)"
    override val testLayerRpi = "2. Remote Package Installer (PS4)"
    override val testNotTested = "Not tested yet"
    override val testChecking = "Checking..."
    override val troubleshootingTips = "Troubleshooting Tips"

    override val languageTitle = "App Language"
    override val languageEnglish = "English (Default)"
    override val languageEnglishDesc = "English interface language"
    override val languageIndonesian = "Bahasa Indonesia"
    override val languageIndonesianDesc = "Tampilan antarmuka Bahasa Indonesia"
    override val themeTitle = "Theme Mode"
    override val themeDark = "Dark Theme"
    override val themeDarkDesc = "Deep dark theme optimized for OLED & gaming"
    override val themeLight = "Light Theme"
    override val themeLightDesc = "Clean & bright appearance for daylight"
    override val themeSystem = "System Default"
    override val themeSystemDesc = "Automatically follows system display settings"

    override val batteryOptTitle = "Background & Battery Optimization"
    override val openSystemSettings = "Open System Settings"

    override val creditsTitle = "About & Credits"
    override val creditsDeveloper = "Developer"
    override val creditsDeveloperName = "Fazzdevv"
    override val creditsApi = "Remote Package Installer PS4 API by flatz"
    override val creditsVersion = "Version 1.0.0"
    override val creditsNote = "Engineered for rock-solid, high-speed PS4 PKG deployment directly from your Android device over Wi-Fi."
}

object IndonesianStrings : AppStrings {
    override val navFiles = "Berkas PKG"
    override val navQueue = "Antrean"
    override val navSettings = "Koneksi"

    override val appTitle = "PS4 PKG Sender"
    override val selectFolder = "Pilih Folder"
    override val searchPlaceholder = "Cari .pkg..."
    override val selectAll = "Pilih Semua"
    override val deselectAll = "Batal Pilih"
    override val sendFiles = { count: Int -> "Kirim ($count File)" }
    override val noFilesFound = "Tidak ada berkas .pkg di folder ini"
    override val selectFolderPrompt = "Silakan pilih folder penyimpanan berkas PKG Anda"
    override val scanningFiles = "Memindai folder mencari berkas .pkg..."

    override val oemDetected = { vendor: String -> "Deteksi Perangkat: $vendor" }
    override val oemConfigure = "Buka Pengaturan Latar Belakang"
    override val oemDismiss = "Sudah Diatur"
    override val oemClose = "Tutup"
    override val oemExplanationXiaomi = "Perangkat Xiaomi/HyperOS/MIUI secara agresif mematikan server latar belakang. Buka 'Autostart' dan atur Penghemat Baterai ke 'Tidak ada pembatasan' agar pengiriman PKG tidak terputus."
    override val oemExplanationOppo = "ColorOS/Realme UI membatasi aplikasi berjalan saat layar mati. Aktifkan 'Izinkan aktivitas latar belakang' di Pengaturan Baterai."
    override val oemExplanationHuawei = "EMUI mematikan server jika layar terkunci. Atur peluncuran aplikasi ke 'Kelola secara manual' dan izinkan berjalan di latar belakang."
    override val oemExplanationSamsung = "One UI dapat menidurkan aplikasi. Tambahkan PS4 PKG Sender ke daftar 'Aplikasi yang tidak pernah tidur' (Never sleeping apps)."
    override val oemExplanationGeneric = "Pastikan optimasi baterai dinonaktifkan agar transfer file berukuran besar tetap stabil saat layar HP dimatikan."

    override val queueTitle = "Antrean & Progres"
    override val tabActive = "Antrean Aktif"
    override val tabCompleted = "Riwayat Selesai"
    override val clearQueue = "Bersihkan Semua"
    override val emptyActiveQueue = "Tidak ada antrean yang sedang berjalan"
    override val emptyCompletedQueue = "Belum ada riwayat transfer"
    override val statusQueued = "Menunggu"
    override val statusSending = "Mengirim..."
    override val statusInstalling = "Memasang di PS4..."
    override val statusSuccess = "Selesai"
    override val statusFailed = "Gagal"
    override val statusPaused = "Dijeda"
    override val statusCancelled = "Dibatalkan"
    override val pause = "Jeda"
    override val resume = "Lanjutkan"
    override val cancel = "Batalkan"
    override val speed = "Kecepatan"
    override val eta = "Estimasi Waktu"
    override val batchProgress = "Progres Antrean Keseluruhan"

    override val settingsTitle = "Pengaturan Jaringan & PS4"
    override val ps4Destination = "Tujuan PS4"
    override val ps4IpLabel = "Alamat IP PS4 (Contoh: 192.168.1.100)"
    override val ps4PortLabel = "Port RPI (Standar: 12800)"
    override val localServer = "Server HTTP Lokal (HP)"
    override val localPortLabel = "Port Server HTTP Lokal (Standar: 8080)"
    override val localIpLabel = { ip: String -> "Alamat IP HP: $ip" }
    override val testConnection = "Uji Koneksi"
    override val testingConnection = "Sedang Menguji Koneksi..."
    override val testResultTitle = "Hasil Uji Koneksi (2-Lapis)"
    override val testLayerServer = "1. Server HTTP Lokal (HP)"
    override val testLayerRpi = "2. Remote Package Installer (PS4)"
    override val testNotTested = "Belum diuji"
    override val testChecking = "Sedang memeriksa..."
    override val troubleshootingTips = "Tips Troubleshooting"

    override val languageTitle = "Bahasa Aplikasi"
    override val languageEnglish = "English (Default)"
    override val languageEnglishDesc = "English interface language"
    override val languageIndonesian = "Bahasa Indonesia"
    override val languageIndonesianDesc = "Tampilan antarmuka Bahasa Indonesia"
    override val themeTitle = "Tema Tampilan"
    override val themeDark = "Tema Gelap"
    override val themeDarkDesc = "Nuansa gelap khas PlayStation, hemat baterai OLED"
    override val themeLight = "Tema Terang"
    override val themeLightDesc = "Tampilan bersih & kontras tinggi di siang hari"
    override val themeSystem = "Ikuti Sistem"
    override val themeSystemDesc = "Menyesuaikan otomatis dengan setelan sistem HP"

    override val batteryOptTitle = "Optimasi Latar Belakang & Baterai"
    override val openSystemSettings = "Buka Pengaturan Sistem"

    override val creditsTitle = "Tentang & Kredit"
    override val creditsDeveloper = "Pengembang"
    override val creditsDeveloperName = "Fazzdevv"
    override val creditsApi = "API Remote Package Installer PS4 oleh flatz"
    override val creditsVersion = "Versi 1.0.0"
    override val creditsNote = "Dirancang untuk instalasi berkas PKG PS4 langsung dari perangkat Android secara stabil dan efisien melalui Wi-Fi."
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }

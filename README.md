# PS4 Package Sender for Android

Aplikasi Android native (Kotlin + Jetpack Compose + Material 3) untuk mengelola, menyajikan, dan mengirim berkas `.pkg` (fPKG) langsung dari smartphone/tablet Android ke konsol PS4 yang menjalankan **Remote Package Installer (RPI)** tanpa memerlukan PC.

Dikembangkan berdasarkan spesifikasi lengkap **PRD v0.4**.

---

## Fitur Utama

- **Storage Access Framework (SAF) & Room Cache:**
  - Pemilihan folder fPKG menggunakan Document Tree Picker bawaan Android dengan izin URI persisten.
  - Hasil scan tersimpan di Room Database lokal sehingga daftar file langsung muncul instan tanpa perlu scan ulang.
  - Multi-selection file dengan indikator ukuran dan total kalkulasi batch.

- **Local Multi-Threaded HTTP Server dengan HTTP Range (RFC 7233):**
  - Mengalirkan file berukuran puluhan GB langsung dari `ContentResolver`/`ParcelFileDescriptor` dengan pemosisian `FileChannel.position(start)`.
  - Mendukung `Range: bytes=start-end`, `206 Partial Content`, dan `Content-Range`.
  - Arsitektur multi-threaded dengan worker pool untuk menangani koneksi paralel dari PS4.

- **Single-Task per Batch Installation (PRD 6.1 & 6.8):**
  - Mengirim seluruh file yang ditandai dalam satu payload `POST /api/install` (array `packages`), menghasilkan satu `task_id` terpadu.
  - Proteksi kegagalan instan: deteksi dini `task_id: -1` dari RPI.
  - Pelacakan progres agregat real-time via polling `GET /api/get_task_progress`.
  - Label status akurat: **"Terkirim ke PS4 ✅"** (bukan "Instalasi selesai").
  - Tombol Kirim otomatis nonaktif selama ada batch yang sedang aktif berjalan.

- **Wi-Fi Lock & Foreground Service:**
  - `TransferForegroundService` berjalan di latar belakang dengan notifikasi persisten dan aksi kontrol.
  - Mengunci radio Wi-Fi dengan `WifiManager.WIFI_MODE_FULL_HIGH_PERF` dan `PARTIAL_WAKE_LOCK` agar transfer file besar 40GB+ tidak terhenti atau menurun kecepatannya saat layar HP terkunci.

- **Uji Koneksi 2-Lapis (Connection Test):**
  - Lapis 1: Verifikasi HTTP Server lokal (loopback).
  - Lapis 2: Verifikasi konektivitas ke PS4 RPI (`GET /api/is_exists`) dengan fallback raw socket connect.
  - Panduan troubleshooting otomatis jika tes gagal (AP/Client isolation, IP, RPI foreground).

- **Multi-Theme & Modern PlayStation Aesthetic:**
  - Pilihan tema gelap khas PlayStation (OLED-optimized), tema terang kontras tinggi, dan ikuti setelan sistem.
  - UI responsif dan elegan dengan Material 3, aksen PlayStation Blue & Cyan, dan transisi halus.

- **Multilingual Support (Bilingual):**
  - Mendukung Bahasa Inggris (Default) dan Bahasa Indonesia dengan opsi perubahan instan di Pengaturan.

- **Panduan & Dismiss Restriksi Baterai OEM:**
  - Deteksi otomatis vendor perangkat (`Build.MANUFACTURER`: HyperOS/MIUI, ColorOS/Realme, EMUI, One UI).
  - Banner peringatan dapat dikonfigurasi langsung atau ditutup jika sudah diatur.

---

## Struktur Proyek

```
PROYEK android to ps4/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/fazzdev/ps4pkgsender/
│   │   │   │   ├── PkgSenderApp.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/ (PkgFile, BatchTask, RpiApiModels, ConnectionStatus)
│   │   │   │   │   ├── local/ (Room DB: PkgEntity, PkgDao, AppDatabase)
│   │   │   │   │   ├── network/ (NetworkUtils, RpiApiService, RpiClient)
│   │   │   │   │   └── server/ (ContentRangeStreamer, PackageHttpServer)
│   │   │   │   ├── service/ (TransferForegroundService)
│   │   │   │   ├── util/ (OemBatteryHelper, StorageScanner, ByteFormatter)
│   │   │   │   └── ui/
│   │   │   │       ├── MainActivity.kt
│   │   │   │       ├── theme/ (PlayStation Dark Aesthetic)
│   │   │   │       ├── navigation/ (Screen routes)
│   │   │   │       ├── components/ (StatusBadge, OemBanner, PkgCard, ProgressBar)
│   │   │   │       └── screens/
│   │   │   │           ├── files/ (FilesScreen, FilesViewModel)
│   │   │   │           ├── queue/ (QueueScreen, QueueViewModel)
│   │   │   │           ├── settings/ (SettingsScreen, SettingsViewModel)
│   │   │   │           └── tools/ (ToolsScreen, ToolsViewModel)
│   │   │   └── res/ (Strings, Themes, Drawables, Mipmaps)
│   │   └── test/ (ContentRangeTest, ByteFormatterTest, RpiPayloadTest)
│   └── build.gradle.kts
├── gradle/
│   ├── wrapper/gradle-wrapper.properties
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

---

## Cara Build & Menjalankan Aplikasi

1. Buka folder proyek ini di **Android Studio** (Hedgehog, Iguana, Koala, atau versi yang lebih baru).
2. Biarkan Gradle melakukan sync dependensi secara otomatis.
3. Hubungkan perangkat Android fisik atau Emulator (target Android 10+ / API 29+).
4. Klik **Run** (`Shift + F10`) di Android Studio untuk meng-compile dan menginstall APK.
5. Atau via baris perintah:
   ```bash
   ./gradlew assembleDebug
   ```
   File APK akan dibuat di: `app/build/outputs/apk/debug/app-debug.apk`.

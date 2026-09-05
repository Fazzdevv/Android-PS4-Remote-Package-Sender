# PRD: PS4 Package Sender for Android

**Versi dokumen:** 0.4 (Draft)
**Tanggal:** 4 September 2026
**Status:** Draft — untuk direview & disempurnakan sebelum development dimulai
**Perubahan dari 0.3:**
1. **Keputusan desain: single task per batch** (bukan multi-task per file) — satu `POST /api/install` per aksi Kirim, berisi seluruh file yang ditandai dalam satu `packages` array, menghasilkan **satu** `task_id`. Section 4, 5, 6.1, 6.8, 9 diperbarui untuk mencerminkan ini; progres & kontrol (pause/resume/stop) sekarang eksplisit di level batch, bukan per item.
2. **6.8:** ditambah state eksplisit untuk `task_id: -1` — kegagalan instan, bukan lewat timeout — berdasarkan laporan pengguna nyata di RPI fork lain (base PKG tertentu mengembalikan `task_id: -1`, DLC normal).
3. **6.8/9:** dicatat risiko baru — granularitas progres kemungkinan cuma level batch/agregat, bukan per file individual, sampai format respons `get_task_progress` divalidasi empiris.

---

## 1. Latar Belakang

Saat ini alat untuk mengirim file `.pkg` (fPKG) ke PS4 yang sudah di-jailbreak (HEN/GoldHEN) hanya tersedia dalam bentuk aplikasi desktop berbasis Electron (contoh: `ps4-remote-pkg-sender`). Aplikasi ini menjalankan HTTP server lokal untuk menyajikan file, lalu memerintahkan **Remote Package Installer (RPI)** buatan flatz yang berjalan di PS4 untuk mengambil dan menginstal file tersebut lewat jaringan lokal.

Tidak ada versi native untuk Android. Banyak pengguna menyimpan koleksi fPKG mereka di HP/tablet Android dan ingin bisa mengirim langsung dari sana tanpa perlu PC.

## 2. Tujuan (Goals)

- Menyediakan aplikasi Android yang bisa:
  - Menemukan file `.pkg`/fPKG di storage HP.
  - Menyajikan file tersebut lewat HTTP server lokal.
  - Mengirim perintah install/uninstall ke PS4 lewat RPI API.
  - Menampilkan antrian & status proses instalasi secara akurat (via Task Progress API RPI).
- Pengalaman pengguna sederhana: pilih file → pilih PS4 tujuan → kirim.

## 3. Target Pengguna

Pemilik PS4 dengan firmware yang sudah di-jailbreak dan RPI aktif, yang ingin mengelola & mengirim fPKG langsung dari perangkat Android (HP/tablet) tanpa PC.

## 4. Ringkasan Fitur (v1 / MVP)

| # | Fitur | Prioritas |
|---|-------|-----------|
| 1 | Scan storage HP untuk file `.pkg` (SAF folder picker default; deep-scan opsional via all-files access — lihat 6.10) | Must |
| 2 | Local HTTP server (foreground service) untuk serve file yang dipilih — **multi-threaded + Range support** (6.7) | Must |
| 3 | Konfigurasi IP & port PS4 (manual input) | Must |
| 4 | Kirim perintah install ke RPI (`POST /api/install`) | Must |
| 5 | Antrian (queue) multi-file — dikirim sebagai **satu task tunggal** per batch (`packages` array dalam satu `POST /api/install`), progres agregat via polling Task Progress API (6.8) | Must |
| 6 | Log request/response dari RPI (untuk debugging koneksi) | Should |
| 7 | Uninstall game/patch/DLC (`/api/uninstall_*`) | Should |
| 8 | Auto-discovery PS4 di jaringan (broadcast/DDP) | Could |
| 9 | Dukungan PS5 | Could |
| 10 | Baca judul dari SFO header / cover art | Won't (v1) |
| 11 | Deteksi OEM (HyperOS/MIUI, ColorOS, EMUI, One UI) & panduan nonaktifkan battery/background restriction | Must |
| 12 | Tombol Test Koneksi — cek HTTP server lokal & konektivitas ke PS4/RPI (via `/api/is_exists`, 6.6) | Must |
| 13 | Task control: pause/resume/stop instalasi yang sedang berjalan (`/api/pause_task`, `/api/resume_task`, `/api/stop_task`) | Should |

## 5. Alur Pengguna Utama (User Flow)

1. Buka app → beri izin akses storage (SAF folder picker).
2. App scan folder yang dipilih & menampilkan daftar file `.pkg` yang ditemukan.
3. User memasukkan/memilih IP PS4 (dan port RPI, default `12800`).
3a. User menekan **Test Koneksi** untuk memverifikasi: (a) HTTP server lokal di HP sudah aktif & bisa diakses, (b) RPI di PS4 merespons (via `/api/is_exists`). Hasil ditampilkan sebagai status jelas (✅/❌ + pesan error) sebelum lanjut kirim file.
4. User menandai satu/lebih file → tambah ke antrian (batch belum dikirim, murni daftar lokal, bisa diedit bebas).
5. User tekan **Kirim** → app menyalakan HTTP server lokal (jika belum jalan) → mengirim **satu** `POST /api/install` ke RPI dengan `packages` berisi URL **semua file yang ditandai** (mengarah ke server lokal itu sendiri) → app menyimpan **satu** `task_id` untuk seluruh batch itu. Daftar file dalam batch ini terkunci setelah dikirim.
6. App menampilkan progres batch (pending → task dibuat → mengunduh X% agregat → terkirim ke PS4), diperbarui lewat polling `GET /api/get_task_progress?task_id=<id-batch>`. Seluruh item dalam batch berpindah status bersamaan (lihat catatan granularitas di 6.8). File susulan yang ditandai setelah Kirim ditekan menunggu batch berjalan selesai/dibatalkan, lalu dikirim sebagai task baru.
7. User bisa hapus/uninstall title dari PS4 lewat menu terpisah (dengan konfirmasi via `/api/is_exists` dulu).

## 6. Spesifikasi Teknis

### 6.1 Protokol RPI (referensi, sudah publik dari flatz)
RPI di PS4 berjalan sebagai HTTP server pada port **12800**. Endpoint yang terdokumentasi publik:

```
POST http://<IP-PS4>:12800/api/install
Body: {"type":"direct","packages":["http://<IP-HP>:<port>/nama.pkg", ...]}
Respons: berisi task_id untuk task yang dibuat

POST http://<IP-PS4>:12800/api/uninstall_game
Body: {"title_id":"CUSA02299"}

POST http://<IP-PS4>:12800/api/uninstall_patch
Body: {"title_id":"CUSA08344"}

GET  http://<IP-PS4>:12800/api/is_exists?title_id=<ID>
→ cek apakah title sudah terinstall (juga mengembalikan ukuran jika ada)

GET  http://<IP-PS4>:12800/api/get_task_progress?task_id=<id>
→ progres task berjalan

GET  http://<IP-PS4>:12800/api/find_task?content_id=<id>
→ cari task berdasarkan content

POST /api/pause_task   Body: {"task_id":"<id>"}
POST /api/resume_task  Body: {"task_id":"<id>"}
POST /api/stop_task    Body: {"task_id":"<id>"}
POST /api/unregister_task Body: {"task_id":"<id>"}
```

Catatan: RPI di PS4 harus dalam kondisi foreground/aktif (tidak diminimize) agar bisa menerima koneksi jaringan — ini batasan dari RPI itu sendiri, bukan dari app pengirim.

**Keputusan desain (v0.4):** app selalu mengirim **satu task per aksi Kirim**. Kalau user menandai banyak file lalu tekan Kirim, semua URL dimasukkan ke satu array `packages` dalam satu panggilan `POST /api/install`, menghasilkan satu `task_id` untuk keseluruhan batch — app **tidak** melakukan panggilan `install` terpisah per file. Implikasi granularitas progres & kontrol per-item dijelaskan di 6.8.

### 6.2 Komponen Aplikasi Android

| Komponen | Fungsi | Opsi teknis |
|----------|--------|-------------|
| File Scanner | Cari file `.pkg` di storage | Storage Access Framework (folder picker) sebagai default; `MANAGE_EXTERNAL_STORAGE` sebagai opsi deep-scan (6.10) |
| Local HTTP Server | Serve file pkg ke PS4 | NanoHTTPD / Ktor Server (native) — **wajib konfigurasi thread pool** (6.7) |
| RPI Client | Kirim command ke PS4 + polling task progress | OkHttp/Retrofit |
| Foreground Service | Jaga server tetap hidup saat app di-background | Android `Foreground Service` + notifikasi persisten |
| Queue Manager | Kelola antrian, task_id, status dari polling | ViewModel + State |
| UI | Pilih file, atur PS4, lihat status | Jetpack Compose |
| Network Info | Ambil IP lokal HP untuk disisipkan ke URL file | **`ConnectivityManager` / `LinkProperties`** — jangan `WifiManager.getConnectionInfo()` karena butuh izin lokasi di Android 8–12 |

### 6.3 Pilihan Arsitektur

**Opsi A — Native Kotlin (direkomendasikan untuk v1)**
- Kontrol penuh atas lifecycle Android (izin storage, foreground service, battery optimization).
- Tidak ada overhead runtime tambahan (tidak perlu bundle Node.js ke APK).
- Trade-off: seluruh logic (scan, server, API client) ditulis ulang dari nol.

**Opsi B — Hybrid via `nodejs-mobile`**
- Bagian server Express + logic panggil API RPI dari proyek desktop lama bisa dipakai ulang hampir tanpa ubah.
- Trade-off: ukuran APK lebih besar (bundle Node runtime), integrasi dengan permission Android (storage, foreground service) lebih rumit karena harus jembatani JS ↔ native lewat bridge.

*Rekomendasi: mulai dengan Opsi A untuk MVP karena lebih stabil di Android; Opsi B bisa dipertimbangkan kalau ingin mempercepat porting fitur-fitur kompleks (HB Store, SFO parser) di v2.*

### 6.4 Permission yang Dibutuhkan
- `INTERNET`
- `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`
- `MANAGE_EXTERNAL_STORAGE` (opsional — hanya kalau user aktifkan mode deep-scan, 6.10)
- `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS` (Android 13+)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (untuk minta whitelist dari Doze/App Standby standar Android)
- `WAKE_LOCK` (prasyarat wajib untuk `WifiManager.WifiLock` — lihat 6.9)

*Catatan: tidak perlu izin lokasi untuk mengambil IP lokal, asal pakai `ConnectivityManager` (lihat 6.2).*

### 6.5 Penanganan Battery/Background Restriction OEM

API standar Android (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) **tidak cukup** di banyak custom ROM, karena mereka punya lapisan pembatasan sendiri di luar AOSP:

| OEM/ROM | Fitur pembatas | Halaman setting yang perlu dibuka |
|---------|----------------|-----------------------------------|
| Xiaomi (HyperOS/MIUI) | Battery Saver + Autostart Manager | `com.miui.securitycenter` → Autostart / No restrictions |
| Oppo/Realme (ColorOS) | Battery optimization + Startup Manager | Startup Manager / "Allow background running" |
| Huawei (EMUI/MagicOS) | Battery + App Launch Manager | App Launch → Manage manually |
| Samsung (One UI) | Adaptive Battery / Sleeping apps | Battery → Never sleeping apps |

**Rekomendasi implementasi:**
- Deteksi manufacturer via `Build.MANUFACTURER` saat onboarding.
- Kalau terdeteksi OEM yang bermasalah, tampilkan dialog dengan tombol yang langsung membuka activity setting terkait (lewat `Intent` ke component yang sesuai, dengan fallback ke halaman App Info standar kalau intent tidak tersedia di versi ROM tertentu).
- Sertakan penjelasan singkat kenapa izin ini dibutuhkan (server harus tetap hidup selama proses kirim ke PS4).
- Referensi umum: pendekatan & daftar OEM ini mengikuti pola yang didokumentasikan komunitas di dontkillmyapp.com — perlu dicek berkala karena intent/activity name berubah antar versi ROM.

### 6.6 Tombol Test Koneksi

Test koneksi dirancang 2 lapis, tanpa memicu proses install:

| Test | Cara kerja | Hasil yang ditampilkan |
|------|-----------|-------------------------|
| **HTTP server lokal (di HP)** | Kirim request loopback ke `http://127.0.0.1:<port>` milik server sendiri | Server aktif & bisa serve file / server belum jalan |
| **Konektivitas ke PS4 (RPI)** | **Utama:** `GET /api/is_exists?title_id=XXXX00000_0000000000000000` (ID dummy tidak valid) ke `<IP-PS4>:12800` dengan timeout 3–5 detik. Respons JSON — apa pun bentuknya — membuktikan RPI hidup & merespons. **Fallback:** kalau respons tidak terbaca/bentuknya tak terduga, raw TCP connect ke port 12800 | RPI aktif & merespons ✅ / RPI tidak jalan (connection refused) ❌ / timeout (IP salah, beda jaringan, AP isolation, firewall) ❌ |

**Detail UX:**
- Tombol test menampilkan 3 kemungkinan status per bagian: ✅ Terhubung, ❌ Gagal (dengan pesan spesifik: refused/timeout/host unreachable), ⏳ Sedang mengecek.
- Kalau test PS4 gagal, tampilkan tips troubleshooting singkat:
  - PS4 & HP satu jaringan Wi-Fi yang sama (lewat router yang sama)?
  - RPI sedang foreground di PS4 (tidak diminimize)?
  - IP PS4 sudah benar? (cek di Settings → Network)
  - **AP/client isolation di router dimatikan?** (penyebab umum #1: router memblok traffic antar client)
- Test bisa dijalankan manual (tombol) maupun otomatis tiap kali IP PS4 diubah.

### 6.7 Kebutuhan Wajib: HTTP Range Requests + Multi-threading

Local HTTP server **wajib** mendukung `Range` request (`Accept-Ranges: bytes`, respons `206 Partial Content` + header `Content-Range`), bukan cuma serve seluruh file lewat `200 OK`. Ini bukan fitur opsional — tanpanya, alur inti aplikasi berisiko gagal total untuk file besar.

**Kenapa kritis:**
- File `.pkg` game AAA bisa puluhan GB. RPI mengambil file lewat HTTP GET dan lazim melakukan request bertahap/`Range` untuk membaca file secara progresif.
- Kalau server hanya bisa serve `200 OK` penuh tanpa `Range`, permintaan bertahap akan gagal atau server mengembalikan seluruh body — tergantung implementasi RPI, ini bisa membuat instalasi gagal start sama sekali atau tidak bisa resume kalau koneksi Wi-Fi sempat putus di tengah transfer besar.
- Tanpa dukungan `Range`, setiap gangguan jaringan kecil (radio Wi-Fi sleep sebentar, HP pindah channel, dsb.) memaksa transfer diulang dari byte 0 — untuk file 40–50GB ini sangat mahal dan meningkatkan risiko gagal total.

**Multi-threading (tambahan v0.3):**
- Dokumentasi flatz sendiri merekomendasikan server lokal yang **support multi-threading** karena PS4 membuka koneksi paralel saat mengambil file.
- Implikasi: NanoHTTPD default-nya single-threaded (async selector) — **wajib konfigurasi thread pool eksplisit**. Ktor pun perlu dispatcher yang sesuai.
- Karena koneksi paralel + range request, jangan andalkan urutan byte untuk hitung progres — progres resmi diambil dari Task Progress API (6.8); log server hanya cross-check.

**Implikasi teknis:**
- NanoHTTPD dan Ktor sama-sama bisa mendukung `Range`, tapi **tidak otomatis aktif** — perlu implementasi eksplisit di handler file serving (baca `Range` header dari request, seek ke offset yang diminta, set status `206` + `Content-Range: bytes <start>-<end>/<total>`).
- Perlu test case khusus: request dengan `Range: bytes=0-`, `Range: bytes=<mid>-<end>`, multiple concurrent range requests, dan tanpa header `Range` sama sekali — pastikan semuanya menghasilkan response yang benar.
- Item ini **Must** di MVP (Fase 1) — tanpanya, use case utama (kirim file besar) bisa tidak jalan.

### 6.8 Mekanisme Tracking Status (ditulis ulang — v0.3)

**Temuan kunci:** RPI flatz **punya Task Progress API**. Respons `POST /api/install` mengembalikan `task_id`, dan progres bisa dipolling lewat `GET /api/get_task_progress?task_id=<id>`. Ada pula `find_task` (cari task by content_id), `pause_task`, `resume_task`, `stop_task`, dan `unregister_task`. Karena itu tracking status **utama** dibangun di atas API ini, bukan dari log server.

**Model: satu task = satu batch.** Sejak keputusan di 6.1, `POST /api/install` selalu dikirim dengan seluruh file yang ditandai user dalam **satu** panggilan (array `packages`), menghasilkan **satu** `task_id` untuk keseluruhan batch — bukan satu `task_id` per file. Konsekuensinya, status & progres di UI pada dasarnya berlaku di **level batch**, bukan per file individual, kecuali format respons `get_task_progress` (belum terdokumentasi publik — lihat Section 9) ternyata memuat breakdown per-package. Sampai divalidasi empiris, asumsikan progres yang tampil adalah agregat seluruh batch, dan seluruh item dalam batch itu berpindah status bersamaan (semua "Mengunduh", semua "Terkirim ke PS4", dst).

**Rancangan state machine (per batch/task):**

| Status di UI | Sumber sinyal |
|---|---|
| Pending | Queue manager (file ditandai, batch belum dikirim) |
| Terkirim — task dibuat | Respons sukses `POST /api/install` berisi `task_id` valid (bukan `-1`, lihat baris berikut) |
| **Gagal — task_id tidak valid** | Respons `POST /api/install` mengembalikan `task_id: -1` (dilaporkan terjadi di beberapa fork RPI untuk base PKG tertentu) → gagal instan, jangan masuk siklus polling sama sekali |
| Mengunduh (X% agregat) | Polling `get_task_progress` → field progres dari respons RPI, mewakili seluruh batch |
| Terkirim ke PS4 ✅ | Task melaporkan selesai / progres 100% untuk seluruh batch. **Label UI wajib "Terkirim ke PS4", bukan "Instalasi selesai"** (lihat catatan) |
| Dijeda / Dibatalkan | User menekan pause/stop → `/api/pause_task` / `/api/stop_task` — berlaku untuk **seluruh batch**, tidak bisa pause satu file saja dalam batch yang sama |
| Gagal (setelah polling berjalan) | Respons RPI berisi error, task hilang tanpa selesai, atau polling timeout (30–60 detik tanpa respons) |

**Implikasi UX yang perlu disepakati:**
- Karena kontrol (pause/resume/stop) beroperasi di level `task_id`, user **tidak bisa** membatalkan satu file saja dari batch yang sedang berjalan — hanya bisa batalkan seluruh batch.
- File yang ditandai **setelah** batch sebelumnya dikirim tidak digabung ke task yang sedang berjalan — otomatis jadi task/batch baru (`POST /api/install` kedua, `task_id` baru) setelah batch pertama selesai/dibatalkan.
- Tombol **Kirim** sebaiknya nonaktif selama ada task yang masih berjalan, supaya tidak ada kondisi dua task aktif bersamaan yang belum diverifikasi perilakunya di RPI (lihat Section 9).

**Cross-check sekunder:** log akses HTTP server sendiri (request GET/Range dari IP PS4) dipakai sebagai validasi — misalnya memastikan progres polling konsisten dengan byte yang benar-benar ditarik. Kalau polling & log jauh berbeda, anggap polling lebih authoritative dan log untuk diagnosis.

**Interval polling:** 1–2 detik saat ada task aktif; backoff saat idle. Polling berhenti saat task selesai/gagal/dibatalkan atau saat app ditutup (resume saat app dibuka lagi via `find_task`).

**Catatan penting yang tetap berlaku (tidak berubah dari 0.2):**
- App **tidak bisa** memverifikasi secara programatik bahwa proses internal PS4 setelah unduhan (unpack, checksum, tulis storage) benar-benar sukses — batasan ini murni proses internal PS4/RPI. "Selesai" di UI = "file sudah selesai diunduh PS4 dari HP".
- **Risiko terdokumentasi:** format lengkap respons `get_task_progress` (nama field progres, skala persen, status task, dan apakah ada breakdown per-package dalam batch) tidak terdokumentasi lengkap secara publik — wajib divalidasi empiris terhadap build RPI yang dipakai (lihat Section 9). Task control (`pause_task` dsb.) masuk Fase 2 sampai validasi selesai.
- **Bukti lapangan:** ada laporan pengguna RPI fork lain mengembalikan `task_id: -1` untuk base PKG tertentu (pesan error "Invalid value for 'task_id' parameter specified") sementara DLC normal — app wajib menangani `task_id` tidak valid sebagai kegagalan instan (lihat tabel di atas), bukan diasumsikan selalu berhasil.

### 6.9 Wi-Fi Lock (High Performance Mode)

Selain Foreground Service (poin 6.2) yang menjaga proses tetap hidup, app juga wajib menahan **`WifiManager.WifiLock`** dengan mode `WIFI_MODE_FULL_HIGH_PERF` selama local HTTP server aktif mengirim file.

**Kenapa dibutuhkan terpisah dari Foreground Service:** Foreground Service mencegah *proses* dibunuh sistem, tapi tidak mencegah radio Wi-Fi masuk mode hemat daya saat layar mati/HP idle. Kalau user mengunci layar HP di tengah transfer file besar (skenario yang sangat mungkin terjadi — transfer 40GB bisa makan waktu lama), radio Wi-Fi bisa masuk power-save mode dan throughput turun drastis atau koneksi RPI ke server lokal jadi tidak stabil/timeout, meskipun proses app-nya sendiri masih hidup.

**Detail implementasi:**
- Gunakan `WIFI_MODE_FULL_HIGH_PERF`, **bukan** `WIFI_MODE_FULL_LOW_LATENCY` — mode low-latency mensyaratkan layar menyala agar aktif, sedangkan kebutuhan di sini justru sebaliknya (transfer besar sering berjalan saat layar mati/HP di-lock). High-perf mode tetap efektif walau layar mati.
- Acquire lock saat mulai serve file (request pertama dari PS4 masuk atau saat command install dikirim), release saat transfer selesai/gagal/dibatalkan — jangan pegang lock terus-menerus di luar sesi transfer aktif untuk hemat baterai.
- **Perlu permission di manifest:** `android.permission.WAKE_LOCK` — prasyarat wajib untuk `WifiLock`.
- Idealnya lifecycle WifiLock disatukan dengan lifecycle Foreground Service (acquire di `onStartCommand`/saat transfer mulai, release di `onDestroy`/saat transfer selesai) supaya tidak ada kondisi lock kepegang tapi service sudah mati atau sebaliknya.

### 6.10 Akses Storage (keputusan — baru di v0.3)

**Konteks teknis (terverifikasi):** Mulai Android 13 (API 33), `READ_EXTERNAL_STORAGE` tidak berpengaruh lagi, dan `READ_MEDIA_IMAGES/VIDEO/AUDIO` **hanya mencakup file media** — file `.pkg` bukan media, jadi tidak ada izin runtime yang bisa mengaksesnya di folder sembarang.

**Keputusan:**
| Mode | Mekanisme | Untuk siapa |
|------|-----------|-------------|
| **Default** | SAF folder picker — user menunjuk satu/ beberapa folder koleksi fPKG; app menyimpan tree permission persisten | Semua user |
| **Opsional (deep-scan)** | `MANAGE_EXTERNAL_STORAGE` (all-files access) — diminta eksplisit lewat onboarding khusus power user | User yang fPKG-nya tersebar & tidak mau pilih folder satu per satu |

**Alasan:** SAF picker UX-nya lebih aman & tidak butuh izin spesial; `MANAGE_EXTERNAL_STORAGE` tersedia bebas karena app **tidak didistribusikan via Play Store** (Google mensyaratkan approval khusus untuk izin ini — lihat 11.1). Keputusan distribusi & storage saling menguatkan.

**Implikasi scanner:**
- Scanner SAF memakai `DocumentFile`/`ContentResolver` walk pada tree yang dipilih.
- Index hasil scan (path + size + last-modified) disimpan di database lokal supaya daftar file tidak dipindai ulang penuh tiap buka app; refresh incremental.

## 7. Kebutuhan Non-Fungsional

- **Jaringan:** HP dan PS4 wajib berada di jaringan lokal yang sama — keduanya connect ke router yang sama (protokol RPI memerlukan konektivitas client-to-client melalui router; skenario hotspot HP **tidak didukung**). App perlu validasi & pesan error yang jelas kalau gagal connect (timeout, IP salah, RPI tidak aktif, AP isolation).
- **Baterai:** Server HTTP hanya aktif saat ada proses pengiriman aktif; auto-stop setelah idle beberapa menit untuk hemat baterai (sinkronkan stop ini dengan release WifiLock, 6.9 — jangan stop server saat antrian masih ada task aktif).
- **Keamanan:** Server lokal tidak perlu autentikasi (mengikuti pola aplikasi desktop aslinya) karena hanya untuk pemakaian di jaringan rumah sendiri. Mitigasi minimal: (a) server bind ke interface Wi-Fi lokal (interface default route), bukan 0.0.0.0 ke semua interface; (b) pertimbangkan token acak di path URL file (mis. `/<token>/nama.pkg`) agar device lain di LAN tidak bisa menebak URL; (c) auto-stop saat idle.
- **Kompatibilitas:** Target Android 10+ (mempertimbangkan scoped storage — solusi di 6.10).

## 8. Metrik Keberhasilan (MVP)

- **Fungsional:** 100% alur "scan file → test koneksi → kirim ke PS4 → status Terkirim ke PS4" berhasil tanpa error pada kondisi jaringan normal (Wi-Fi rumah, RPI foreground) dalam uji internal.
- **Setup:** waktu dari install app sampai siap kirim pertama (permission + folder picker + IP PS4 + test koneksi) **< 1 menit** untuk user baru.
- **Reliabilitas:** crash-free sessions **≥ 99%** selama pengujian beta.
- **Keberhasilan transfer:** ≥ 95% attempt kirim file berakhir status "Terkirim ke PS4" pada kondisi jaringan normal (kegagalan karena jaringan user tidak dihitung).
- **Resume:** transfer file > 10GB yang terputus di tengah bisa dilanjutkan tanpa mengulang dari byte 0 (validasi Range bekerja).

## 9. Risiko & Pertanyaan Terbuka

- **Format respons Task Progress API** tidak terdokumentasi lengkap secara publik — validasi empiris wajib sebelum implementasi 6.8 final; siapkan fallback ke log-server tracking kalau field progres tidak bisa diparse.
- **Granularitas progres per batch:** karena satu task mewakili banyak file sekaligus (keputusan di 6.1/6.8), progres yang bisa ditampilkan kemungkinan cuma level agregat, bukan per file — kalau respons `get_task_progress` ternyata tidak punya breakdown per-package, UI queue harus jujur menampilkan progres batch, bukan menjanjikan progres per-item yang tidak bisa dipenuhi.
- Perilaku RPI bisa berubah antar versi HEN/GoldHEN/RPI — perlu testing manual berkala.
- Perilaku RPI saat menerima `install` kedua saat task pertama masih berjalan (ditolak? diantre? menimpa?) belum diverifikasi — mitigasi desain: tombol Kirim dinonaktifkan selama task aktif (lihat 6.8), tapi skenario ini tetap perlu diuji sebagai jaring pengaman kalau ada jalur lain yang memicu install kedua. **Action item: uji di PS4 nyata di minggu pertama development.**
- Auto-discovery PS4 di jaringan (broadcast/DDP) butuh riset tambahan protokolnya — belum didokumentasikan publik selengkap `/api/install`.
- Intent/activity name untuk halaman pengaturan autostart tiap OEM tidak stabil antar versi ROM (bisa berubah/dihapus tanpa API resmi) — perlu maintenance & testing di device fisik tiap brand, bukan cuma emulator.
- ~~Keputusan SAF vs all-files access~~ → **sudah diputuskan** di 6.10.

## 10. Roadmap Bertahap

**Fase 1 (MVP):** Fitur #1–5, #11, #12 di tabel Section 4 — scan (SAF), server multi-threaded + Range, config manual, kirim install, antrian dasar dengan polling task progress, battery/OEM handling, test koneksi via `is_exists`.
**Fase 2:** Logging, uninstall + `is_exists` check, task control (#13: pause/resume/stop — setelah validasi empiris format respons), error handling lebih baik, deep-scan mode (6.10 opsional).
**Fase 3:** Auto-discovery, dukungan PS5, cover art/SFO reader, resume-antrian lintas restart app via `find_task`.

## 11. Strategi Distribusi Aplikasi

### 11.1 Google Play Store — bukan kanal utama

App ini kemungkinan besar **tidak lolos review** Google Play Store atau rawan *takedown* setelah publish, meskipun fungsinya sendiri (HTTP file server + HTTP client) generik dan tidak melanggar hukum. Alasannya:
- Keyword & deskripsi fungsional app (PS4, RPI, jailbreak/HEN/GoldHEN, pkg installer) terkait langsung dengan modifikasi console pihak ketiga — kategori yang historisnya sering kena penolakan/takedown manual di Play Store terlepas dari legalitas kode itu sendiri.
- `MANAGE_EXTERNAL_STORAGE` — yang dipakai untuk mode deep-scan (6.10) — mensyaratkan approval khusus dari Google dan terbatas untuk kategori app seperti file manager; app ini hampir pasti ditolak untuk izin tersebut.
- Risiko laporan pihak ketiga (termasuk dari pemegang platform PS4) yang memicu review ulang atau takedown mendadak setelah app sudah punya user, bukan cuma risiko di tahap submit awal.

**Keputusan yang perlu diambil sebelum coding:** treat Play Store sebagai *bukan* kanal distribusi utama sejak awal desain, bukan sebagai fallback kalau ditolak — supaya arsitektur signing & update (lihat 11.2, 11.3) tidak didesain dengan asumsi keliru "nanti tinggal upload ke Play Store". Keputusan ini sekaligus membebaskan pemakaian `MANAGE_EXTERNAL_STORAGE` untuk mode deep-scan.

### 11.2 Kanal distribusi yang direkomendasikan

- **Sideload APK langsung** — rilis lewat GitHub Releases atau halaman web milik proyek; user unduh & install manual.
- **Repo komunitas homebrew** (mis. F-Droid kalau app open-source & memenuhi kriteria mereka, atau forum komunitas PS4 jailbreak seperti GBAtemp/PSX-Place) — audiens target sudah terbiasa sideload untuk kebutuhan jailbreak lain, jadi friction lebih rendah dibanding audiens umum.

### 11.3 Implikasi teknis dari keputusan ini

- **Signing:** perlu keystore sendiri (self-managed), tidak bisa memakai Play App Signing — perlu proses aman untuk simpan & pakai keystore ini di pipeline build/release.
- **Update mechanism:** karena tidak ada auto-update dari Play Store, app perlu cek versi terbaru sendiri (misal panggil GitHub Releases API saat start) dan beri notifikasi in-app + link unduh kalau ada versi baru — kalau tidak, user lama akan stuck di versi lama tanpa sadar ada update.
- **Onboarding "unknown sources":** Android 8+ mewajibkan user mengizinkan "Install unknown apps" secara eksplisit per sumber. Perlu step onboarding yang jelasin ini (pola serupa dengan onboarding battery optimization di 6.5), termasuk untuk proses update (install ulang APK baru).
- **Verifikasi keaslian rilis:** karena app ini berpotensi jadi target pemalsuan (APK modifikasi disusupi malware, mengingat audiensnya sudah terbiasa sideload dari sumber tidak resmi), sertakan checksum (SHA-256) resmi di setiap halaman rilis supaya user bisa verifikasi APK yang mereka unduh tidak diubah pihak lain.

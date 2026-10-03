# iOS Duo Status Bar for Android

> **A compact, native Android system-telemetry overlay inspired by the visual language of premium mobile status indicators.**
>
> Built to be explored, improved, and extended together by the Android developer community.

<!-- BUILD_META_START -->
**Build terakhir:** belum tersedia — jalankan GitHub Actions untuk membuat artifact APK.  
**APK artifact:** akan dibuat otomatis oleh CI.  
**Run:** tersedia pada tab **Actions** repository.
<!-- BUILD_META_END -->

---

## ✦ Project Vision

iOS Duo Status Bar adalah proyek **open-source, community-driven** untuk menghadirkan satu indikator telemetri yang sangat ringkas di area atas layar Android.

Alih-alih memakai bitmap atau library UI pihak ketiga untuk bentuk indikator utama, proyek ini menggambar seluruh komposisi melalui **native Kotlin Canvas + vector geometry**. Tujuannya sederhana: bentuk tetap tajam pada berbagai density, animasi/perubahan status terasa instan, dan footprint aplikasi tetap kecil.

Proyek ini **tidak berafiliasi dengan Apple** dan tidak menggunakan aset proprietary Apple. Nama "Duo" mengacu pada konsep komposisi dua sisi pada indikator overlay yang menjadi inspirasi visual proyek ini.

### Design goals

- **Minimal** — hanya satu komposisi kecil untuk tiga telemetry utama.
- **Precise** — geometry dikontrol langsung melalui Canvas.
- **Elegant** — stroke tipis, rounded caps, spacing konsisten, dan micro-halo.
- **Readable** — foreground hitam/putih dipadukan dengan halo kontras.
- **Compact** — ukuran overlay dapat diturunkan hingga **24 dp**.
- **Status-aware** — battery, Wi-Fi, dan cellular signal diperbarui dari telemetry Android.
- **Community-first** — source code, dokumentasi, CI, dan roadmap terbuka untuk kontribusi.

---

## ◉ Visual Language

Komposisi indikator mengikuti struktur berikut:

```text
                    57

             ╭             ╮
             │    Wi-Fi    │
             │     ╭─╮     │
             │     ╰●╯     │
             │             │
                •  •  •  •

             BATTERY  →  side arcs
             Wi-Fi    →  3 nested arcs
             CELLULAR →  4 lower dots
```

### 1. Battery

- Persentase baterai ditampilkan sebagai angka kecil di bagian atas.
- Dua **side arcs simetris** membentuk siluet Duo.
- Panjang bagian aktif mengikuti persentase baterai.
- Track redup tetap terlihat sehingga bentuk indikator tidak kehilangan struktur ketika level baterai rendah.

### 2. Wi-Fi

- Terdiri dari **tiga nested arcs** dan satu center dot.
- Jumlah arc aktif mengikuti kekuatan RSSI Wi-Fi.
- Jika perangkat tidak terhubung ke Wi-Fi, indikator menjadi kosong/dim.

```text
Wi-Fi kuat       ╭───╮
                 ╰───╯
                   ●

Wi-Fi lemah      ╭─╮
                  ╰●

Wi-Fi disconnected
                  ·
```

### 3. Cellular

- Empat titik disusun mengikuti **lower shallow arc**.
- 0–4 titik aktif menunjukkan level sinyal operator.
- Sistem menggunakan `TelephonyManager` / `SignalStrength` untuk memperoleh level sinyal.

---

## ✧ Adaptive Contrast

Mode `AUTO` dirancang untuk menjaga ikon tetap terlihat pada wallpaper yang terang maupun gelap tanpa meminta akses screen capture.

Prioritas pemilihan warna:

1. Membaca `WallpaperColors` Android jika API mendukung.
2. Menghitung luminance relatif dari warna wallpaper.
3. Memilih foreground **putih** pada latar gelap atau **hitam** pada latar terang.
4. Menambahkan **micro-halo** berlawanan warna untuk mempertahankan edge definition.
5. Jika warna wallpaper tidak tersedia, mode terang/gelap sistem digunakan sebagai fallback.

> **Catatan teknis:** mode ini membaca karakteristik wallpaper, bukan pixel aplikasi yang sedang berada di belakang overlay. Pixel-perfect adaptation terhadap aplikasi lain memerlukan `MediaProjection`, yang sengaja tidak menjadi dependency default karena akan menambah permission, resource usage, dan kompleksitas privacy.

---

## ⚙ Overlay Configuration

Overlay dapat dikalibrasi untuk notch, punch-hole, atau status-bar layout perangkat yang berbeda.

| Parameter | Rentang | Fungsi |
|---|---:|---|
| **Ukuran ikon** | 24–100 dp | Mengatur footprint indikator |
| **Offset kanan X** | 0–100 dp | Menggeser indikator dari tepi kanan |
| **Offset vertikal Y** | −48–80 dp | Memungkinkan indikator dinaikkan masuk ke area status bar |
| **Opasitas** | 20–100% | Mengatur transparansi overlay |

Nilai default dirancang sebagai titik awal untuk perangkat dengan punch-hole/status-bar modern, tetapi posisi terbaik tetap bergantung pada OEM, density, dan konfigurasi status bar perangkat.

---

## 🏗 Architecture

```text
Android System Telemetry
          │
          ├── BatteryManager
          ├── ConnectivityManager / WifiManager
          └── TelephonyManager / SignalStrength
          │
          ▼
┌──────────────────────────────┐
│    SystemStatusMonitor       │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│ StatusBarOverlayService      │
│ Foreground + System Overlay   │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│ CircularStatusView            │
│ Native Canvas / Vector        │
│ Geometry / Adaptive Contrast  │
└──────────────────────────────┘
```

### Repository structure

```text
app/src/main/java/com/example/iosstatusbar/
├── CircularStatusView.kt
│   └── Premium Canvas renderer
├── MainActivity.kt
│   └── Configuration / permissions / controls
├── StatusBarOverlayService.kt
│   └── Foreground overlay lifecycle
└── SystemStatusMonitor.kt
    └── Battery / Wi-Fi / cellular telemetry

.github/workflows/android.yml
└── Automated CI/CD build + artifact + README metadata
```

---

## 🔐 Permissions & Privacy

The application uses permissions required by the overlay and telemetry architecture:

- `SYSTEM_ALERT_WINDOW` — menampilkan indikator di atas aplikasi lain.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — menjaga lifecycle overlay pada Android modern.
- `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` — membaca konektivitas dan Wi-Fi RSSI.
- `READ_PHONE_STATE` — memperoleh cellular signal information.
- `POST_NOTIFICATIONS` — notifikasi foreground service pada Android yang mendukung permission tersebut.

**Tidak ada screen recording sebagai bagian dari arsitektur default.**

---

## 🚀 Build Locally

Requirements:

- Android Studio modern
- JDK 17
- Android SDK 35
- Gradle Wrapper generated/committed by the project

```bash
git clone https://github.com/<YOUR-OWNER>/<YOUR-REPOSITORY>.git
cd <YOUR-REPOSITORY>

./gradlew lintRelease
./gradlew assembleRelease
```

APK output:

```text
app/build/outputs/apk/release/app-release.apk
```

---

## 🤖 GitHub Actions CI/CD

Setiap push ke branch utama/development atau `workflow_dispatch` dapat menjalankan pipeline:

```text
Checkout
   ↓
JDK 17 + Gradle
   ↓
Lint Release
   ↓
Release Signing / Debug Fallback
   ↓
assembleRelease
   ↓
APK Naming + SHA-256
   ↓
Upload Artifact
   ↓
README Build Metadata
```

### Release signing

Untuk menghasilkan APK release dengan signing key milik project, buat GitHub Actions Secrets berikut:

```text
RELEASE_KEYSTORE_B64
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Tanpa secrets tersebut, CI menggunakan debug keystore agar contributor tetap dapat memperoleh APK installable untuk testing.

> Untuk distribusi production, gunakan release keystore yang disimpan sebagai GitHub Secret. Jangan commit keystore atau password ke repository.

---

## 🌍 Community Collaboration

Proyek ini sengaja dibuat sebagai **open engineering project**, bukan sekadar APK sekali jadi.

Kami mengundang developer, UI/UX designer, Android performance engineer, QA tester, technical writer, dan contributor dari berbagai negara untuk membantu mengembangkan indikator ini.

### Contribution ideas

- 📐 penyempurnaan vector geometry
- 📱 optimasi positioning untuk Samsung / Pixel / Xiaomi / OnePlus dan OEM lainnya
- 🎨 adaptive contrast dan color science
- 📡 peningkatan akurasi Wi-Fi/cellular telemetry
- ⚡ rendering & battery-efficiency profiling
- ♿ accessibility dan readability
- 🧪 automated UI/device testing
- 📚 dokumentasi dan multilingual README
- 🐛 bug reports dan reproducible test cases

### Contribution workflow

```bash
git checkout -b feature/your-improvement

# implement + test
./gradlew lintRelease
./gradlew assembleRelease

git add .
git commit -m "feat: describe your improvement"
git push origin feature/your-improvement
```

Kemudian buat **Pull Request** ke branch `main` dan jelaskan:

1. masalah yang ingin diselesaikan;
2. pendekatan teknis yang digunakan;
3. perangkat/API level yang diuji;
4. dampak terhadap performance;
5. screenshot/video jika perubahan bersifat visual.

### Community principle

> **Measure first. Render precisely. Document clearly. Improve together.**

Setiap kontribusi yang baik seharusnya membuat proyek ini lebih akurat, lebih ringan, lebih kompatibel, atau lebih mudah dikembangkan oleh contributor berikutnya.

---

## 🗺 Roadmap

### Phase 1 — Core Overlay

- [x] Native Canvas renderer
- [x] Battery percentage + Duo side arcs
- [x] Wi-Fi 3-level visual telemetry
- [x] Cellular 4-dot telemetry
- [x] Compact 24–100 dp scaling
- [x] Negative Y positioning
- [x] Adaptive wallpaper contrast
- [x] Foreground service architecture

### Phase 2 — Device Excellence

- [ ] Device-specific calibration profiles
- [ ] Punch-hole / notch presets
- [ ] More precise cellular state handling
- [ ] Performance profiling on multiple OEMs
- [ ] Automated screenshot regression tests

### Phase 3 — Advanced Visual Engine

- [ ] Optional pixel-aware contrast experiment using MediaProjection
- [ ] Smooth telemetry transitions
- [ ] More refined color adaptation
- [ ] User-selectable visual themes
- [ ] Accessibility-aware scaling

Advanced features will be introduced only when they can improve the experience without unnecessarily increasing permission scope or resource consumption.

---

## 📜 License

This project is distributed under the **MIT License**. See `LICENSE` for details.

---

## Disclaimer

This project is an independent open-source Android project. It is **not affiliated with, endorsed by, or sponsored by Apple Inc.** or any other device manufacturer.

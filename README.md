# iOS Duo Status Bar for Android

> **A compact, native Android telemetry overlay inspired by the visual language of premium mobile status indicators.**
>
> **Measure first. Render precisely. Document clearly. Improve together.**

<!-- BUILD_META_START -->
**Build terakhir:** akan diperbarui otomatis oleh GitHub Actions.  
**Release:** `5.2.1`  
**Artifact:** tersedia pada tab **Actions → Artifacts** setelah workflow selesai.
<!-- BUILD_META_END -->

---

## ✦ Project Vision

iOS Duo Status Bar adalah proyek **open-source, community-driven** untuk menghadirkan indikator telemetri sistem yang ringkas, tajam, dan dapat dikalibrasi di area atas layar Android.

Seluruh indikator utama dirender menggunakan **native Kotlin Canvas + vector geometry**. Tidak ada bitmap telemetry untuk rendering overlay. Tujuannya adalah mempertahankan ketajaman pada berbagai density, menjaga penggunaan resource tetap rasional, dan memberi ruang bagi komunitas untuk bereksperimen dengan geometry, telemetry, dan perangkat yang berbeda.

Proyek ini **tidak berafiliasi dengan Apple**, tidak menggunakan aset proprietary Apple, dan menggunakan istilah “Duo” sebagai nama konsep visual komposisi indikator.

## ✦ Visual System

```text
                    57       Wi-Fi*

              ╭────────────╮  ╭──╮
            ╭                ╮╭────╮
           │                  ╰──●─╯
            ╰                ╯
              •    •    •    •

       Battery  → side arcs + percentage
       Wi-Fi    → 3 nested arcs + center dot
       Cellular → 4 lower dots

* Camera Hole Mode: Wi-Fi berada di luar ring dan sejajar
  horizontal dengan angka baterai.
```

### Battery
- Angka persentase berada di atas ring.
- Dua side-arc simetris membentuk satu indikator baterai.
- Panjang arc aktif mengikuti persentase baterai.
- Ketebalan arc, ukuran angka, dan weight angka dapat diatur independen.

### Wi-Fi
- Tiga nested arcs menunjukkan level RSSI.
- Terhubung: jumlah arc aktif mengikuti kekuatan jaringan.
- Tidak terhubung: indikator menjadi dim/kosong.
- **Camera Hole Mode** memindahkan Wi-Fi ke sisi angka baterai pada satu garis horizontal, sehingga pusat ring dapat dibiarkan kosong untuk punch-hole kamera.

### Cellular
- Empat titik mengikuti kurva bawah.
- Jumlah titik aktif mengikuti level sinyal operator.
- Ukuran titik dapat dikalibrasi agar sebanding dengan status-bar icon perangkat.

## ✦ Premium Calibration

v5.2.1 memisahkan parameter visual yang sebelumnya saling bergantung:

| Parameter | Fungsi |
|---|---|
| Icon Size | Skala keseluruhan overlay |
| Arc Thickness | Ketebalan battery ring |
| Battery Text Size | Ukuran angka persentase |
| Battery Text Weight | Ketebalan angka |
| Wi-Fi Stroke | Ketebalan Wi-Fi |
| Wi-Fi Offset | Posisi horizontal Wi-Fi pada Camera Hole Mode |
| Cellular Dot Size | Ukuran titik sinyal operator |
| X / Y Offset | Kalibrasi posisi terhadap status bar, notch, atau punch-hole |
| Opacity | Transparansi overlay |

**Prinsip desain:** jangan memperbesar seluruh icon hanya untuk membuat stroke terbaca. Gunakan parameter detail untuk mendapatkan komposisi **small-but-bold**.

## ✦ Cara Menggunakan

1. Buka aplikasi.
2. Tekan **START**.
3. Izinkan **Tampil di atas aplikasi lain**.
4. Atur **Icon Size**, X/Y, dan opacity.
5. Cocokkan **Arc Thickness** dan **Battery Text** dengan icon status bar bawaan perangkat.
6. Jika kamera punch-hole berada di tengah ring, aktifkan **Wi-Fi di luar ring**.
7. Geser **Wi-Fi Offset** sampai Wi-Fi sejajar dengan angka baterai.
8. Tekan **SIMPAN & TERAPKAN**.

> **Tip:** lakukan kalibrasi sambil membuka wallpaper/aplikasi dengan background terang dan gelap. Tujuannya mencari konfigurasi yang tetap terbaca pada keduanya.

## ✦ Adaptive Contrast

AUTO Contrast menggunakan informasi wallpaper/system appearance yang tersedia Android untuk memilih foreground terang atau gelap, lalu memakai micro-halo untuk menjaga separation pada background kompleks.

Aplikasi **tidak menggunakan screen capture** untuk fungsi ini.

## ✦ Privacy & Permissions

Permission digunakan sesuai fungsi:

- `SYSTEM_ALERT_WINDOW` — menampilkan overlay di atas aplikasi lain.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — menjaga telemetry overlay tetap aktif sebagai foreground service.
- `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` — membaca status dan kekuatan Wi-Fi.
- `READ_PHONE_STATE` — membaca informasi sinyal seluler yang diperlukan indikator cellular.
- `POST_NOTIFICATIONS` — notification foreground service pada versi Android yang memerlukannya.

Project tidak membutuhkan akses kontak, SMS, kamera, mikrofon, atau penyimpanan pribadi untuk fungsi telemetry utama.

## ✦ Architecture

```text
MainActivity
    │
    ├── User configuration
    ├── Permission flow
    └── SharedPreferences
            │
            ▼
StatusBarOverlayService
    │
    ├── Foreground Service
    ├── WindowManager Overlay
    └── CircularStatusView
            │
            └── Native Canvas Renderer
                    ├── Battery
                    ├── Wi-Fi
                    ├── Cellular
                    └── Adaptive Contrast

SystemStatusMonitor
    ├── Battery
    ├── Wi-Fi
    └── Cellular
```

## ✦ Repository Structure

```text
.github/workflows/android.yml
app/
├── src/main/java/com/example/iosstatusbar/
│   ├── CircularStatusView.kt
│   ├── MainActivity.kt
│   ├── StatusBarOverlayService.kt
│   └── SystemStatusMonitor.kt
└── src/main/res/
    ├── drawable/
    ├── layout/
    ├── mipmap-anydpi-v26/
    └── values/
README.md
build.gradle
settings.gradle
gradle.properties
```

## ✦ Local Development

```bash
git clone https://github.com/<owner>/<repository>.git
cd <repository>
./gradlew :app:lintRelease
./gradlew :app:assembleRelease
```

## ✦ Continuous Integration

GitHub Actions menjalankan:

```text
Checkout
   ↓
JDK 17 + Gradle
   ↓
Android Lint
   ↓
Release Build
   ↓
APK Verification
   ↓
SHA-256
   ↓
Artifact Upload
   ↓
README Build Metadata
```

Production signing dapat menggunakan GitHub Secrets:

```text
RELEASE_KEYSTORE_B64
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Jika secret release belum tersedia, workflow dapat menggunakan debug signing untuk testing sesuai konfigurasi repository.

## ✦ Community Collaboration

Proyek ini dibangun dengan semangat **global open-source collaboration**. Kontribusi tidak harus berupa fitur besar. Issue kecil tentang density, notch, punch-hole, OEM behavior, accessibility, telemetry accuracy, rendering, dokumentasi, dan testing perangkat sangat bernilai.

### Contribution workflow

1. Fork repository.
2. Buat branch:
   `feature/<nama-fitur>`
3. Jelaskan masalah dan alasan perubahan.
4. Implementasikan perubahan sekecil dan sejelas mungkin.
5. Jalankan lint dan build.
6. Sertakan screenshot sebelum/sesudah jika perubahan bersifat visual.
7. Buka Pull Request ke `main`.

### Community principles

- **Evidence over assumption** — ukur pada perangkat nyata jika membahas geometry atau telemetry.
- **Small, reviewable changes** — hindari PR besar yang mencampur banyak tujuan.
- **Document the why** — jelaskan alasan desain, bukan hanya perubahan kode.
- **Respect device diversity** — Android memiliki banyak density, OEM, notch, dan punch-hole layout.
- **Build together** — kritik teknis harus membantu proyek menjadi lebih baik.

## ✦ Roadmap

- [x] Native Canvas telemetry renderer
- [x] Battery side arcs
- [x] Wi-Fi RSSI indicator
- [x] Cellular dot indicator
- [x] Adjustable stroke/text calibration
- [x] Camera Hole Mode
- [x] Adaptive contrast without screen capture
- [x] Premium configuration UI
- [ ] Device-specific layout profiles
- [ ] Automated visual regression tests
- [ ] Broader OEM telemetry compatibility
- [ ] Release distribution through official Android channels

## License

MIT License. See `LICENSE` when included by the repository maintainer.

---

**iOS Duo Status Bar — an open experiment in precise, compact Android telemetry UI.**

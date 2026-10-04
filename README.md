# iOS Duo Status Bar for Android

> **A compact, native Android telemetry overlay inspired by premium mobile status-indicator language.**
>
> **Measure first. Render precisely. Document clearly. Improve together.**

<!-- BUILD_META_START -->
**Build terakhir:** akan diperbarui otomatis oleh GitHub Actions.  
**Release:** `5.3.1`  
**Artifact:** tersedia di **Actions → Artifacts** setelah workflow selesai.
<!-- BUILD_META_END -->

## Project Vision

iOS Duo Status Bar adalah proyek **open-source, community-driven, dan device-oriented** untuk menghadirkan indikator telemetri sistem yang ringkas, tajam, dan dapat dikalibrasi di area atas layar Android.

Proyek dibangun dari **native Kotlin + Canvas + vector geometry**. Telemetry overlay tidak memakai bitmap sebagai sumber rendering, sehingga geometry dapat diskalakan tanpa kehilangan bentuk dasar.

Proyek ini tidak berafiliasi dengan Apple dan tidak menggunakan aset proprietary Apple. Nama **Duo** mengacu pada komposisi visual indikator yang menggabungkan battery, Wi-Fi, dan cellular dalam satu mark.

## Visual System

```text
                     35       Wi-Fi
                              ╭───╮
                             ╭─────╮
                                ●

                  ╭────────────────────╮
                ╭                        ╮
               │        CAMERA HOLE      │
                ╰                        ╯
                   • • • •
                   • • • •
                 SIM 1   SIM 2
```

### Battery

- Angka persentase berada di atas ring.
- Arc kiri dan kanan adalah **satu indikator baterai**; keduanya mengikuti level baterai yang sama.
- Panjang arc aktif mengikuti persentase baterai.
- Ketebalan arc dan tipografi dapat dikalibrasi secara independen.

### Wi-Fi

- Tiga nested arcs menunjukkan level sinyal Wi-Fi.
- Saat Wi-Fi terhubung, jumlah arc aktif mengikuti level telemetri.
- Saat Wi-Fi tidak terhubung, glyph menjadi dim/kosong.
- **Standard Mode:** Wi-Fi ditempatkan tepat di pusat ring.
- **Camera Hole Mode:** Wi-Fi dipindahkan ke luar ring dan dibuat sejajar dengan angka baterai. Angka + Wi-Fi membentuk pasangan yang simetris terhadap pusat ring sehingga area punch-hole dapat tetap kosong.
- Ukuran glyph dan ketebalan stroke Wi-Fi dapat diatur terpisah.

### Cellular / Dual SIM

- Satu SIM digambarkan dengan **4 titik**.
- **SIM 1 / primary** berada pada baris atas.
- **SIM 2 / secondary** berada pada baris bawah.
- Jarak antartitik dibuat lebih longgar untuk menjaga keterbacaan pada ukuran overlay kecil.
- Bila SIM kedua tidak tersedia, baris kedua tetap menjadi track redup sehingga struktur Duo tidak berubah.

## Premium Calibration

Gunakan kontrol detail untuk mencocokkan icon dengan status-bar icon bawaan perangkat tanpa harus memperbesar seluruh overlay.

| Parameter | Fungsi |
|---|---|
| Icon Size | Skala keseluruhan overlay |
| Arc Thickness | Ketebalan battery ring |
| Battery Text Size | Ukuran angka persentase |
| Battery Text Weight | Bobot tipografi angka |
| Wi-Fi Stroke | Ketebalan glyph Wi-Fi |
| Wi-Fi Size | Skala glyph Wi-Fi tanpa mengubah ring |
| Cellular Dot Size | Diameter titik SIM |
| Wi-Fi Distance | Penyesuaian jarak pasangan angka + Wi-Fi pada Camera Hole Mode |
| X / Y Offset | Kalibrasi terhadap status bar, notch, atau punch-hole |
| Opacity | Transparansi overlay |

### Rekomendasi awal

Untuk perangkat modern, mulai dari **30–34 dp**. Kemudian sesuaikan:

1. Arc Thickness.
2. Battery Text Size dan Weight.
3. Wi-Fi Size dan Stroke.
4. Cellular Dot Size.
5. Baru terakhir X/Y Offset.

Dengan urutan tersebut, pengguna tidak perlu mengubah posisi setiap kali ingin memperbaiki keterbacaan icon.

## Cara Menggunakan

1. Buka aplikasi.
2. Tekan **START**.
3. Berikan izin **Tampil di atas aplikasi lain**.
4. Atur ukuran dan posisi overlay.
5. Samakan ketebalan arc serta angka dengan icon status bar perangkat.
6. Untuk punch-hole di tengah ring, aktifkan **Wi-Fi di luar ring**.
7. Atur ukuran/ketebalan Wi-Fi dan jarak pasangan angka + Wi-Fi.
8. Tekan **SIMPAN & TERAPKAN**.

> **Tip:** kalibrasi pada wallpaper terang dan gelap. Jangan hanya mengoptimalkan satu kondisi background.

## Adaptive Contrast

Adaptive contrast pada versi ini **tidak menggunakan MediaProjection atau screen recording**.

- API 27+: `WallpaperColors` dipakai sebagai referensi luminance.
- API 24–26: renderer menggunakan fallback system appearance.
- Foreground dan micro-halo dibuat sebagai dua elemen berbeda sehingga bentuk tetap tajam tanpa shadow besar.

Tujuan desainnya adalah **small-but-bold**: overlay tetap compact, tetapi garis dan angka tidak ikut menghilang ketika ukuran icon dikecilkan.

## Architecture

```text
app/src/main/java/com/example/iosstatusbar/
│
├── CircularStatusView.kt
│   └── Canvas geometry, typography, adaptive contrast
│
├── SystemStatusMonitor.kt
│   └── Battery + Wi-Fi + dual-SIM telemetry
│
├── StatusBarOverlayService.kt
│   └── Foreground overlay lifecycle + WindowManager layout
│
└── MainActivity.kt
    └── Configuration UI + persistent preferences
```

## Engineering Principles

**Native first.** Rendering tetap menggunakan Canvas dan primitive geometry.

**Telemetry separated from rendering.** Pengambilan status sistem dipisahkan dari renderer agar debugging dan pengembangan perangkat berbeda tetap terukur.

**Device calibration over assumptions.** Punch-hole, density, status-bar inset, dan perilaku OEM dapat berbeda. Aplikasi menyediakan kontrol agar posisi dapat dikalibrasi dari perangkat nyata.

**No unnecessary screen capture.** Versi utama tidak merekam layar dan tidak memerlukan MediaProjection hanya untuk adaptive contrast.

**Lint must remain meaningful.** Compatibility issue diperbaiki melalui resource/API guards, bukan dengan mematikan lint.

## Local Development

```bash
git clone https://github.com/<owner>/<repository>.git
cd <repository>
./gradlew :app:lintRelease
./gradlew :app:assembleRelease
```

## GitHub Actions

Workflow berada di:

```text
.github/workflows/android.yml
```

Pipeline utama:

```text
Checkout
   ↓
JDK 17
   ↓
Gradle
   ↓
Lint Release
   ↓
Release Build
   ↓
APK Verification
   ↓
SHA-256
   ↓
Upload Artifact
   ↓
README build metadata
```

Release signing dapat menggunakan GitHub Secrets:

```text
RELEASE_KEYSTORE_B64
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Tanpa release keystore secret, workflow dapat memakai debug signing untuk menghasilkan artifact installable untuk pengujian. Untuk distribusi produksi, gunakan release keystore yang dikelola dengan aman dan konsisten.

## Community Collaboration

Proyek ini dibangun dengan semangat **open engineering**: perangkat nyata, pengujian lintas OEM, feedback visual, dan dokumentasi komunitas adalah bagian dari proses pengembangan.

Kontribusi yang paling bernilai bukan hanya menambah fitur, tetapi juga memperbaiki akurasi telemetry, kompatibilitas perangkat, accessibility, efisiensi rendering, dokumentasi, dan automated testing.

### Contribution Flow

```text
Issue / Observation
        ↓
Reproduce on device
        ↓
Measure & document
        ↓
Implement minimal change
        ↓
Lint + build
        ↓
Pull Request
        ↓
Community review
```

Saat membuka issue, sertakan bila memungkinkan:

- model perangkat;
- versi Android;
- ukuran/density display;
- screenshot sebelum dan sesudah;
- konfigurasi overlay;
- log atau lint error yang relevan.

Dengan data tersebut, perubahan dapat dibahas berdasarkan bukti, bukan asumsi.

## Roadmap

- [x] Native Canvas renderer
- [x] Adjustable arc thickness
- [x] Adjustable battery text size and weight
- [x] Adjustable Wi-Fi size and stroke
- [x] Camera Hole Mode dengan pasangan angka + Wi-Fi simetris
- [x] Dual-SIM indicator rows
- [x] API-safe adaptive contrast
- [x] GitHub Actions validation and release artifact
- [ ] Device-specific calibration profiles
- [ ] Automated visual regression tests
- [ ] Broader OEM telemetry compatibility
- [ ] Official distribution and release-channel hardening

## Privacy & Permissions

Aplikasi membutuhkan permission yang relevan untuk fungsi overlay dan telemetri sistem, termasuk:

- `SYSTEM_ALERT_WINDOW` untuk overlay;
- `FOREGROUND_SERVICE` untuk menjaga service aktif;
- `ACCESS_NETWORK_STATE` dan `ACCESS_WIFI_STATE` untuk status Wi-Fi;
- `READ_PHONE_STATE` untuk telemetri sinyal SIM;
- notification permission pada Android yang membutuhkannya untuk foreground service notification.

Aplikasi tidak menggunakan MediaProjection untuk menggambar overlay maupun untuk adaptive contrast pada versi ini.

## License

MIT License. Lihat file `LICENSE` pada repository untuk teks lisensi lengkap.

---

**iOS Duo Status Bar — a community experiment in precise, compact Android telemetry UI.**

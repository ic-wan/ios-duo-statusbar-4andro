# 📱 iOS Duo Status Bar for Android

[![Build Status](https://github.com/ic-wan/ios-duo-statusbar-4andro/actions/workflows/main.yml/badge.svg)](https://github.com/ic-wan/ios-duo-statusbar-4andro/actions)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API Level](https://img.shields.io/badge/API-24%2B-blue.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**iOS Duo Status Bar** adalah aplikasi Android berbasis *System Alert Window (Overlay)* yang dirancang untuk menghadirkan tampilan status bar terpadu bergaya iOS Duo secara presisi dan elegan di sudut atas layar Android Anda.

Proyek ini dibuat secara terstruktur, bersih, dan modular menggunakan Kotlin serta Android Jetpack komponen dasar, sehingga sangat terbuka untuk dikembangkan bersama oleh komunitas (*open-source collaborative project*).

---

## 📥 Download APK (Build Terakhir)

Anda dapat mengunduh APK hasil kompilasi otomatis (*CI/CD Build*) secara langsung melalui tautan berikut:

🔗 **[Halaman Artifact Download Build Terakhir (iOS_Duo_Status_Bar_20261004_000111.apk)](https://github.com/ic-wan/ios-duo-statusbar-4andro/actions/runs/37138862159)**

> 💡 **Cara Mengunduh:**
> 1. Klik tautan di atas untuk menuju halaman detail *GitHub Actions Run*.
> 2. Gulir layar ke bagian paling bawah pada seksi **Artifacts**.
> 3. Klik pada file **`iOS_Duo_Status_Bar_20261004_000111.apk`** (atau file  artifact) untuk mengunduhnya langsung ke perangkat Anda.

---

## 📊 Informasi Kompilasi Saat Ini

| Parameter | Detail Spesifikasi |
| :--- | :--- |
| **Status Build** | ✅ Sukses Diselesaikan |
| **Waktu Kompilasi** |  |
| **Nama File Output** |  |
| **Target SDK** | Android 14 (SDK 34) |
| **Minimum SDK** | Android 7.0 Nougat (SDK 24) |
| **Environment** | Java 17 + Gradle 8.2 + Kotlin 1.9.22 |

---

## ✨ Fitur Utama

* **Compact Floating Overlay**: Menampilkan bulatan status bar minimalis di sudut kanan atas layar tanpa mengganggu navigasi aplikasi lain.
* **Real-time Battery Monitor**: Menampilkan persentase daya baterai secara akurat dan langsung terhubung dengan  Android.
* **Wi-Fi & Cellular Signal Indicators**: Indikator ikon Wi-Fi dan 4-dot sinyal GSM bergaya khas iOS.
* **Custom Control Configuration**:
  * **X Offset Slider**: Mengatur pergeseran horizontal ikon status bar.
  * **Y Offset Slider**: Mengatur pergeseran vertikal sesuai dengan ukuran *notch* atau *punch-hole* HP.
  * **Opacity / Alpha Slider**: Mengatur tingkat transparansi tampilan dari 0% hingga 100%.
* **Persistensi Pengaturan**: Nilai posisi dan opasitas tersimpan otomatis di .

---

## 🏗️ Arsitektur & Struktur Proyek

Aplikasi ini dirancang dengan arsitektur modular yang memisahkan antara antarmuka konfigurasi, *Service* tampilan overlay, dan *BroadcastReceiver* pemantau sistem.



### Komponen Tampilan (XML Resources):
* : Tampilan layout utama bulatan status bar.
* : Shape drawable dengan border tipis elegan.
*  & : Vector drawable independen untuk indikator sinyal.

---

## 🛠️ Panduan Pengembangan & Kontribusi (Collaborators Welcome!)

Proyek ini sangat membutuhkan kontribusi dari pengembang Android lain untuk memperkaya fitur dan kompatibilitas perangkat!

### Prasyarat Pengembangan Lokal:
1. **JDK 17** atau yang lebih baru.
2. **Android Studio** (Hedgehog / Jellyfish / versi lebih baru direkomendasikan).
3. **Android SDK 34**.

### Langkah Memulai Pengembangan:


### Alur Kontribusi (*Pull Request*):
1. **Fork** repositori ini.
2. Buat *feature branch* baru ().
3. Commit perubahan Anda ().
4. Push branch Anda ().
5. Buka **Pull Request** ke branch .

---

## 🗺️ Peta Jalan Fitur (Roadmap)

Berikut adalah beberapa fitur yang direncanakan untuk versi mendatang (Anda sangat disarankan membantu mengerjakan poin berikut):

- [ ] **Dynamic Notch Auto-alignment**: Penyesuaian otomatis posisi berdasarkan koordinat *DisplayCutout* HP.
- [ ] **Interactive Gestures**: Aksi *double tap* atau *long press* pada overlay untuk membuka Quick Settings.
- [ ] **Real Wi-Fi/GSM Receiver**: Pemantauan level dBm sinyal secara dinamik (*SignalStrength API*).
- [ ] **Theme Personalization**: Pilihan warna latar belakang dan warna ikon kustom.
- [ ] **Auto-start on Boot**: Memulai service secara otomatis saat HP baru dinyalakan ().

---

## 🔒 Permintaan Izin (Permissions)

Aplikasi ini memerlukan izin khusus berikut agar dapat berjalan dengan normal:
* : Untuk menampilkan overlay di atas aplikasi lain.
* : Menjaga agar tampilan overlay tidak dimatikan oleh OS.
*  & : Untuk membaca status koneksi jaringan.

---

## 📄 Lisensi

Didistribusikan di bawah Lisensi **MIT**. Lihat file  untuk informasi lebih lanjut.

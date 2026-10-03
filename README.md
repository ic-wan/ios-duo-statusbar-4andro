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

🔗 **[Halaman Artifact Download Build Terakhir (iOS_Duo_Status_Bar_20261004_001633.apk)](https://github.com/ic-wan/ios-duo-statusbar-4andro/actions/runs/37139761234)**

> 💡 **Cara Mengunduh:**
> 1. Klik tautan di atas untuk menuju halaman detail *GitHub Actions Run*.
> 2. Gulir layar ke bagian paling bawah pada seksi **Artifacts**.
> 3. Klik pada file **`iOS_Duo_Status_Bar_20261004_001633.apk`** (atau file  artifact) untuk mengunduhnya langsung ke perangkat Anda.

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

* **Compact Circular Overlay**: Menampilkan bulatan status bar sempurna di sudut kanan atas layar tanpa mengganggu navigasi.
* **Dynamic Battery Arc**: Garis busur setengah lingkaran atas melengkung secara dinamik (*0°-180°*) mengikuti sisa persentase daya baterai.
* **Wi-Fi & Cellular Signal Indicators**: Indikator kekuatan sinyal Wi-Fi di tengah dan busur sinyal GSM di setengah lingkaran bawah.
* **Custom Control Configuration**:
  * **X Offset Slider**: Mengatur pergeseran horizontal ikon status bar.
  * **Y Offset Slider**: Mengatur pergeseran vertikal sesuai dengan ukuran *notch* atau *punch-hole* HP.
  * **Opacity / Alpha Slider**: Mengatur tingkat transparansi tampilan dari 0% hingga 100%.
* **Persistensi Pengaturan**: Nilai posisi dan opasitas tersimpan otomatis di .

---

## 🏗️ Arsitektur & Struktur Proyek

Aplikasi ini dirancang dengan arsitektur modular yang memisahkan antara antarmuka konfigurasi, *Service* tampilan overlay, dan *Custom View* khusus penampil indikator.



---

## 🛠️ Panduan Pengembangan & Kontribusi (Collaborators Welcome!)

Proyek ini sangat membutuhkan kontribusi dari pengembang Android lain untuk memperkaya fitur dan kompatibilitas perangkat!

### Langkah Memulai Pengembangan:


---

## 📄 Lisensi

Didistribusikan di bawah Lisensi **MIT**. Lihat file  untuk informasi lebih lanjut.

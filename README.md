# 📱 iOS Duo Status Bar for Android (Enterprise Collaborative Edition)

[![Build Status](https://github.com/ic-wan/ios-duo-statusbar-4andro/actions/workflows/main.yml/badge.svg)](https://github.com/ic-wan/ios-duo-statusbar-4andro/actions)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API Level](https://img.shields.io/badge/API-24%2B-blue.svg)](https://android-arsenal.com/api?level=24)
[![Language](https://img.shields.io/badge/Language-Kotlin%201.9.22-orange.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**iOS Duo Status Bar** adalah proyek *open-source collaborative enterprise-grade* Android berbasis *System Alert Window (Overlay)* yang secara presisi menghadirkan indikator telemetri sistem terpadu bergaya iOS Duo di sudut atas layar Android Anda.

Proyek ini dibangun dari dasar menggunakan **Pure Native Kotlin Canvas Drawing & Vector Geometry**, tanpa keterbatasan library UI Pihak Ketiga, sehingga menghasilkan performa rendering ultra-ringan, penggunaan RAM minimalis (<15 MB), serta respon time instan terhadap dinamika baterai dan konektivitas jaringan.

---

## 📥 Download APK (CI/CD Automated Build)

Anda dapat mengunduh APK siap pakai hasil kompilasi otomatis (*CI/CD Pipeline*) melalui tautan resmi di bawah ini:

🔗 **[Halaman Artifact Download Build Terakhir (iOS_Duo_Status_Bar_20261004_011215.apk)](https://github.com/ic-wan/ios-duo-statusbar-4andro/actions/runs/37143169835)**

> 💡 **Petunjuk Unduh & Instalasi:**
> 1. Klik tautan di atas untuk membuka halaman *GitHub Actions Run Detail*.
> 2. Gulir ke bagian **Artifacts** di bagian paling bawah halaman.
> 3. Unduh berkas **`iOS_Duo_Status_Bar_20261004_011215.apk`**.
> 4. Jalankan instalasi di perangkat Android Anda (Mendukung Android 7.0 hingga Android 14/15 seperti Samsung Galaxy S25).

---

## 📊 Informasi Kompilasi Pipeline Saat Ini

| Parameter Pipeline | Spesifikasi Kompilasi |
| :--- | :--- |
| **Status Pipeline** | ✅ **SUCCESSFULLY COMPILED** |
| **Waktu Build (WIB)** |  |
| **Nama File Output** |  |
| **Target SDK** | Android 14 (API Level 34) |
| **Minimum Compatibility** | Android 7.0 Nougat (API Level 24) |
| **Compiler Runtime** | Java 17 Temurin + Gradle 8.2 + AGP 8.2.2 |
| **Digital Signature** | V2/V3 Signed () |

---

## ✨ Desain & Fitur Arsitektur Utama



1. **Symmetrical Dual-Bracket Battery Arc**:
   - Garis busur melengkung ganda di sisi Kiri dan Kanan () yang secara teratur terisi dari bawah ke atas sesuai daya baterai.
   - Memiliki *dim-track translucent arc* di latar belakang agar bentuk busur utuh bergaya Apple Watch selalu membayang rapi.
2. **Adaptive Contrast Outer Halo**:
   - Menggunakan teknik rendering *Dual-pass Stroke Halo Shadow*.
   - Menjamin seluruh garis, angka, dan titik indikator **tetap kontras dan 100% terbaca jelas di atas wallpaper apa pun** (gelap, terang, maupun *colorful*).
3. **Vector Wi-Fi Signal Telemetry**:
   - Tampilan busur sinyal Wi-Fi di posisi pusat secara instan menyesuaikan dengan indeks RSSI jaringan (*Signal Level 0..4*).
4. **Curved Cellular GSM Signal Dots**:
   - 4 titik indikator sinyal seluler yang tersusun melengkung presisi mengikuti kontur busur lingkaran bawah.
5. **Interaktif Granular Overlay Control**:
   - **Diameter Size Slider**: Mengatur skala ukuran widget dari 20 dp hingga 100 dp.
   - **X & Y Offset Sliders**: Mengatur penempatan widget agar pas dengan *notch* atau *punch-hole* kamera.
   - **Opacity / Alpha Slider**: Mengatur transparansi widget dari 0% hingga 100%.

---

## 🏗️ Struktur & Arsitektur Modul Proyek

Aplikasi ini dirancang dengan prinsip *Single Responsibility Architecture*:



---

## 🛠️ Panduan Pengembang & Kolaborasi (Developer Workflow)

Kami sangat menyambut kontribusi dari pengembang Android komunitas untuk pengembangan jangka panjang!

### 1. Kloning Repositori & Kompilasi Lokal:


### 2. Standar Alur Kontribusi (*Pull Request*):
1. **Fork** repositori ini ke akun Anda.
2. Buat *feature branch* baru ().
3. Commit perubahan secara terstruktur ().
4. Push branch Anda ().
5. Buka **Pull Request** ke branch .

---

## 📄 Lisensi

Proyek ini didistribusikan di bawah Lisensi **MIT**. Silakan lihat berkas  untuk informasi selengkapnya.

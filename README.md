# iOS Duo Status Bar for Android 📱

Aplikasi Android untuk menampilkan modul overlay status bar gaya **iOS Duo**, di mana ikon **Baterai**, **Wi-Fi**, dan **Sinyal Seluler** ditumpuk secara vertikal dalam satu grup visual tanpa memerlukan akses Root.

---

## 🌟 Fitur Utama
- **Stacked Status Bar Icons**: Ikon Baterai, Wi-Fi, dan Seluler disusun menumpuk secara vertikal ala iOS Duo.
- **Tanpa Root**: Menggunakan `WindowManager` (`TYPE_APPLICATION_OVERLAY`) sehingga dapat berjalan di semua perangkat Android (Android 7.0 / SDK 24+).
- **Real-Time System Monitoring**: Memantau status persentase dan pengisian daya baterai secara langsung menggunakan `BroadcastReceiver`.

---

## 🛠️ Skema Pembuatan & Arsitektur

Aplikasi ini dibangun menggunakan arsitektur native Android (Kotlin) dan dikompilasi secara otomatis menggunakan **GitHub Actions CI/CD Pipeline**.

### 1. Komponen Aplikasi
* **`MainActivity.kt`**: Memeriksa dan meminta izin akses overlay (`SYSTEM_ALERT_WINDOW`) dari pengguna.
* **`StatusBarOverlayService.kt`**: Service yang merender layout tumpuk di atas status bar sistem menggunakan `WindowManager`.
* **`SystemStatusMonitor.kt`**: Memantau perubahan status baterai dan konektivitas secara *real-time*.
* **`layout_stacked_status.xml`**: Layout UI vertikal (`LinearLayout`) tempat penumpukan ikon Baterai, Wi-Fi, dan Seluler.

### 2. Pipeline Kompilasi Otomatis (CI/CD)
Proses pengerjaan dan *build* dilakukan otomatis melalui skrip `.github/workflows/main.yml`:
1. **Environment**: Linux Ubuntu dengan Java JDK 17.
2. **Build Tool**: Gradle 8.5 via Gradle Wrapper & Android Gradle Plugin (AGP) 8.2.2.
3. **Trigger**: Memicu kompilasi otomatis setiap ada `push` ke branch `main`/`master` atau dijalankan secara manual (`workflow_dispatch`).
4. **Penamaan Dinamis**: Setiap artifact yang dihasilkan secara otomatis diberi penamaan berdasarkan tanggal kompilasi (`iOS-Duo-StatusBar-YYYY-MM-DD`).

---

## 📦 Unduh APK & Artifacts

File APK hasil kompilasi dapat diunduh langsung dari tab **Actions** pada repository ini:

👉 **[Link ke Halaman Artifacts / Build Terbaru](../../actions)**

### Daftar Rilis Artifacts:
* **File Rilis Awal**: `iOS-Duo-StatusBar-2026-10-03`
* **Rilis Berikutnya**: Setiap *build* atau *push* baru akan secara otomatis dinamai `iOS-Duo-StatusBar-YYYY-MM-DD` mengikuti tanggal proses kompilasi dijalankan.

---

## 🚀 Cara Instalasi & Penggunaan

1. Masuk ke tab **[Actions](../../actions)** pada repository ini.
2. Klik pada riwayat *build* paling atas yang bertanda **Centang Hijau (✓)**.
3. Gulir ke bagian **Artifacts** di bawah halaman detail build, lalu tekan nama file artifact (misalnya: `iOS-Duo-StatusBar-2026-10-03`) untuk mengunduhnya.
4. Ekstrak file `.zip` yang terunduh untuk mendapatkan file `app-debug.apk`.
5. Instal file `.apk` tersebut di HP Android Anda.
6. Buka aplikasi, klik tombol **Aktifkan iOS Duo Status Bar**, dan berikan izin **Tampilkan di atas aplikasi lain** (*Display over other apps*).

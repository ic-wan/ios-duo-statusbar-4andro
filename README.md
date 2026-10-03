# 📱 iOS Duo Status Bar for Android

Overlay indikator sistem bergaya **iOS Duo** untuk Android, dibuat dengan **native Kotlin + Canvas** tanpa library UI pihak ketiga.

<!-- BUILD_META_START -->
Build metadata akan diisi otomatis oleh GitHub Actions setelah build pada branch `main`.
<!-- BUILD_META_END -->

## Tampilan

Konsep visual utama mengikuti referensi:

```text
              83
          (       )
            ( WiFi )
             •
           • • • •
```

- Angka baterai di bagian atas.
- Dua bracket arc simetris di kiri/kanan.
- Wi-Fi di tengah.
- Empat titik sinyal seluler melengkung di bagian bawah.
- Stroke native Canvas dengan anti-aliasing.
- **Auto contrast**: foreground otomatis memilih hitam/putih mengikuti mode terang/gelap sistem, ditambah halo kebalikan warna. Ini aman dan ringan tanpa akses screenshot layar.
- Bentuk tetap ringan dan tajam pada ukuran overlay kecil.

> **Catatan teknis penting soal warna:** overlay Android biasa tidak boleh membaca piksel aplikasi lain di bawahnya secara langsung. Karena itu versi ini tidak memakai MediaProjection/screenshot. Mode AUTO menggunakan mode terang/gelap sistem sebagai proxy, lalu menerapkan foreground + halo kebalikan warna. Hasilnya stabil dan ringan, tetapi **bukan** pembacaan warna piksel real-time per aplikasi.

## Fitur

- Battery percentage real-time.
- Charging indicator.
- Wi-Fi connected/disconnected.
- Wi-Fi signal 0–4.
- Cellular signal 0–4 (melalui `READ_PHONE_STATE`).
- Ukuran overlay 44–120 dp.
- X/Y offset untuk notch atau punch-hole.
- Opacity 20–100%.
- Foreground service untuk menjaga overlay tetap hidup.
- `TYPE_APPLICATION_OVERLAY` pada Android 8+.
- Release build dengan R8/shrinkResources.
- APK otomatis dibuat GitHub Actions dan ditandatangani. Untuk produksi, gunakan GitHub Secrets `RELEASE_KEYSTORE_B64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, dan `RELEASE_KEY_PASSWORD`; jika belum diisi, CI memakai debug keystore agar APK tetap bisa diinstal.

## Persyaratan

- Android 7.0+ (API 24).
- Izin **Display over other apps**.
- Android 8+ memakai `TYPE_APPLICATION_OVERLAY`.
- Foreground-service notification ditampilkan saat overlay aktif.
- Tidak menggunakan MediaProjection sehingga tidak meminta izin perekaman layar.

## Struktur

```text
app/src/main/java/com/example/iosstatusbar/
├── CircularStatusView.kt
├── MainActivity.kt
├── StatusBarOverlayService.kt
└── SystemStatusMonitor.kt

.github/workflows/
└── android.yml
```

## Build lokal

```bash
git clone https://github.com/OWNER/REPOSITORY.git
cd REPOSITORY
./gradlew assembleRelease
```

APK:

```text
app/build/outputs/apk/release/app-release.apk
```

## GitHub Actions

Workflow:

```text
.github/workflows/android.yml
```

Pipeline melakukan:

1. Checkout repository.
2. Setup JDK 17.
3. Setup Gradle.
4. Generate Gradle Wrapper jika repository belum memilikinya.
5. Lint release.
6. Build `assembleRelease`.
7. Upload APK sebagai artifact.
8. Pada branch `main`, memperbarui metadata build di README.
9. Commit README memakai `[skip ci]` agar tidak membuat loop build.

## Kenapa tidak membuat source code dari workflow?

Source code aplikasi sebaiknya berada di repository, bukan dibuat ulang oleh CI setiap build. Dengan cara ini:

- perubahan kode dapat direview melalui Git diff;
- pull request dapat menguji perubahan yang sebenarnya;
- build reproducible;
- CI tidak menimpa source developer;
- README dan workflow tidak menjadi satu script raksasa.

## Lisensi

MIT.

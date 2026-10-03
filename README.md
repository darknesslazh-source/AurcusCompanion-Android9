# Aurcus Companion — Visual Toolkit (Android 9+)

Aplikasi Android pendamping dengan tema dark-fantasy, panel mengambang yang dapat dipindahkan/dilipat, alat catatan, checklist, timer, dan pratinjau visual lokal.

## Fitur

- Dashboard dan floating overlay.
- Panel Damage HUD untuk mengubah skala angka contoh.
- Panel Attack Range untuk melihat lingkaran referensi visual.
- Panel Skill AoE dan Skill Effect untuk mengubah skala pratinjau lokal.
- Checklist quest dan tugas farming yang disimpan lokal.
- Catatan map/rute, spawn tracker manual, dan pembanding statistik speed.
- Pengaturan overlay disimpan pada perangkat.
- GitHub Actions membangun debug APK dan mengunggahnya sebagai artifact.

## Batasan fungsi

Pengaturan Damage, Attack Range, AoE, dan Speed di aplikasi ini adalah kalkulator atau pratinjau companion saja. Pengaturan tersebut **tidak mengubah** damage aktual, hitbox, jangkauan serangan, kecepatan karakter, atau server Aurcus Online. Aplikasi tidak menginjeksi proses game, membaca memori game, maupun mengirim input gameplay otomatis.

## Build APK melalui GitHub Actions

1. Ekstrak ZIP ini.
2. Unggah **seluruh isi folder ini** ke root repositori GitHub (termasuk folder tersembunyi `.github/workflows`). `settings.gradle.kts` harus langsung berada di root repositori.
3. Commit dan push ke branch `main` atau `master`, atau buka tab **Actions** dan jalankan workflow **Build Aurcus Companion APK** dengan **Run workflow**.
4. Tunggu job selesai. Jika statusnya **Success**, buka run tersebut dan unduh artifact `AurcusCompanion-debug-apk`.
5. Ekstrak artifact untuk mendapatkan `app-debug.apk`.

Jika build gagal, buka langkah **Compile Kotlin and build debug APK** dan lihat pesan pertama `e:` atau `error:`. Artifact `AurcusCompanion-build-diagnostics` mungkin tersedia untuk membantu diagnosis.

## Build lokal dengan Android Studio

- JDK 17
- Android SDK Platform 35 dan Build Tools 35.0.0
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Kotlin 2.0.21

Buka folder proyek yang berisi `settings.gradle.kts`, tunggu Gradle Sync, lalu pilih **Build → Build APK(s)**. Output debug biasanya berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Status verifikasi

Konfigurasi dan workflow disusun untuk build di GitHub Actions. Keberhasilan APK hanya terkonfirmasi setelah workflow benar-benar selesai dengan status **Success**; ZIP sumber saja bukan bukti APK sudah terkompilasi.

# Aurcus Companion — Visual Toolkit (Android 9+)

Project Android Studio untuk aplikasi pendamping dengan tema dark fantasy, panel mengambang yang bisa dipindahkan/dilipat, serta kontrol pratinjau visual.

## Fitur
- Floating overlay dengan menu MOD, DMG, RANGE, AOE, QUEST, dan MORE.
- Damage HUD preview: skala angka contoh dan durasi indikator.
- Attack range preview: slider dan lingkaran referensi visual.
- Skill AoE preview: pengaturan radius dan skala efek visual.
- Checklist quest lokal, catatan map/spawn/speed, serta timer farming di aplikasi utama.
- Preferensi slider dan toggle overlay disimpan lokal.
- Build workflow GitHub Actions untuk menghasilkan debug APK.

## Batasan penting
Kontrol DMG/RANGE/AOE hanya mengubah pratinjau di companion. Aplikasi ini tidak mengubah atribut karakter, damage aktual, hitbox, kecepatan, area skill di Aurcus Online publik, tidak menginjeksi proses game, tidak membaca memori game, tidak mengirim input otomatis, dan tidak mengubah data server.

## Import dan build
1. Ekstrak ZIP.
2. Android Studio → Open → pilih folder yang berisi `settings.gradle.kts`.
3. Gunakan JDK 17 dan tunggu Gradle Sync.
4. Pilih Build → Build APK(s), jalankan `build-apk.bat` jika Gradle tersedia di PATH, atau push ke GitHub lalu jalankan Actions workflow `Build Aurcus Companion APK`.
5. Artifact debug tersedia sebagai `AurcusCompanion-debug-apk`.

Konfigurasi: Android Gradle Plugin 8.7.3, Kotlin 2.0.21, compileSdk 35, minSdk 28, target Java/Kotlin JVM 17.

## Status verifikasi
ZIP sumber diperiksa integritasnya. Perbaikan statis dilakukan pada konstanta warna Kotlin di `FloatingOverlayService.kt` (mengganti `const val` yang memakai `.toInt()` menjadi `val`). Build APK penuh belum diverifikasi di lingkungan ini; verifikasi melalui Android Studio atau GitHub Actions.

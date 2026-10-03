# Panduan build APK melalui GitHub

1. Ekstrak ZIP proyek.
2. Buka repositori GitHub tujuan.
3. Unggah isi folder proyek ini ke root repositori, bukan folder induk. Pastikan `settings.gradle.kts`, `app/`, dan `.github/workflows/build-apk.yml` berada langsung di root.
4. Commit perubahan ke branch `main` atau `master`.
5. Buka tab **Actions** dan pilih **Build Aurcus Companion APK**.
6. Jika build belum berjalan otomatis, klik **Run workflow**.
7. Jika job **Compile and package debug APK** berstatus **Success**, buka hasil run dan unduh artifact **AurcusCompanion-debug-apk**.
8. Ekstrak artifact untuk mengambil `app-debug.apk`.

Jika gagal, buka log job dan cari pesan error Kotlin/Gradle pertama. Workflow juga mencoba mengunggah artifact `AurcusCompanion-build-diagnostics` saat gagal.

Catatan: build debug bukan APK release bertanda tangan untuk distribusi Play Store. Nilai damage/range/AoE pada aplikasi ini hanya pratinjau lokal companion dan tidak memodifikasi server/game.

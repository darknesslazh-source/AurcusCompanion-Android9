# Aurcus Companion — Visual Toolkit (Android 9+)

Aplikasi Android pendamping dengan tema dark-fantasy, panel mengambang yang dapat dipindahkan/dilipat, alat catatan, checklist, timer, dan pratinjau visual lokal.

## Fitur

- Dashboard dan floating overlay.
- **Menu Fitur Game** (tab *Menu* dan halaman `MENU` di overlay): 18 kategori fitur (PvP, Skill/Job, Guild, Inventori, Toko/Gacha, Equipment, Misi, Housing, Crafting, Auction, dll.) yang diturunkan dari `aurcus_class_index.csv`. Tiap menu punya deskripsi, kata kunci UI, catatan pribadi, dan favorit.
- **Daftar Modifikasi Build** (tab *Build* dan halaman `BUILD` di overlay): rencana Enhance, Slot Enhance, Evolve, Remodel, Ekstrak Ability, Elemen, Status Point, Ganti Job, Skill, Slot Skill, Crafting, dan Maker Level. Tombol **Tambah semua** memasukkan seluruh jenis sekali ketuk; target, level, dan catatan bisa diisi di aplikasi utama, dan centang selesai bisa dilakukan dari overlay.
- Panel Damage HUD untuk mengubah skala angka contoh.
- Panel Attack Range untuk melihat lingkaran referensi visual.
- Panel Skill AoE dan Skill Effect untuk mengubah skala pratinjau lokal.
- Checklist quest dan tugas farming yang disimpan lokal.
- Catatan map/rute, spawn tracker manual, dan pembanding statistik speed.
- Pengaturan overlay disimpan pada perangkat.
- GitHub Actions membangun debug APK dan mengunggahnya sebagai artifact.

## Sumber data menu

`GameFeatureCatalog.kt` dan `ModificationCatalog.kt` berisi data statis yang diturunkan dari indeks kelas game (`aurcus_class_index.csv`, kelas keluarga `GAME` dengan peran "UI fitur: ..."). Data itu hanya dipakai sebagai label, deskripsi, dan kata kunci referensi. Aplikasi tidak memuat atau memanggil kode `classes.dex` milik game.

## Batasan fungsi

Pengaturan Damage, Attack Range, AoE, dan Speed di aplikasi ini adalah kalkulator atau pratinjau companion saja. Menu fitur dan daftar modifikasi build hanyalah catatan/rencana lokal: aplikasi tidak melakukan enhance, remodel, evolve, atau aksi apa pun di game. Pengaturan tersebut **tidak mengubah** damage aktual, hitbox, jangkauan serangan, kecepatan karakter, atau server Aurcus Online. Aplikasi tidak menginjeksi proses game, membaca memori game, maupun mengirim input gameplay otomatis.

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

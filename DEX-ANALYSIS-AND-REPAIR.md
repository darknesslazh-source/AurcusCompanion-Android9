# Aurcus Companion — hasil audit dan perbaikan awal

## Batas verifikasi
- File input: source ZIP Aurcus Companion dan `classes.dex` yang diunggah pada percakapan.
- `classes.dex` terdeteksi sebagai Dalvik DEX versi 035, ukuran 7,602,856 byte.
- String pool memuat string yang cocok secara literal dengan `CURRENCY_NORMAL`, `PVP_ROOM_TITLE`, `ERROR_ENTER_ROOM_SEARCH_NO_ROOM`, `GACHA_RARE_S`, `NORMAL_ATTACK_HIT_EFFECT`, serta `SpeedHack`.
- Ini membuktikan keberadaan string, bukan bahwa string tersebut merupakan API publik, metode yang dapat dipanggil dari aplikasi lain, atau fitur yang bisa diaktifkan dari overlay.
- Tool decompiler (JADX/apktool/baksmali) dan Gradle tidak tersedia di lingkungan audit ini. APK belum dibangun dan belum diuji pada Android 9.

## Perubahan kode
1. Map notes dan spawn notes sekarang dimuat dari dan disimpan ke SharedPreferences; sebelumnya nilainya hanya bertahan selama layar Compose tetap hidup.
2. Label UI katalog diubah agar menyebut “kata kunci/string DEX”, bukan mengklaim jumlah kelas yang belum diverifikasi dari DEX yang tersedia.
3. Detail fitur menampilkan peringatan bahwa string DEX bukan bukti bahwa companion dapat memanggil fungsi internal game.
4. Gerakan overlay dibatasi agar panel tidak mudah diseret jauh keluar dari batas layar.
5. Perilaku yang ada tetap companion lokal: favorit, checklist, rencana build, catatan, dan pratinjau visual. Tidak ada injeksi, pembacaan memori game, manipulasi paket, atau perubahan server.

## Belum diperbaiki / butuh bukti tambahan
- Pemetaan akurat kelas → metode → parameter memerlukan dekompilasi lengkap serta identifikasi kelas terkait; DEX tunggal saja tidak memberi kontrak integrasi yang aman.
- Damage, attack range, AoE, speed, dan status gameplay tidak dihubungkan ke game; UI menjelaskannya sebagai simulasi/monitor lokal.
- Belum ada hasil build, instalasi, atau uji runtime. Jangan anggap ZIP ini sebagai APK yang sudah berhasil.

## Uji lanjutan Android 9
1. Build `:app:assembleDebug` dengan JDK 17, Gradle wrapper/toolchain yang sesuai, Android SDK platform 35 dan build-tools 35.
2. Instal APK pada Android 9 dan beri izin “Tampil di atas aplikasi lain”.
3. Uji mulai/berhenti overlay, lipat/buka, drag pada layar kecil, navigasi semua tab, dan persistensi map/spawn notes setelah aplikasi ditutup lalu dibuka lagi.
4. Simpan logcat bila overlay gagal tampil atau service berhenti.

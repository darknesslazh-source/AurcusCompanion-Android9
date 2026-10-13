# Koneksi DEX

Aplikasi ini terhubung ke `classes.dex` klien Aurcus Online **lewat data**, bukan lewat game yang berjalan.

## Yang terhubung
- `app/src/main/assets/dex_ui_keys.json` berisi 1.729 kunci teks UI yang diekstrak dari kelas `v1.m`
  (SHA-256 dex ada di bagian `meta`). Jumlah dan daftar kunci tiap kategori Menu dihitung dari berkas ini.
- Tab **Dex** (dan halaman DEX di overlay) memverifikasi berkas `classes.dex` atau APK pilihanmu:
  tabel string dibaca, lalu dicocokkan dengan indeks. Hasilnya muncul juga di kartu Menu.
- Dari Menu: cari kunci, salin kunci, tulis catatan, dan buka alat lokal terkait (Build/Farm/Map/Mods).

## Membangun ulang indeks untuk versi game lain
```
pip install androguard
python3 tools/gen_dex_index.py path/ke/classes.dex
```
Jika nama kelas tabel teks berubah, sesuaikan `SOURCE_CLASS` di skrip. Aturan kategori ada di `RULES`
dalam skrip yang sama dan ikut tersimpan di JSON.

## Batasan (sengaja)
Aplikasi tidak memuat atau menjalankan kode dex, tidak menyuntikkan apa pun ke proses game, tidak membaca
memori game, dan tidak mengubah damage, jangkauan, AoE, atau speed. Slider dan toggle hanya memengaruhi
pratinjau di companion.

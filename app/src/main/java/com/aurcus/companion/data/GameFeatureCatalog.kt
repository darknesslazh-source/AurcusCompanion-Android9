package com.aurcus.companion.data

/**
 * Kategori menu referensi. Judul, deskripsi, dan ide catatan adalah interpretasi manual;
 * jumlah dan daftar kunci tiap kategori dihitung dari indeks dex ([DexKeyIndex]), bukan ditulis tangan.
 * [tool] menunjuk alat lokal yang relevan (lihat [Tools]); null berarti hanya catatan.
 * Tidak ada hubungan runtime dengan game.
 */
data class GameFeature(
    val id: String,
    val code: String,
    val title: String,
    val description: String,
    val noteHint: String,
    val tool: String? = null
)

object GameFeatureCatalog {
    val features: List<GameFeature> = listOf(
        GameFeature("umum", "GEN", "Umum & Mata Uang", "Dialog umum, mata uang, level, pesan loading dan error yang dipakai semua layar.", "Catat nilai mata uang, batas level, dan pesan error yang sering kamu temui."),
        GameFeature("pvp", "PVP", "PvP & Arena", "Room PvP: aturan room, mode, judul room, dan pencarian room.", "Catat mode room favorit, aturan, dan hasil pertandingan secara manual."),
        GameFeature("skill", "SKL", "Ability, Skill & Job", "Status point, pelajari/reset skill, level skill baru, slot skill, dan ganti job.", "Rencanakan alokasi status point, urutan belajar skill, dan job tujuan.", "build"),
        GameFeature("guild", "GLD", "Guild", "Daftar guild, batas masuk, nama anggota, dan guild house.", "Catat jadwal event guild, kontribusi, dan target guild house."),
        GameFeature("inventori", "INV", "Inventori & Item", "Tas utama/premiere/pribadi, jumlah item, dan pemilihan item.", "Catat item target, jumlah stok, dan material yang masih kurang."),
        GameFeature("toko", "SHP", "Toko, Koin & Gacha", "Toko, stok habis, pembayaran, dan tingkat rare gacha (S/A/B).", "Catat anggaran koin, hasil gacha, dan daftar belanja prioritas."),
        GameFeature("ranking", "RNK", "Ranking, Title & Achievement", "Tampilan semua/selesai/belum selesai untuk achievement dan title.", "Centang achievement yang sudah dan belum selesai."),
        GameFeature("party", "PRT", "Party, Teman & Chat", "Nama party, status login teman, ketua party, dan daftar block.", "Catat anggota party tetap dan jadwal main bersama."),
        GameFeature("equipment", "EQP", "Equipment, Remodel & Enhance", "Pilih equipment, slot, enhance, evolve (material), dan remodel base item.", "Buka tab Build untuk merencanakan Enhance, Evolve, dan Remodel.", "build"),
        GameFeature("misi", "MSN", "Misi & Field Mission", "Quest, treasure box, dan hadiah field mission.", "Catat misi aktif, target drop, dan progres harian.", "farm"),
        GameFeature("housing", "HSE", "Housing & Furnitur", "Penempatan furnitur, tinggi/posisi, dan hak akses guild house.", "Catat desain ruangan, daftar furnitur, dan hak akses."),
        GameFeature("crafting", "CRF", "Crafting & Maker", "Pembuatan item, material langka, dan maker level up.", "Catat resep, material yang dibutuhkan, dan target maker level.", "build"),
        GameFeature("akun", "ACC", "Akun & Lobi", "Pembuatan karakter, anggota lobi, dan poin hadiah teman.", "Catat daftar karakter dan tujuan masing-masing karakter."),
        GameFeature("auction", "AUC", "Auction", "Pasang barang, beli, dan penalti gagal pasang.", "Catat harga pasar yang kamu amati dan barang yang ingin dibeli."),
        GameFeature("channel", "CHN", "Channel, Warp & Peta", "Ganti channel, warp, dan nama peta.", "Catat channel/peta favorit untuk farming.", "map"),
        GameFeature("config", "CFG", "Konfigurasi", "Kontrol klasik, tampilan HP pemain lain, dan notifikasi.", "Catat pengaturan yang nyaman sebagai referensi pribadi.", "mods"),
        GameFeature("revive", "REV", "Mati & Revive", "Pesan kematian, menunggu respawn, dan scroll revive.", "Catat stok revive scroll dan penyebab kematian yang sering terjadi."),
        GameFeature("trade", "TRD", "Trade", "Trade antar pemain dan status trade yang sudah diputuskan.", "Catat barang yang ditukar dan mitra trade.")
    )

    fun byId(id: String): GameFeature? = features.firstOrNull { it.id == id }

    /** Favorit tampil lebih dulu; urutan asli dipertahankan di dalam tiap kelompok. */
    fun sorted(favorites: Set<String>): List<GameFeature> =
        features.filter { it.id in favorites } + features.filter { it.id !in favorites }
}

package com.aurcus.companion.data

/**
 * Katalog referensi yang dikelompokkan secara manual dari string UI yang ditemukan di classes.dex.
 * Judul/deskripsi adalah interpretasi, bukan pemetaan metode yang sudah dibuktikan.
 * classCount berasal dari metadata katalog lama dan belum diverifikasi ulang terhadap DEX yang diunggah.
 * Tidak ada hubungan runtime dengan game.
 */
data class GameFeature(
    val id: String,
    val code: String,
    val title: String,
    val description: String,
    val noteHint: String,
    val classCount: Int,
    val terms: List<String>
)

object GameFeatureCatalog {
    val features: List<GameFeature> = listOf(
        GameFeature("umum", "GEN", "Umum & Mata Uang", "Dialog umum, mata uang, level, pesan loading dan error yang dipakai semua layar.", "Catat nilai mata uang, batas level, dan pesan error yang sering kamu temui.", 57, listOf("CURRENCY_NORMAL", "LEVEL", "NOW_DATA_LOADING", "ERROR_LONG_NAME", "VALUE")),
        GameFeature("pvp", "PVP", "PvP & Arena", "Room PvP: aturan room, mode, judul room, dan pencarian room.", "Catat mode room favorit, aturan, dan hasil pertandingan secara manual.", 35, listOf("PVP_ROOM_RULE", "ERROR_ENTER_ROOM_SEARCH_NO_ROOM", "SCOPE", "PVP_ROOM_MODE", "PVP_ROOM_TITLE")),
        GameFeature("skill", "SKL", "Ability, Skill & Job", "Status point, pelajari/reset skill, level skill baru, slot skill, dan ganti job.", "Rencanakan alokasi status point, urutan belajar skill, dan job tujuan.", 25, listOf("PERCENT", "USE_STATUS_POINT", "ERROR_CHANGE_JOB_DEFAULT", "LEVEL", "SUCCESS_RESET_SKILL")),
        GameFeature("guild", "GLD", "Guild", "Daftar guild, batas masuk, nama anggota, dan guild house.", "Catat jadwal event guild, kontribusi, dan target guild house.", 22, listOf("PLAYER_NAME", "CURRENCY_NORMAL", "VALUE", "LIMIT_ENTRANCE", "NOT_LIMIT")),
        GameFeature("inventori", "INV", "Inventori & Item", "Tas utama/premiere/pribadi, jumlah item, dan pemilihan item.", "Catat item target, jumlah stok, dan material yang masih kurang.", 21, listOf("NUMBER_OF_ITEM", "ERROR_SET_ITEM_SC_SELECT", "BULLET", "CHANGE_ARROW_BIG", "ITEM_SC_ARENA")),
        GameFeature("toko", "SHP", "Toko, Koin & Gacha", "Toko, stok habis, pembayaran, dan tingkat rare gacha (S/A/B).", "Catat anggaran koin, hasil gacha, dan daftar belanja prioritas.", 18, listOf("SHOP_WAIT_LIST", "SHOP_SOLD_OUT", "ERROR_BOOT_NETWORK_CONNECT", "GACHA_RARE_S", "PAYMENT")),
        GameFeature("ranking", "RNK", "Ranking, Title & Achievement", "Tampilan semua/selesai/belum selesai untuk achievement dan title.", "Centang achievement yang sudah dan belum selesai.", 14, listOf("APK_UPDATE_TITLE", "DISPLAY_ALL", "DISPLAY_ACHIEVED", "DISPLAY_NOT_ACHIEVED", "APK_DOWNLOAD_COMMENT")),
        GameFeature("party", "PRT", "Party, Teman & Chat", "Nama party, status login teman, ketua party, dan daftar block.", "Catat anggota party tetap dan jadwal main bersama.", 14, listOf("PT_NAME", "BULLET_POINT", "PARTY_NOW", "LOGIN_NOW", "HIDE_LOGIN_INFORMATION")),
        GameFeature("equipment", "EQP", "Equipment, Remodel & Enhance", "Pilih equipment, slot, enhance, evolve (material), dan remodel base item.", "Buka tab Build untuk merencanakan Enhance, Evolve, dan Remodel.", 13, listOf("PLEASE_EQUIP_CHOICE", "ERROR_USE_ITEM", "SCOPE", "BASE_PARAMETER", "ENFORCE")),
        GameFeature("misi", "MSN", "Misi & Field Mission", "Quest, treasure box, dan hadiah field mission.", "Catat misi aktif, target drop, dan progres harian.", 10, listOf("CONFIRM_SELECT_FIRST_TREASURE_CHEST", "ERROR_USE_ITEM_DEFAULT", "ETC", "GET_TREASURE_BOX_ITEM", "EMPTY_TREASURE_BOX")),
        GameFeature("housing", "HSE", "Housing & Furnitur", "Penempatan furnitur, tinggi/posisi, dan hak akses guild house.", "Catat desain ruangan, daftar furnitur, dan hak akses.", 9, listOf("GUILD_HOUSE", "ERROR_PUT_THIS_PLACE", "ERROR_PUT_FURNITURE", "ERROR_PUT_HEIGHT", "ERROR_FURNITURE_AUTHORITY")),
        GameFeature("crafting", "CRF", "Crafting & Maker", "Pembuatan item, material langka, dan maker level up.", "Catat resep, material yang dibutuhkan, dan target maker level.", 7, listOf("ASK_RARE_MATERIAL", "CONFIRM_MAKE_TIME_ITEM", "ERROR_CONNECT", "BULLET", "CONFIRM_MAKER_LEVEL_UP")),
        GameFeature("akun", "ACC", "Akun & Lobi", "Pembuatan karakter, anggota lobi, dan poin hadiah teman.", "Catat daftar karakter dan tujuan masing-masing karakter.", 7, listOf("NUMBER_OF_LOBBY_MEMBER", "LEVEL", "MISSION_REWARD_FRIEND_POINT", "CHARACTER_CREATION", "ERROR_AUTHENTICATE")),
        GameFeature("auction", "AUC", "Auction", "Pasang barang, beli, dan penalti gagal pasang.", "Catat harga pasar yang kamu amati dan barang yang ingin dibeli.", 7, listOf("ERROR_AUCTION_BUY_BAG_FULL", "SUCCESS_AUCTION_BUY", "ERROR_AUCTION_BUY_DEFAULT", "SUCCESS_AUCTION_EXHIBIT", "ERROR_CONNECT")),
        GameFeature("channel", "CHN", "Channel, Warp & Peta", "Ganti channel, warp, dan nama peta.", "Catat channel/peta favorit untuk farming.", 5, listOf("CHANNEL", "PREFIX_TO", "FAILED_CHANGE_CHANNEL", "NOW_CHANGE_CHANNEL", "ERROR_CHANGE_CHANNEL_CURRENT")),
        GameFeature("config", "CFG", "Konfigurasi", "Kontrol klasik, tampilan HP pemain lain, dan notifikasi.", "Catat pengaturan yang nyaman sebagai referensi pribadi.", 4, listOf("CONFIRM_CONFIG_CANCEL", "CONFIG_DRAW_ANOTHER_PLAYER_HP_DECREASED", "CONFIG_DRAW_ANOTHER_PLAYER_HP_ENABLE", "CLASSIC", "CONFIG_CLASSIC_CONTROL")),
        GameFeature("revive", "REV", "Mati & Revive", "Pesan kematian, menunggu respawn, dan scroll revive.", "Catat stok revive scroll dan penyebab kematian yang sering terjadi.", 2, listOf("DEAD_MESSAGE", "DEAD_WAIT_RESPAWN", "PVP_FIGHT_WAIT", "CONFIRM_REVIVAL_PREMIERE", "USE_REVIVAL_SCROLL")),
        GameFeature("trade", "TRD", "Trade", "Trade antar pemain dan status trade yang sudah diputuskan.", "Catat barang yang ditukar dan mitra trade.", 1, listOf("CURRENCY_NORMAL", "ERROR_TRADE_DECIDED", "ERROR_TRADE_REGISTERED"))
    )

    fun byId(id: String): GameFeature? = features.firstOrNull { it.id == id }

    /** Favorit tampil lebih dulu; urutan asli dipertahankan di dalam tiap kelompok. */
    fun sorted(favorites: Set<String>): List<GameFeature> =
        features.filter { it.id in favorites } + features.filter { it.id !in favorites }
}

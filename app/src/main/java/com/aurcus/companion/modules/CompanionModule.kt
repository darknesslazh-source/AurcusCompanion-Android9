package com.aurcus.companion.modules

/**
 * Arsitektur modular: setiap alat companion mendaftar sebagai modul.
 * Modul baru cukup didaftarkan di ModuleRegistry tanpa mengubah layar utama.
 * Semua modul berjalan di dalam aplikasi ini saja.
 */
data class CompanionModule(val id: String, val title: String, val glyph: String, val description: String)

object ModuleRegistry {
    private val modules = linkedMapOf<String, CompanionModule>()
    fun register(m: CompanionModule) { modules[m.id] = m }
    fun all(): List<CompanionModule> = modules.values.toList()
    init {
        register(CompanionModule("mod", "Mod", "⚔", "Kalkulator damage & pratinjau visual"))
        register(CompanionModule("quest", "Quest", "📜", "Pelacak quest pribadi"))
        register(CompanionModule("map", "Map", "🗺", "Catatan lokasi & rute"))
        register(CompanionModule("spawn", "Spawn", "👹", "Log pengamatan respawn manual"))
        register(CompanionModule("stat", "Stat", "📊", "Monitor kecepatan & performa"))
        register(CompanionModule("setting", "Setting", "⚙", "Tema, ukuran, transparansi"))
    }
}

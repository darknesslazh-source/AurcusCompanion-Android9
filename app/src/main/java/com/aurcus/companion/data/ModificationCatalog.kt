package com.aurcus.companion.data

/** Jenis modifikasi build yang bisa direncanakan. Diturunkan dari kunci string UI di aurcus_class_index.csv. */
data class ModTemplate(val id: String, val name: String, val category: String, val description: String)

object ModificationCatalog {
    val templates: List<ModTemplate> = listOf(
        ModTemplate("enhance", "Enhance equipment", "Equipment", "Naikkan level equipment (ENFORCE). Catat target dan material."),
        ModTemplate("slot_enhance", "Enhance per slot", "Equipment", "Enhance slot tertentu (SLOT_ENFORCE)."),
        ModTemplate("evolve", "Evolve equipment", "Equipment", "Evolve equipment dengan material evolve (EVO_MATERIAL)."),
        ModTemplate("remodel", "Remodel equipment", "Equipment", "Pilih base item dan completion item untuk remodel."),
        ModTemplate("ability_extract", "Ekstrak ability slot", "Equipment", "Ambil ability dari slot equipment."),
        ModTemplate("element", "Atribut elemen equipment", "Equipment", "Rencanakan elemen equipment (Wind / Water, dll.)."),
        ModTemplate("status_points", "Alokasi status point", "Karakter", "Atur pembagian status point (STR / VIT / DEX / INT)."),
        ModTemplate("job_change", "Ganti job", "Karakter", "Rencanakan job tujuan dan syarat misinya."),
        ModTemplate("skill_learn", "Pelajari & naikkan skill", "Skill", "Urutan belajar skill dan level skill target."),
        ModTemplate("skill_slots", "Susun slot skill & scroll", "Skill", "Atur skill di slot dan skill scroll yang dipakai."),
        ModTemplate("crafting", "Crafting item", "Crafting", "Daftar item yang ingin dibuat dan material yang dibutuhkan."),
        ModTemplate("maker_level", "Maker level up", "Crafting", "Target level maker untuk membuka resep baru.")
    )

    /** Menambahkan semua template yang belum ada di daftar. Entri yang sudah ada tidak diubah. */
    fun addAll(existing: List<BuildModification>): List<BuildModification> {
        val have = existing.map { it.templateId }.toSet()
        return existing + templates.filter { it.id !in have }.map { fromTemplate(it, "mod-${it.id}") }
    }

    fun fromTemplate(t: ModTemplate, id: String) =
        BuildModification(id = id, templateId = t.id, name = t.name, category = t.category)
}

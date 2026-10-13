package com.aurcus.companion.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** Permintaan membuka tab tertentu; [nonce] membedakan permintaan berulang ke tab yang sama. */
data class TabRequest(val tab: Int, val nonce: Long)

/**
 * Satu sumber kebenaran untuk rute antar alat: tab di aplikasi utama dan halaman di overlay.
 */
object Tools {
    const val EXTRA_TAB = "aurcus_open_tab"

    val tabIds = listOf("home", "menu", "build", "mods", "map", "spawn", "farm", "speed", "dex")
    val tabLabels = listOf("Home", "Menu", "Build", "Mods", "Map", "Spawn", "Farm", "Speed", "Dex")
    val tabGlyphs = listOf("⌂", "☰", "◈", "⚔", "⌖", "◎", "◷", "↗", "▤")

    fun tabIndex(toolId: String?): Int = tabIds.indexOf(toolId).coerceAtLeast(0)

    fun tabLabel(toolId: String?): String = tabLabels[tabIndex(toolId)]

    /** Halaman overlay untuk sebuah alat; null jika alat itu hanya ada di aplikasi utama. */
    fun overlayPage(toolId: String?): String? = when (toolId) {
        "menu" -> "MENU"
        "build" -> "BUILD"
        "mods" -> "MOD"
        "map" -> "MAP"
        "spawn" -> "SPAWN"
        "farm" -> "FARM"
        "speed" -> "SPEED"
        "dex" -> "DEX"
        else -> null
    }
}

/** Kunci dan nilai bawaan pengaturan overlay; dipakai bersama oleh overlay dan aplikasi utama. */
object OverlayOptions {
    const val DAMAGE_SCALE = "damage_scale"
    const val DAMAGE_DURATION = "damage_duration"
    const val ATTACK_RANGE = "attack_range"
    const val AOE_RADIUS = "aoe_radius"
    const val EFFECT_SCALE = "effect_scale"

    const val T_DAMAGE = "Damage HUD preview"
    const val T_RANGE = "Attack range preview"
    const val T_AOE = "Skill AoE preview"
    const val T_EFFECT = "Skill effect preview"
    const val T_SPEED = "Speed monitor"
    const val T_QUEST = "Quest checklist"
    const val T_FARM = "Farm session timer"

    /** Nama toggle (juga kunci prefs) beserta nilai bawaannya, dalam urutan tampil. */
    val toggles: Map<String, Boolean> = linkedMapOf(
        T_DAMAGE to true, T_RANGE to true, T_AOE to true, T_EFFECT to true,
        T_SPEED to false, T_QUEST to true, T_FARM to false
    )

    fun describe(name: String): String = when (name) {
        T_DAMAGE -> "Tampilkan halaman DMG: angka contoh dan skala panel damage"
        T_RANGE -> "Tampilkan halaman RANGE: lingkaran referensi jangkauan visual"
        T_AOE -> "Tampilkan halaman AOE: pratinjau radius area skill"
        T_EFFECT -> "Tampilkan slider skala efek visual di halaman AOE"
        T_SPEED -> "Tampilkan halaman SPEED: bandingkan statistik manual"
        T_QUEST -> "Tampilkan halaman QUEST: checklist tujuan pribadi"
        else -> "Tampilkan halaman FARM dan timer sesi di header overlay"
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun formatDuration(totalSeconds: Int): String =
    "%02d:%02d:%02d".format(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)

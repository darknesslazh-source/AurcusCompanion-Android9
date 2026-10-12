package com.aurcus.companion.data

data class Item(
    val id: String,
    val name: String,
    val slot: String,
    val attack: Int = 0,
    val defense: Int = 0,
    val magic: Int = 0,
    val notes: String = ""
)

data class BuildPreset(
    val name: String,
    val strength: Int,
    val vitality: Int,
    val dexterity: Int,
    val intelligence: Int
)

data class QuestTask(
    val id: String,
    val title: String,
    val category: String,
    val targetCount: Int = 1,
    val currentCount: Int = 0,
    val completed: Boolean = false
)

data class BuildSummary(
    val totalPoints: Int,
    val physicalFocusPercent: Int,
    val magicFocusPercent: Int
)

/** Satu baris rencana modifikasi build milik pengguna (catatan lokal, bukan perubahan pada game). */
data class BuildModification(
    val id: String,
    val templateId: String,
    val name: String,
    val category: String,
    val target: String = "",
    val fromLevel: Int = 0,
    val toLevel: Int = 1,
    val note: String = "",
    val done: Boolean = false
)

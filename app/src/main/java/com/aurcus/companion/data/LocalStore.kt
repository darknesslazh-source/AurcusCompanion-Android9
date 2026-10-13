package com.aurcus.companion.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small local JSON store so the starter persists on Android 9 without network access.
 * For larger datasets, migrate to Room/SQLite.
 */
class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("aurcus_companion", Context.MODE_PRIVATE)
    private val notes = context.getSharedPreferences("aurcus_companion_notes", Context.MODE_PRIVATE)
    private val overlayPrefs = context.getSharedPreferences("aurcus_overlay", Context.MODE_PRIVATE)

    fun loadTasks(): List<QuestTask> {
        if (!prefs.contains("tasks")) {
            // Pertama kali: isi checklist awal; centang lama dari overlay versi sebelumnya ikut dibawa.
            val seeded = DEFAULT_TASKS.mapIndexed { i, (title, category) ->
                QuestTask("task-default-$i", title, category, completed = overlayPrefs.getBoolean("quest_$title", false))
            }
            saveTasks(seeded)
            return seeded
        }
        val array = runCatching { JSONArray(prefs.getString("tasks", "[]")) }.getOrNull()
            ?: JSONArray()
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                QuestTask(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    category = o.getString("category"),
                    targetCount = o.optInt("targetCount", 1),
                    currentCount = o.optInt("currentCount", 0),
                    completed = o.optBoolean("completed", false)
                )
            }.getOrNull()
        }
    }

    fun saveTasks(tasks: List<QuestTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("category", task.category)
                put("targetCount", task.targetCount)
                put("currentCount", task.currentCount)
                put("completed", task.completed)
            })
        }
        prefs.edit().putString("tasks", array.toString()).apply()
    }

    fun loadMods(): List<BuildModification> {
        val array = runCatching { JSONArray(prefs.getString("mods", "[]")) }.getOrNull() ?: JSONArray()
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                BuildModification(
                    id = o.getString("id"),
                    templateId = o.optString("templateId", ""),
                    name = o.getString("name"),
                    category = o.optString("category", ""),
                    target = o.optString("target", ""),
                    fromLevel = o.optInt("fromLevel", 0),
                    toLevel = o.optInt("toLevel", 1),
                    note = o.optString("note", ""),
                    done = o.optBoolean("done", false)
                )
            }.getOrNull()
        }
    }

    fun saveMods(mods: List<BuildModification>) {
        val array = JSONArray()
        mods.forEach { m ->
            array.put(JSONObject().apply {
                put("id", m.id)
                put("templateId", m.templateId)
                put("name", m.name)
                put("category", m.category)
                put("target", m.target)
                put("fromLevel", m.fromLevel)
                put("toLevel", m.toLevel)
                put("note", m.note)
                put("done", m.done)
            })
        }
        prefs.edit().putString("mods", array.toString()).apply()
    }

    fun loadFavorites(): Set<String> = prefs.getStringSet("favorite_features", emptySet())?.toSet() ?: emptySet()

    /** Mengembalikan himpunan favorit terbaru setelah [id] di-toggle. */
    fun toggleFavorite(id: String): Set<String> {
        val next = loadFavorites().toMutableSet().also { if (!it.add(id)) it.remove(id) }
        prefs.edit().putStringSet("favorite_features", next).apply()
        return next
    }

    fun loadFeatureNote(id: String): String = prefs.getString("feature_note_$id", "") ?: ""

    fun saveFeatureNote(id: String, note: String) {
        prefs.edit().putString("feature_note_$id", note).apply()
    }

    // ---- Pengaturan overlay (dibagi dengan aplikasi utama) ----

    fun loadScale(key: String, default: Int = 100): Int = overlayPrefs.getInt(key, default)

    fun saveScale(key: String, value: Int) {
        overlayPrefs.edit().putInt(key, value).apply()
    }

    fun loadToggle(name: String): Boolean =
        overlayPrefs.getBoolean(name, OverlayOptions.toggles[name] ?: false)

    fun saveToggle(name: String, value: Boolean) {
        overlayPrefs.edit().putBoolean(name, value).apply()
    }

    fun resetScales() {
        overlayPrefs.edit()
            .putInt(OverlayOptions.DAMAGE_SCALE, 100).putInt(OverlayOptions.DAMAGE_DURATION, 2)
            .putInt(OverlayOptions.ATTACK_RANGE, 100).putInt(OverlayOptions.AOE_RADIUS, 100)
            .putInt(OverlayOptions.EFFECT_SCALE, 100).apply()
    }

    // ---- Timer farm (persisten; dibaca overlay dan aplikasi utama) ----

    fun farmRunning(): Boolean = prefs.getLong("farm_since_ms", 0L) > 0L

    fun farmElapsedSeconds(now: Long = System.currentTimeMillis()): Int {
        val since = prefs.getLong("farm_since_ms", 0L)
        val running = if (since > 0L) (now - since).coerceAtLeast(0L) else 0L
        return ((prefs.getLong("farm_acc_ms", 0L) + running) / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun farmStart(now: Long = System.currentTimeMillis()) {
        if (!farmRunning()) prefs.edit().putLong("farm_since_ms", now).apply()
    }

    fun farmPause(now: Long = System.currentTimeMillis()) {
        val since = prefs.getLong("farm_since_ms", 0L)
        if (since <= 0L) return
        val acc = prefs.getLong("farm_acc_ms", 0L) + (now - since).coerceAtLeast(0L)
        prefs.edit().putLong("farm_acc_ms", acc).putLong("farm_since_ms", 0L).apply()
    }

    fun farmReset() {
        prefs.edit().putLong("farm_acc_ms", 0L).putLong("farm_since_ms", 0L).apply()
    }

    // ---- Monster, catatan spawn, dan catatan map ----

    fun loadMobs(): List<String> {
        val raw = notes.getString("mobs", null) ?: return DEFAULT_MOBS
        return raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { DEFAULT_MOBS }
    }

    fun addMob(name: String): List<String> {
        val clean = name.trim().take(40)
        val next = if (clean.isEmpty()) loadMobs() else (loadMobs() + clean).distinct()
        notes.edit().putString("mobs", next.joinToString("\n")).apply()
        return next
    }

    fun removeMob(name: String): List<String> {
        val next = loadMobs().filterNot { it == name }.ifEmpty { DEFAULT_MOBS }
        notes.edit().putString("mobs", next.joinToString("\n")).apply()
        if (loadSelectedMob() == name) saveSelectedMob(next.first())
        return next
    }

    fun loadSelectedMob(): String {
        val mobs = loadMobs()
        return notes.getString("selected_mob", null)?.takeIf { it in mobs } ?: mobs.first()
    }

    fun saveSelectedMob(name: String) {
        notes.edit().putString("selected_mob", name).apply()
    }

    fun loadSpawnNotes(): String = notes.getString("spawn_notes", "") ?: ""

    fun saveSpawnNotes(value: String) {
        notes.edit().putString("spawn_notes", value).apply()
    }

    /** Tambah satu baris berstempel waktu; mengembalikan seluruh catatan terbaru. */
    fun appendSpawnLog(mob: String): String = appendLine("spawn_notes", "[${stamp()}] Respawn: $mob")

    fun loadMapNotes(): String = notes.getString("map_notes", "") ?: ""

    fun saveMapNotes(value: String) {
        notes.edit().putString("map_notes", value).apply()
    }

    fun appendMapLog(point: String): String = appendLine("map_notes", "[${stamp()}] $point")

    private fun appendLine(key: String, line: String): String {
        val current = notes.getString(key, "") ?: ""
        val next = (if (current.isBlank()) line else current.trimEnd('\n') + "\n" + line).takeLast(20000)
        notes.edit().putString(key, next).apply()
        return next
    }

    private fun stamp(): String =
        java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

    // ---- Speed monitor manual ----

    fun loadSpeedCurrent(): Int = notes.getInt("speed_current", 100)

    fun loadSpeedTarget(): Int = notes.getInt("speed_target", 120)

    fun saveSpeedCurrent(value: Int) {
        notes.edit().putInt("speed_current", value.coerceIn(0, 99999999)).apply()
    }

    fun saveSpeedTarget(value: Int) {
        notes.edit().putInt("speed_target", value.coerceIn(0, 99999999)).apply()
    }

    // ---- Hasil verifikasi dex terakhir ----

    fun saveDexScan(s: DexScanSummary) {
        val byFeature = JSONObject().also { o -> s.foundByFeature.forEach { (k, v) -> o.put(k, v) } }
        val json = JSONObject().apply {
            put("fileName", s.fileName); put("dexVersion", s.dexVersion); put("dexFileCount", s.dexFileCount)
            put("stringCount", s.stringCount); put("bundledFound", s.bundledFound); put("bundledTotal", s.bundledTotal)
            put("foundByFeature", byFeature); put("candidateCount", s.candidateCount)
            put("candidateSamples", JSONArray(s.candidateSamples)); put("scannedAt", s.scannedAt)
        }
        prefs.edit().putString("dex_scan", json.toString()).apply()
    }

    fun loadDexScan(): DexScanSummary? {
        val raw = prefs.getString("dex_scan", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val f = o.getJSONObject("foundByFeature")
            val samples = o.getJSONArray("candidateSamples")
            DexScanSummary(
                fileName = o.getString("fileName"), dexVersion = o.getString("dexVersion"),
                dexFileCount = o.getInt("dexFileCount"), stringCount = o.getInt("stringCount"),
                bundledFound = o.getInt("bundledFound"), bundledTotal = o.getInt("bundledTotal"),
                foundByFeature = f.keys().asSequence().associateWith { f.getInt(it) },
                candidateCount = o.getInt("candidateCount"),
                candidateSamples = (0 until samples.length()).map { samples.getString(it) },
                scannedAt = o.getLong("scannedAt")
            )
        }.getOrNull()
    }

    companion object {
        val DEFAULT_TASKS = listOf(
            "Periksa daily quest" to "Quest", "Catat objective aktif" to "Quest",
            "Kumpulkan item target" to "Farming", "Catat lokasi farming" to "Farming",
            "Review quest selesai" to "Quest"
        )
        val DEFAULT_MOBS = listOf("Dark Wolf", "Orc Warrior", "Forest Guardian")
        val MAP_POINTS = listOf(
            "Rute farming A" to "Catat titik awal, jalur, dan waktu tempuh secara manual.",
            "Rute farming B" to "Gunakan untuk membandingkan hasil farming antarsesi.",
            "Area favorit" to "Simpan nama area dan catatan pribadi di sini."
        )
    }
}

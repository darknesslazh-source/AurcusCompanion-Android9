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

    fun loadTasks(): List<QuestTask> {
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
}

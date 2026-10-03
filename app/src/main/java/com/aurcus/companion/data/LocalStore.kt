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
                    completed = o.optBoolean("completed", false),
                    hidden = o.optBoolean("hidden", false)
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
                put("hidden", task.hidden)
            })
        }
        prefs.edit().putString("tasks", array.toString()).apply()
    }

    fun getString(key: String, default: String = ""): String = prefs.getString(key, default) ?: default
    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getBool(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    fun putBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    fun loadLocations(): List<SavedLocation> {
        val array = runCatching { JSONArray(prefs.getString("locations", "[]")) }.getOrNull() ?: JSONArray()
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                SavedLocation(o.getString("id"), o.getString("name"), o.optString("note", ""))
            }.getOrNull()
        }
    }

    fun saveLocations(list: List<SavedLocation>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("note", it.note)) }
        prefs.edit().putString("locations", array.toString()).apply()
    }
}

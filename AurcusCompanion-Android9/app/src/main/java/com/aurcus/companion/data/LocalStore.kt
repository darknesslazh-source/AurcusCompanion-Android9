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
}

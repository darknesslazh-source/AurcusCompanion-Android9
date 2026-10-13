package com.aurcus.companion.monitor

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

data class LocalPerformanceSnapshot(
    val companionPssKb: Long,
    val availableMemoryMb: Long,
    val lowMemory: Boolean
)

/** Measures this companion app and general device memory, not the game process. */
object PerformanceMonitor {
    fun snapshot(context: Context): LocalPerformanceSnapshot {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return LocalPerformanceSnapshot(
            companionPssKb = Debug.getPss(),
            availableMemoryMb = info.availMem / (1024L * 1024L),
            lowMemory = info.lowMemory
        )
    }
}

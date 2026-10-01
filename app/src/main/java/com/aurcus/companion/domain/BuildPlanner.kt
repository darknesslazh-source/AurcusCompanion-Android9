package com.aurcus.companion.domain

import com.aurcus.companion.data.BuildPreset
import com.aurcus.companion.data.BuildSummary

object BuildPlanner {
    fun analyze(build: BuildPreset): BuildSummary {
        val str = build.strength.coerceAtLeast(0)
        val vit = build.vitality.coerceAtLeast(0)
        val dex = build.dexterity.coerceAtLeast(0)
        val int = build.intelligence.coerceAtLeast(0)
        val total = str + vit + dex + int
        if (total == 0) return BuildSummary(0, 0, 0)
        return BuildSummary(
            totalPoints = total,
            physicalFocusPercent = ((str + dex) * 100.0 / total).toInt(),
            magicFocusPercent = (int * 100.0 / total).toInt()
        )
    }
}

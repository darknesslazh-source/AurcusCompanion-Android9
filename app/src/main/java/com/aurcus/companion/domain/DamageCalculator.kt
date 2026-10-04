package com.aurcus.companion.domain

import kotlin.math.max
import kotlin.math.roundToInt

data class DamageRange(val min: Int, val max: Int, val average: Int)

/** Kalkulator manual: hasil murni dari angka yang Anda masukkan, bukan dari game. */
object DamageCalculator {
    fun estimate(atk: Int, def: Int, critPercent: Int): DamageRange {
        val base = max(1, atk - def / 2)
        val crit = critPercent.coerceIn(0, 100) / 100.0
        val low = (base * 0.9).roundToInt()
        val high = (base * (1.0 + crit) * 1.2).roundToInt()
        return DamageRange(low, high, ((low + high) / 2.0).roundToInt())
    }
}

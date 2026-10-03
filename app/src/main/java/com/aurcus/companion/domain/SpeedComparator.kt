package com.aurcus.companion.domain

/** Membandingkan dua angka kecepatan yang Anda catat sendiri (persen). */
object SpeedComparator {
    fun percentChange(normal: Int, current: Int): Int =
        if (normal <= 0) 0 else ((current - normal) * 100.0 / normal).toInt()
}

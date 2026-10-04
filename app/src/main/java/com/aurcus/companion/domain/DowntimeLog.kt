package com.aurcus.companion.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Satu kejadian server bermasalah. endMs == null berarti masih berlangsung. */
data class DowntimeEvent(val startMs: Long, val endMs: Long?, val note: String = "")

data class DowntimeSummary(val count: Int, val totalMs: Long, val longestMs: Long, val averageMs: Long)

/** Pencatat manual downtime server: murni catatan lokal, tidak menyentuh game atau jaringan. */
object DowntimeLog {

    fun durationMs(e: DowntimeEvent, nowMs: Long): Long =
        ((e.endMs ?: nowMs) - e.startMs).coerceAtLeast(0L)

    fun ongoing(events: List<DowntimeEvent>): DowntimeEvent? = events.firstOrNull { it.endMs == null }

    /** Mulai kejadian baru; diabaikan bila sudah ada yang berlangsung. Terbaru di depan. */
    fun start(events: List<DowntimeEvent>, nowMs: Long, note: String = ""): List<DowntimeEvent> =
        if (ongoing(events) != null) events
        else (listOf(DowntimeEvent(nowMs, null, clean(note))) + events).take(MAX_EVENTS)

    /** Tutup kejadian yang sedang berlangsung. */
    fun end(events: List<DowntimeEvent>, nowMs: Long): List<DowntimeEvent> =
        events.map { if (it.endMs == null) it.copy(endMs = maxOf(nowMs, it.startMs)) else it }

    fun summarize(events: List<DowntimeEvent>, nowMs: Long): DowntimeSummary {
        if (events.isEmpty()) return DowntimeSummary(0, 0, 0, 0)
        val d = events.map { durationMs(it, nowMs) }
        return DowntimeSummary(events.size, d.sum(), d.max(), d.sum() / events.size)
    }

    fun format(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return when { h > 0 -> "${h}j ${m}m"; m > 0 -> "${m}m ${sec}d"; else -> "${sec}d" }
    }

    /** Teks laporan siap salin untuk dukungan resmi operator game. */
    fun buildReport(events: List<DowntimeEvent>, nowMs: Long, playerName: String = ""): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val sum = summarize(events, nowMs)
        return buildString {
            appendLine("Laporan gangguan server — Aurcus Online")
            if (playerName.isNotBlank()) appendLine("Karakter: ${playerName.trim()}")
            appendLine("Jumlah kejadian: ${sum.count} • Total: ${format(sum.totalMs)} • Terlama: ${format(sum.longestMs)}")
            appendLine()
            events.sortedBy { it.startMs }.forEachIndexed { i, e ->
                val end = e.endMs?.let { fmt.format(Date(it)) } ?: "masih berlangsung"
                append("${i + 1}. ${fmt.format(Date(e.startMs))} → $end (${format(durationMs(e, nowMs))})")
                if (e.note.isNotBlank()) append(" — ${e.note}")
                appendLine()
            }
        }.trimEnd()
    }

    fun encode(events: List<DowntimeEvent>): String =
        events.joinToString("\n") { "${it.startMs}|${it.endMs ?: ""}|${clean(it.note)}" }

    fun decode(raw: String): List<DowntimeEvent> = raw.split("\n").mapNotNull { line ->
        val p = line.split("|", limit = 3)
        val start = p.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        DowntimeEvent(start, p.getOrNull(1)?.toLongOrNull(), p.getOrNull(2).orEmpty())
    }

    private fun clean(s: String) = s.replace('|', '/').replace('\n', ' ').trim().take(80)
    private const val MAX_EVENTS = 100
}

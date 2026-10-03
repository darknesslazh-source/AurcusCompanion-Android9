package com.aurcus.companion.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aurcus.companion.data.LocalStore
import com.aurcus.companion.data.QuestTask
import com.aurcus.companion.data.SavedLocation
import com.aurcus.companion.domain.DamageCalculator
import com.aurcus.companion.domain.DamageRange
import com.aurcus.companion.domain.SpeedComparator
import com.aurcus.companion.modules.ModuleRegistry
import com.aurcus.companion.monitor.PerformanceMonitor
import com.aurcus.companion.overlay.FloatingOverlayService
import kotlinx.coroutines.delay

private val Ink = Color(0xFF071321)
private val Panel = Color(0xFF10243A)
private val Gold = Color(0xFFE8B84B)
private val Muted = Color(0xFFA8BED1)
private val Green = Color(0xFF6DE3B3)
private val Swatches = listOf(
    Color(0xFF22B8F0), Color(0xFF3D8BE0), Color(0xFF7B5FE0), Color(0xFFE8B84B),
    Color(0xFFE04B4B), Color(0xFF1FBFA8), Color(0xFF2A3A4C)
)
private val Mobs = listOf("Dark Wolf", "Orc Warrior", "Forest Guardian")
private val Cyan @Composable get() = LocalAccent.current
private val LocalAccent = compositionLocalOf { Swatches[0] }

@Composable
fun CompanionScreen() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var page by remember { mutableStateOf("mod") }
    var accentIdx by remember { mutableIntStateOf(store.getInt("accent", 0).coerceIn(0, Swatches.lastIndex)) }
    val modules = remember { ModuleRegistry.all() }

    CompositionLocalProvider(LocalAccent provides Swatches[accentIdx]) {
        Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
            Header()
            Row(Modifier.weight(1f)) {
                NavigationRail(containerColor = Color(0xFF0B1928), modifier = Modifier.fillMaxHeight()) {
                    modules.forEach { m ->
                        NavigationRailItem(
                            selected = page == m.id, onClick = { page = m.id },
                            icon = { Text(m.glyph, style = MaterialTheme.typography.titleMedium) },
                            label = { Text(m.title, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Cyan, selectedTextColor = Cyan,
                                indicatorColor = Color(0xFF173A56), unselectedIconColor = Muted, unselectedTextColor = Muted)
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (page) {
                        "mod" -> ModPage(store)
                        "quest" -> QuestPage(store)
                        "map" -> MapPage(store)
                        "spawn" -> SpawnPage(store)
                        "stat" -> StatPage(store)
                        else -> SettingPage(store, accentIdx) { accentIdx = it; store.putInt("accent", it) }
                    }
                }
            }
        }
    }
}

@Composable private fun Header() {
    Row(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B1B2D), Color(0xFF123858))))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(Color(0xFF153D5D), CircleShape), contentAlignment = Alignment.Center) {
            Text("✦", color = Gold, style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("AURCUS COMPANION", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text("Your Adventure, With Better Tools.", color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        Surface(color = Color(0xFF123D37), shape = RoundedCornerShape(50)) {
            Text("● Siap", color = Green, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/* ───────────── MOD ───────────── */
@Composable private fun ModPage(store: LocalStore) {
    val context = LocalContext.current
    var atk by remember { mutableStateOf("500") }
    var def by remember { mutableStateOf("200") }
    var crit by remember { mutableStateOf("15") }
    var result by remember { mutableStateOf<DamageRange?>(null) }
    val labels = listOf("Damage HUD" to "dmg_hud", "Attack Range" to "range", "Skill AoE" to "aoe", "Skill Effect" to "fx")
    val values = remember { mutableStateMapOf<String, Float>().apply { labels.forEach { (_, k) -> put(k, store.getInt("scale_$k", 100).toFloat()) } } }

    Page("Mod Menu", "Alat hitung & pratinjau visual lokal") {
        item {
            Box(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Damage Calculator", color = Cyan, fontWeight = FontWeight.Bold)
                    NumField("ATK", atk) { atk = it }
                    NumField("DEF", def) { def = it }
                    NumField("CRIT %", crit) { crit = it }
                    Button(onClick = { result = DamageCalculator.estimate(atk.toIntOrNull() ?: 0, def.toIntOrNull() ?: 0, crit.toIntOrNull() ?: 0) },
                        modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) { Text("Hitung") }
                    result?.let {
                        Text("Perkiraan Damage", color = Muted, style = MaterialTheme.typography.labelMedium)
                        Text("%,d - %,d".format(it.min, it.max), color = Green, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        items(labels.size) { i ->
            val (title, key) = labels[i]
            val v = values[key] ?: 100f
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(12.dp)) {
                Row { Text(title, color = Color.White, modifier = Modifier.weight(1f)); Text("${v.toInt()}%", color = Cyan, fontWeight = FontWeight.Bold) }
                Slider(value = v, onValueChange = { values[key] = it }, onValueChangeFinished = { store.putInt("scale_$key", (values[key] ?: 100f).toInt()) },
                    valueRange = 50f..300f, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Cyan))
            }
        }
        item {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                    Toast.makeText(context, "Izinkan tampil di atas aplikasi lain, lalu kembali.", Toast.LENGTH_LONG).show()
                } else {
                    val i = Intent(context, FloatingOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
                }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
                Text("Aktifkan Jendela Mengambang", fontWeight = FontWeight.Bold)
            }
        }
        item { Note("Semua nilai di halaman ini adalah kalkulator/pratinjau lokal. Tidak mengubah damage, jangkauan, atau area skill di game.") }
    }
}

/* ───────────── QUEST ───────────── */
@Composable private fun QuestPage(store: LocalStore) {
    var tasks by remember { mutableStateOf(store.loadTasks().ifEmpty {
        listOf(QuestTask("q1", "Kalahkan Dark Wolf", "Quest", 10, 3), QuestTask("q2", "Kumpulkan 5 Crystal", "Quest", 5, 2), QuestTask("q3", "Temukan Wilayah Rahasia", "Quest", 1, 0))
    }) }
    var tab by remember { mutableIntStateOf(0) }
    var newTitle by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) { while (running) { delay(1000); seconds++ } }
    fun update(t: List<QuestTask>) { tasks = t; store.saveTasks(t) }
    val shown = when (tab) { 0 -> tasks.filter { !it.completed && !it.hidden }; 1 -> tasks.filter { it.completed }; else -> tasks.filter { it.hidden } }

    Page("Quest Tracker", "Checklist misi & progres — dicatat manual") {
        item { TabRow(selectedTabIndex = tab, containerColor = Panel, contentColor = Cyan) {
            listOf("Aktif", "Selesai", "Tersembunyi").forEachIndexed { i, s -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(s) }) } } }
        items(shown.size) { i ->
            val t = shown[i]
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(12.dp)) {
                Text(t.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(progress = { (t.currentCount.toFloat() / t.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), color = Cyan)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${t.currentCount}/${t.targetCount}", color = Muted, modifier = Modifier.weight(1f))
                    TextButton(onClick = { update(tasks.map { if (it.id == t.id) it.copy(currentCount = (it.currentCount - 1).coerceAtLeast(0), completed = false) else it }) }) { Text("−") }
                    TextButton(onClick = { update(tasks.map { if (it.id == t.id) { val c = (it.currentCount + 1).coerceAtMost(it.targetCount); it.copy(currentCount = c, completed = c >= it.targetCount) } else it }) }) { Text("+") }
                    TextButton(onClick = { update(tasks.map { if (it.id == t.id) it.copy(hidden = !it.hidden) else it }) }) { Text(if (t.hidden) "Tampilkan" else "Sembunyikan") }
                }
            }
        }
        item {
            OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Quest / tugas baru") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Button(onClick = { if (newTitle.isNotBlank()) { update(tasks + QuestTask("t${System.currentTimeMillis()}", newTitle.trim(), "Quest", 1)); newTitle = "" } },
                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) { Text("+ Tambah") }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Color(0xFF102F48), RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("Sesi Farming (timer manual)", color = Muted, style = MaterialTheme.typography.labelMedium)
                Text("%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60), color = Green, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { running = !running }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E9E57))) { Text(if (running) "Jeda" else "Mulai") }
                    OutlinedButton(onClick = { running = false; seconds = 0 }, modifier = Modifier.weight(1f)) { Text("Reset") }
                }
            }
        }
        item { Note("Timer hanya mencatat durasi. Tidak ada input otomatis ke game.") }
    }
}

/* ───────────── MAP ───────────── */
@Composable private fun MapPage(store: LocalStore) {
    var locs by remember { mutableStateOf(store.loadLocations()) }
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("") }
    val accent = Cyan
    fun update(l: List<SavedLocation>) { locs = l; store.saveLocations(l) }

    Page("Map & Rute", "Catatan lokasi dan rute farming pribadi") {
        item {
            Canvas(Modifier.fillMaxWidth().height(170.dp).background(Color(0xFF1B3A2C), RoundedCornerShape(16.dp)).border(1.dp, accent, RoundedCornerShape(16.dp))) {
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(accent.copy(alpha = 0.5f), radius = size.minDimension / 2.3f, center = c, style = Stroke(2f))
                drawCircle(accent.copy(alpha = 0.25f), radius = size.minDimension / 4f, center = c, style = Stroke(2f))
                locs.forEachIndexed { i, _ ->
                    val a = i * 2.4f
                    val r = size.minDimension / 2.6f * (0.35f + 0.6f * ((i * 37 % 10) / 10f))
                    drawCircle(Gold, radius = 7f, center = Offset(c.x + r * kotlin.math.cos(a), c.y + r * kotlin.math.sin(a)))
                }
                drawCircle(Color.White, radius = 6f, center = c)
            }
        }
        items(locs.size) { i ->
            val l = locs[i]
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("${i + 1}. ${l.name}", color = Color.White, fontWeight = FontWeight.SemiBold); if (l.note.isNotBlank()) Text(l.note, color = Muted, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { update(locs.filter { it.id != l.id }) }) { Text("Hapus") }
            }
        }
        item {
            OutlinedTextField(name, { name = it }, label = { Text("Nama lokasi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (name.isNotBlank()) { update(locs + SavedLocation("l${System.currentTimeMillis()}", name.trim(), note.trim())); name = ""; note = "" } },
                    modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) { Text("Simpan Lokasi") }
                OutlinedButton(onClick = { route = locs.joinToString("  →  ") { it.name } }, modifier = Modifier.weight(1f)) { Text("Buat Rute") }
            }
        }
        if (route.isNotBlank()) item { Note("Rute: $route") }
    }
}

/* ───────────── SPAWN ───────────── */
@Composable private fun SpawnPage(store: LocalStore) {
    var mob by remember { mutableStateOf(Mobs[0]) }
    var count by remember { mutableIntStateOf(5) }
    var place by remember { mutableStateOf("") }
    var log by remember { mutableStateOf(store.getString("spawn_log").split("\n").filter { it.isNotBlank() }) }

    Page("Spawn Tracker", "Catat pengamatan kemunculan monster secara manual") {
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mobs.forEach { m -> FilterChip(selected = mob == m, onClick = { mob = m }, label = { Text(m) }) } } }
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { count = (count - 1).coerceAtLeast(1) }) { Text("−") }
            Text("$count", color = Color.White, modifier = Modifier.padding(horizontal = 18.dp), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { count++ }) { Text("+") }
        } }
        item { OutlinedTextField(place, { place = it }, label = { Text("Lokasi") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { Button(onClick = {
            val line = "${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())} • $mob ×$count • ${place.ifBlank { "-" }}"
            log = (listOf(line) + log).take(50); store.putString("spawn_log", log.joinToString("\n"))
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B5FE0))) { Text("Catat Pengamatan") } }
        items(log.size) { i -> Text(log[i], color = Muted, modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(10.dp), style = MaterialTheme.typography.bodySmall) }
        item { Note("Ini hanya log catatan. Tidak memunculkan monster di game.") }
    }
}

/* ───────────── STAT ───────────── */
@Composable private fun StatPage(store: LocalStore) {
    val context = LocalContext.current
    var normal by remember { mutableStateOf(store.getString("speed_normal", "100")) }
    var current by remember { mutableStateOf(store.getString("speed_now", "100")) }
    val n = normal.toIntOrNull() ?: 0
    val c = current.toIntOrNull() ?: 0
    val snap = remember { PerformanceMonitor.snapshot(context) }

    Page("Stat", "Bandingkan statistik kecepatan yang Anda catat") {
        item { NumField("Kecepatan normal (%)", normal) { normal = it; store.putString("speed_normal", it) } }
        item { NumField("Kecepatan saat ini (%)", current) { current = it; store.putString("speed_now", it) } }
        item {
            Column(Modifier.fillMaxWidth().background(Color(0xFF102F48), RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("PERBANDINGAN", color = Muted, style = MaterialTheme.typography.labelMedium)
                Text("$n% → $c%  (${SpeedComparator.percentChange(n, c)}%)", color = Green, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                LinearProgressIndicator(progress = { (c / 300f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = Cyan)
            }
        }
        item { Note("Performa companion\nPSS: ${snap.companionPssKb} KB • Memori tersedia: ${snap.availableMemoryMb} MB • Low-memory: ${snap.lowMemory}") }
    }
}

/* ───────────── SETTING ───────────── */
@Composable private fun SettingPage(store: LocalStore, accentIdx: Int, onAccent: (Int) -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    Page("Setting", "Tema dan data lokal") {
        item { Text("Warna aksen", color = Color.White, fontWeight = FontWeight.SemiBold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Swatches.forEachIndexed { i, col ->
                Box(Modifier.size(36.dp).background(col, RoundedCornerShape(10.dp))
                    .border(if (i == accentIdx) 2.dp else 0.dp, Color.White, RoundedCornerShape(10.dp)).clickable { onAccent(i) })
            }
        } }
        item { OutlinedButton(onClick = {
            if (confirmReset) { store.saveTasks(emptyList()); store.saveLocations(emptyList()); store.putString("spawn_log", ""); confirmReset = false } else confirmReset = true
        }, modifier = Modifier.fillMaxWidth()) { Text(if (confirmReset) "Tekan lagi untuk menghapus semua data" else "Reset data lokal") } }
        item { Note("Aurcus Companion v1.4.0 — aplikasi pendamping mandiri. Tidak terhubung ke proses atau server game.") }
    }
}

/* ───────────── helpers ───────────── */
@Composable private fun Page(title: String, subtitle: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Column { Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) } }
        content()
    }
}

@Composable private fun Note(text: String) {
    Text(text, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().background(Color(0xFF13283B), RoundedCornerShape(12.dp)).padding(12.dp))
}

@Composable private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(it.filter(Char::isDigit).take(8)) }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

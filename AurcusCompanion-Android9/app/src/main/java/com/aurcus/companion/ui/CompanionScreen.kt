package com.aurcus.companion.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aurcus.companion.data.*
import com.aurcus.companion.domain.BuildPlanner
import com.aurcus.companion.domain.EquipmentComparator
import com.aurcus.companion.monitor.PerformanceMonitor
import com.aurcus.companion.overlay.FloatingOverlayService
import kotlinx.coroutines.delay

private val demoItems = listOf(
    Item("demo-001", "Sample Blade", "Weapon", attack = 24, notes = "Contoh fiktif; bukan database resmi"),
    Item("demo-002", "Sample Robe", "Body", defense = 12, magic = 8, notes = "Contoh fiktif; bukan database resmi"),
    Item("demo-003", "Sample Charm", "Accessory", attack = 3, magic = 10, notes = "Contoh fiktif; bukan database resmi")
)
private val demoMaps = listOf(
    "Map / area (catatan manual)" to "Tambahkan nama area dan koordinat yang kamu catat sendiri.",
    "Rute farming A" to "Catatan rute lokal; tidak mengirim perintah perpindahan ke game.",
    "Rute farming B" to "Catatan rute lokal; tidak membaca lokasi game secara otomatis."
)
private val demoMobs = listOf("Mob / target A", "Mob / target B", "Boss / target C")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionScreen() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var attack by remember { mutableStateOf("100") }
    var skillMultiplier by remember { mutableStateOf("1.5") }
    var critPercent by remember { mutableStateOf("0") }
    var speedStat by remember { mutableStateOf("100") }
    var speedTarget by remember { mutableStateOf("120") }
    var mapNotes by remember { mutableStateOf("") }
    var selectedMob by remember { mutableStateOf(demoMobs.first()) }
    var spawnNotes by remember { mutableStateOf("") }
    var farmRunning by remember { mutableStateOf(false) }
    var farmSeconds by remember { mutableIntStateOf(0) }
    var tasks by remember { mutableStateOf(store.loadTasks()) }
    val tabs = listOf("Home", "Damage", "Map", "Spawn", "Farm", "Speed")

    LaunchedEffect(farmRunning) {
        while (farmRunning) {
            delay(1000)
            farmSeconds += 1
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Column {
            Text("AURCUS COMPANION")
            Text("Android 9+ • Companion lokal • v1.1.0", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary)
        } }) },
        bottomBar = { NavigationBar {
            tabs.forEachIndexed { index, label ->
                NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index },
                    icon = { Text(listOf("⌂", "⚔", "⌖", "◎", "◷", "➤")[index]) }, label = { Text(label) })
            }
        } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (selectedTab) {
                0 -> {
                    item { Text("Companion dashboard", style = MaterialTheme.typography.headlineSmall) }
                    item { InfoCard("Floating overlay", "Panel mengambang bisa dipindah, dilipat, dan ditutup. Tidak membaca memori game atau mengirim perintah ke server.") }
                    item {
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)))
                                Toast.makeText(context, "Izinkan tampil di atas aplikasi lain, lalu kembali dan tekan tombol lagi.", Toast.LENGTH_LONG).show()
                            } else {
                                val intent = Intent(context, FloatingOverlayService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
                                Toast.makeText(context, "Panel mengambang diaktifkan.", Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Aktifkan Jendela Mengambang") }
                    }
                    item { InfoCard("Batas penggunaan", "Speed dan damage adalah kalkulator/catatan lokal; Map adalah catatan rute; Spawn adalah log manual; Farm adalah timer. Tidak ada cheat, injeksi, otomasi input, atau bypass keamanan/server.") }
                    item { InfoCard("Data", "Item dan area bawaan hanyalah contoh. ZIP game yang diunggah berisi paket aplikasi hasil build (DEX, native libraries, aset), bukan source Kotlin lengkap. Tidak mengubah APK game.") }
                }
                1 -> {
                    item { Text("Damage calculator", style = MaterialTheme.typography.headlineSmall) }
                    item { NumberField("ATK / base damage", attack) { attack = it } }
                    item { OutlinedTextField(value = skillMultiplier, onValueChange = { skillMultiplier = it.filter { c -> c.isDigit() || c == '.' }.take(8) }, label = { Text("Skill multiplier (contoh 1.5)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    item { NumberField("Critical bonus (%)", critPercent) { critPercent = it } }
                    item {
                        val base = (attack.toDoubleOrNull() ?: 0.0) * (skillMultiplier.toDoubleOrNull() ?: 0.0)
                        val crit = (critPercent.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 10000.0) / 100.0
                        InfoCard("Perkiraan lokal", "Normal: ${"%.1f".format(base)}\nDengan bonus kritikal: ${"%.1f".format(base * (1 + crit))}\n\nRumus sederhana untuk perbandingan saja; tidak memodelkan defense, resist, buff, atau formula resmi server.")
                    }
                }
                2 -> {
                    item { Text("Map & route notes", style = MaterialTheme.typography.headlineSmall) }
                    item { InfoCard("Navigasi aman", "Catat area, rute, dan koordinat sendiri. Companion tidak memindahkan karakter atau mengirim teleport request.") }
                    items(demoMaps) { (title, body) -> InfoCard(title, body) }
                    item { OutlinedTextField(value = mapNotes, onValueChange = { mapNotes = it }, label = { Text("Catatan map pribadi") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
                    item { Text("Catatan tersimpan selama layar ini terbuka.", style = MaterialTheme.typography.bodySmall) }
                }
                3 -> {
                    item { Text("Spawn / mob log", style = MaterialTheme.typography.headlineSmall) }
                    item { InfoCard("Mode manual", "Pilih target dan catat kemunculan atau respawn yang kamu amati. Panel ini tidak melakukan spawn mob pada server publik.") }
                    item { Text("Target", style = MaterialTheme.typography.titleMedium) }
                    items(demoMobs) { mob ->
                        Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(mob, modifier = Modifier.weight(1f))
                            RadioButton(selected = selectedMob == mob, onClick = { selectedMob = mob })
                        } }
                    }
                    item { OutlinedTextField(value = spawnNotes, onValueChange = { spawnNotes = it }, label = { Text("Log spawn / waktu / jumlah") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
                    item { InfoCard("Target dipilih", selectedMob) }
                }
                4 -> {
                    item { Text("Farm session", style = MaterialTheme.typography.headlineSmall) }
                    item { InfoCard("Timer sesi", "Waktu: ${farmSeconds / 3600}j ${(farmSeconds % 3600) / 60}m ${farmSeconds % 60}d\nTimer hanya mencatat durasi. Tidak menekan tombol game, menyerang otomatis, atau mengirim input ke server.") }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { farmRunning = !farmRunning }, modifier = Modifier.weight(1f)) { Text(if (farmRunning) "Jeda" else "Mulai timer") }
                        OutlinedButton(onClick = { farmRunning = false; farmSeconds = 0 }, modifier = Modifier.weight(1f)) { Text("Reset") }
                    } }
                    item { Text("Checklist farming", style = MaterialTheme.typography.titleMedium) }
                    items(tasks, key = { it.id }) { task ->
                        Card { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(task.title, style = MaterialTheme.typography.titleMedium); Text("${task.category} • ${task.currentCount}/${task.targetCount}") }
                            Checkbox(checked = task.completed, onCheckedChange = { checked -> tasks = tasks.map { if (it.id == task.id) it.copy(completed = checked) else it }; store.saveTasks(tasks) })
                        } }
                    }
                    item { Button(onClick = { tasks = tasks + QuestTask("task-${System.currentTimeMillis()}", "Manual farming task", "Farming"); store.saveTasks(tasks) }) { Text("Tambah tugas") } }
                }
                else -> {
                    item { Text("Speed / movement notes", style = MaterialTheme.typography.headlineSmall) }
                    item { InfoCard("Bukan pengubah kecepatan game", "Gunakan kolom ini untuk membandingkan statistik yang kamu masukkan secara manual. Companion tidak mengubah gerakan karakter atau nilai speed server.") }
                    item { NumberField("Speed stat saat ini", speedStat) { speedStat = it } }
                    item { NumberField("Target / benchmark", speedTarget) { speedTarget = it } }
                    item {
                        val current = speedStat.toDoubleOrNull() ?: 0.0
                        val target = speedTarget.toDoubleOrNull() ?: 0.0
                        val delta = target - current
                        InfoCard("Perbandingan", "Saat ini: $current\nTarget: $target\nSelisih: ${"%.1f".format(delta)}\n${if (delta > 0) "Target lebih tinggi dari nilai saat ini." else if (delta < 0) "Nilai saat ini sudah melampaui target." else "Nilai saat ini sama dengan target."}")
                    }
                    item {
                        val snapshot = remember { PerformanceMonitor.snapshot(context) }
                        InfoCard("Performa companion", "PSS aplikasi: ${snapshot.companionPssKb} KB\nMemori perangkat tersedia: ${snapshot.availableMemoryMb} MB\nLow-memory: ${snapshot.lowMemory}\nTidak mengukur FPS game.")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { onValueChange(it.filter(Char::isDigit).take(8)) },
        label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

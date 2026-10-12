package com.aurcus.companion.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aurcus.companion.data.BuildModification
import com.aurcus.companion.data.GameFeature
import com.aurcus.companion.data.GameFeatureCatalog
import com.aurcus.companion.data.LocalStore
import com.aurcus.companion.data.ModTemplate
import com.aurcus.companion.data.ModificationCatalog
import com.aurcus.companion.data.QuestTask
import com.aurcus.companion.monitor.PerformanceMonitor
import com.aurcus.companion.overlay.FloatingOverlayService
import kotlinx.coroutines.delay

private val Ink = Color(0xFF071321)
private val Panel = Color(0xFF10243A)
private val Cyan = Color(0xFF22B8F0)
private val Gold = Color(0xFFE8B84B)
private val Muted = Color(0xFFA8BED1)
private val demoMaps = listOf(
    "Rute farming A" to "Catat titik awal, jalur, dan waktu tempuh secara manual.",
    "Rute farming B" to "Gunakan untuk membandingkan hasil farming antarsesi.",
    "Area favorit" to "Simpan nama area dan catatan pribadi di sini."
)
private val demoMobs = listOf("Dark Wolf", "Orc Warrior", "Forest Guardian")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionScreen() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var damageHudScale by remember { mutableStateOf(100f) }
    var attackRangeScale by remember { mutableStateOf(100f) }
    var skillAoEScale by remember { mutableStateOf(100f) }
    var skillEffectScale by remember { mutableStateOf(100f) }
    var speedStat by remember { mutableStateOf("100") }
    var speedTarget by remember { mutableStateOf("120") }
    var mapNotes by remember { mutableStateOf("") }
    var selectedMob by remember { mutableStateOf(demoMobs.first()) }
    var spawnNotes by remember { mutableStateOf("") }
    var farmRunning by remember { mutableStateOf(false) }
    var farmSeconds by remember { mutableIntStateOf(0) }
    var tasks by remember { mutableStateOf(store.loadTasks()) }
    var mods by remember { mutableStateOf(store.loadMods()) }
    var favorites by remember { mutableStateOf(store.loadFavorites()) }
    var featureQuery by remember { mutableStateOf("") }
    var expandedFeature by remember { mutableStateOf<String?>(null) }
    val tabs = listOf("Home", "Menu", "Build", "Mods", "Map", "Spawn", "Farm", "Speed")
    val glyphs = listOf("⌂", "☰", "◈", "⚔", "⌖", "◎", "◷", "↗")

    // Overlay mengambang berbagi data yang sama; muat ulang saat pindah tab.
    LaunchedEffect(selectedTab) {
        mods = store.loadMods()
        favorites = store.loadFavorites()
    }

    LaunchedEffect(farmRunning) {
        while (farmRunning) { delay(1000); farmSeconds += 1 }
    }

    Scaffold(
        containerColor = Ink,
        topBar = {
            Column(
                Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0B1B2D), Color(0xFF123858))))
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(Color(0xFF153D5D), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Text("✦", color = Gold, style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AURCUS", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                        Text("COMPANION  •  v1.4.0", style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                    Surface(color = Color(0xFF123D37), shape = RoundedCornerShape(50)) {
                        Text("● READY", color = Color(0xFF63E6BE), modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Your Adventure, With Better Tools.", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Companion lokal • overlay bisa dipindah • Android 9+", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        },
        bottomBar = { ScrollableBottomBar(tabs, glyphs, selectedTab) { selectedTab = it } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item { SectionTitle("Dashboard", "Semua alat pendamping dalam satu panel") }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("FLOATING COMPANION", color = Cyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text("Buka panel ringkas di atas Aurcus Online atau aplikasi lain.", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                                Button(onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                        Toast.makeText(context, "Izinkan tampil di atas aplikasi lain, lalu kembali ke Companion.", Toast.LENGTH_LONG).show()
                                    } else {
                                        val intent = Intent(context, FloatingOverlayService::class.java)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
                                        Toast.makeText(context, "Floating Companion aktif", Toast.LENGTH_SHORT).show()
                                    }
                                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) {
                                    Text("Aktifkan Jendela Mengambang", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item { StatStrip() }
                    item { FeatureCard("☰", "Menu Fitur Game", "${GameFeatureCatalog.features.size} kategori fitur dengan catatan dan favorit pribadi.", Cyan) }
                    item { FeatureCard("◈", "Daftar Modifikasi Build", "Rencanakan Enhance, Evolve, Remodel, skill, dan crafting; tambah semua sekali ketuk.", Gold) }
                    item { FeatureCard("⚔", "Visual Mod Panel", "Atur skala HUD damage contoh, lingkaran jangkauan, dan pratinjau AoE.", Cyan) }
                    item { FeatureCard("⌖", "Map & Route Notes", "Simpan catatan area, jalur farming, dan titik pengamatan.", Gold) }
                    item { FeatureCard("◎", "Quest • Spawn • Farm", "Checklist, pencatat kemunculan monster, dan timer sesi.", Color(0xFFB69CFF)) }
                    item { InfoCard("Mode companion", "Panel ini tidak menginjeksi proses game, tidak membaca memori game, tidak mengirim input otomatis, dan tidak mengubah nilai di server. Fitur bernama speed/damage hanya monitor atau simulasi lokal.") }
                }
                1 -> {
                    item { SectionTitle("Menu Fitur Game", "${GameFeatureCatalog.features.size} kategori dari indeks classes.dex • favorit tampil di atas") }
                    item { OutlinedTextField(value = featureQuery, onValueChange = { featureQuery = it }, label = { Text("Cari menu") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    val query = featureQuery.trim()
                    val visible = GameFeatureCatalog.sorted(favorites).filter {
                        query.isEmpty() || it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true)
                    }
                    if (visible.isEmpty()) item { InfoCard("Tidak ditemukan", "Tidak ada menu yang cocok dengan \"$query\".") }
                    items(visible, key = { it.id }) { feature ->
                        FeatureMenuCard(
                            feature = feature,
                            store = store,
                            favorite = feature.id in favorites,
                            expanded = expandedFeature == feature.id,
                            onToggleExpand = { expandedFeature = if (expandedFeature == feature.id) null else feature.id },
                            onToggleFavorite = { favorites = store.toggleFavorite(feature.id) }
                        )
                    }
                    item { InfoCard("Catatan", "Menu hanya referensi dan catatan lokal berdasarkan indeks kelas. Tidak membuka atau mengubah fitur di Aurcus Online.") }
                }
                2 -> {
                    val doneCount = mods.count { it.done }
                    item { SectionTitle("Daftar Modifikasi Build", "Rencana modifikasi equipment & karakter — catatan lokal") }
                    item { ResultCard("PROGRES", "$doneCount/${mods.size}", if (mods.isEmpty()) "Belum ada rencana modifikasi" else "modifikasi selesai") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { mods = ModificationCatalog.addAll(mods); store.saveMods(mods) }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) { Text("Tambah semua") }
                            OutlinedButton(onClick = { mods = mods.filterNot { it.done }; store.saveMods(mods) }, modifier = Modifier.weight(1f)) { Text("Hapus selesai") }
                        }
                    }
                    item { SectionTitle("Rencana kamu", "Isi target dan level, centang jika sudah selesai") }
                    if (mods.isEmpty()) item { InfoCard("Daftar kosong", "Ketuk \"Tambah semua\" atau pilih satu jenis modifikasi di bagian Katalog di bawah.") }
                    items(mods, key = { it.id }) { mod ->
                        ModificationCard(
                            mod = mod,
                            onChange = { updated -> mods = mods.map { if (it.id == updated.id) updated else it }; store.saveMods(mods) },
                            onDelete = { mods = mods.filterNot { it.id == mod.id }; store.saveMods(mods) }
                        )
                    }
                    item { SectionTitle("Katalog modifikasi", "Tambahkan satu jenis, boleh lebih dari sekali") }
                    items(ModificationCatalog.templates, key = { "tpl-${it.id}" }) { template ->
                        TemplateCard(template) {
                            mods = mods + ModificationCatalog.fromTemplate(template, "mod-${template.id}-${System.currentTimeMillis()}")
                            store.saveMods(mods)
                        }
                    }
                    item { InfoCard("Catatan", "Ini hanya daftar rencana pribadi. Aplikasi tidak melakukan enhance, remodel, atau evolve dan tidak mengubah item di game.") }
                }
                3 -> {
                    item { SectionTitle("Visual Modification Panel", "Kontrol pratinjau lokal companion — bukan atribut gameplay") }
                    item { VisualSliderCard("Damage HUD scale", "Ukuran angka damage contoh", damageHudScale, { damageHudScale = it }) }
                    item { VisualSliderCard("Attack range preview", "Ukuran lingkaran referensi", attackRangeScale, { attackRangeScale = it }) }
                    item { VisualSliderCard("Skill AoE radius", "Ukuran pratinjau area skill", skillAoEScale, { skillAoEScale = it }) }
                    item { VisualSliderCard("Skill effect scale", "Ukuran efek visual contoh", skillEffectScale, { skillEffectScale = it }) }
                    item { InfoCard("Pratinjau lokal", "Slider hanya mengubah tampilan companion. Damage aktual, hitbox, jangkauan serangan, dan area skill di Aurcus Online tidak berubah.") }
                }
                4 -> {
                    item { SectionTitle("Map & Route", "Catatan lokasi pribadi") }
                    items(demoMaps) { (title, body) -> FeatureCard("⌖", title, body, Cyan) }
                    item { OutlinedTextField(value = mapNotes, onValueChange = { mapNotes = it }, label = { Text("Catatan map pribadi") }, modifier = Modifier.fillMaxWidth(), minLines = 4) }
                    item { Text("Catatan ini hanya tersimpan selama layar terbuka pada versi ini.", color = Muted, style = MaterialTheme.typography.bodySmall) }
                }
                5 -> {
                    item { SectionTitle("Spawn Tracker", "Catat observasi respawn secara manual") }
                    item { Text("Pilih target", color = Muted, style = MaterialTheme.typography.labelLarge) }
                    items(demoMobs) { mob ->
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("◎", color = Gold, style = MaterialTheme.typography.titleLarge)
                                Text(mob, Modifier.weight(1f).padding(start = 10.dp), color = Color.White)
                                RadioButton(selected = selectedMob == mob, onClick = { selectedMob = mob }, colors = RadioButtonDefaults.colors(selectedColor = Cyan))
                            }
                        }
                    }
                    item { OutlinedTextField(value = spawnNotes, onValueChange = { spawnNotes = it }, label = { Text("Waktu, lokasi, jumlah, catatan") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
                    item { InfoCard("Target aktif", selectedMob) }
                }
                6 -> {
                    item { SectionTitle("Farm Session", "Timer dan checklist — tanpa auto-input ke game") }
                    item { ResultCard("DURASI SESI", "%02d:%02d:%02d".format(farmSeconds / 3600, (farmSeconds % 3600) / 60, farmSeconds % 60), if (farmRunning) "Timer sedang berjalan" else "Timer dijeda") }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { farmRunning = !farmRunning }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) { Text(if (farmRunning) "Jeda" else "Mulai timer") }
                        OutlinedButton(onClick = { farmRunning = false; farmSeconds = 0 }, modifier = Modifier.weight(1f)) { Text("Reset") }
                    } }
                    item { SectionTitle("Checklist", "Tandai tujuan yang selesai") }
                    items(tasks, key = { it.id }) { task ->
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("${task.category} • ${task.currentCount}/${task.targetCount}", color = Muted, style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(checked = task.completed, onCheckedChange = { checked -> tasks = tasks.map { if (it.id == task.id) it.copy(completed = checked) else it }; store.saveTasks(tasks) }, colors = CheckboxDefaults.colors(checkedColor = Cyan))
                            }
                        }
                    }
                    item { OutlinedButton(onClick = { tasks = tasks + QuestTask("task-${System.currentTimeMillis()}", "Tugas farming baru", "Farming"); store.saveTasks(tasks) }, modifier = Modifier.fillMaxWidth()) { Text("+ Tambah tugas") } }
                }
                else -> {
                    item { SectionTitle("Speed Monitor", "Bandingkan nilai manual dan statistik perangkat") }
                    item { InfoCard("Monitor aman", "Tidak mengubah kecepatan karakter. Masukkan statistik yang kamu lihat sendiri untuk membandingkan target.") }
                    item { NumberField("Speed saat ini", speedStat) { speedStat = it } }
                    item { NumberField("Target / benchmark", speedTarget) { speedTarget = it } }
                    item {
                        val current = speedStat.toDoubleOrNull() ?: 0.0
                        val target = speedTarget.toDoubleOrNull() ?: 0.0
                        ResultCard("PERBANDINGAN", "${"%.1f".format(current)} → ${"%.1f".format(target)}", "Selisih: ${"%.1f".format(target - current)}")
                    }
                    item {
                        val snapshot = remember { PerformanceMonitor.snapshot(context) }
                        InfoCard("Performa companion", "PSS aplikasi: ${snapshot.companionPssKb} KB\nMemori perangkat tersedia: ${snapshot.availableMemoryMb} MB\nLow-memory: ${snapshot.lowMemory}\nTidak mengukur FPS atau memori game.")
                    }
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}


@Composable private fun VisualSliderCard(title: String, subtitle: String, value: Float, onChange: (Float) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text("${value.toInt()}%", color = Cyan, fontWeight = FontWeight.Bold)
            }
            Slider(value = value, onValueChange = onChange, valueRange = 50f..300f, steps = 24,
                colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Cyan, inactiveTrackColor = Color(0xFF29415D)))
        }
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun StatStrip() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("TOOLS" to "08", "MODE" to "LOCAL", "ANDROID" to "9+").forEach { (label, value) ->
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
                    Text(value, color = Cyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable private fun FeatureCard(icon: String, title: String, body: String, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFF193A55), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text(icon, color = accent, style = MaterialTheme.typography.headlineSmall)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(body, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13283B)), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Gold, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun ResultCard(title: String, value: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF102F48)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(value, color = Color(0xFF6DE3B3), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { onValueChange(it.filter(Char::isDigit).take(8)) },
        label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable private fun ScrollableBottomBar(tabs: List<String>, glyphs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = Color(0xFF0B1928), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            tabs.forEachIndexed { index, label ->
                val active = selected == index
                Column(
                    Modifier.width(66.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (active) Color(0xFF173A56) else Color.Transparent)
                        .clickable { onSelect(index) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(glyphs[index], color = if (active) Cyan else Muted, style = MaterialTheme.typography.titleMedium)
                    Text(label, color = if (active) Cyan else Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

@Composable private fun FeatureMenuCard(
    feature: GameFeature, store: LocalStore, favorite: Boolean, expanded: Boolean,
    onToggleExpand: () -> Unit, onToggleFavorite: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().clickable { onToggleExpand() }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(Color(0xFF193A55), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Text(feature.code, color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(feature.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("${feature.classCount} kelas di indeks", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onToggleFavorite) { Text(if (favorite) "★" else "☆", color = Gold, style = MaterialTheme.typography.titleLarge) }
            }
            if (expanded) {
                Text(feature.description, color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("Ide catatan: ${feature.noteHint}", color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("Kata kunci UI game: ${feature.terms.joinToString(", ")}", color = Muted, style = MaterialTheme.typography.labelSmall)
                var note by remember(feature.id) { mutableStateOf(store.loadFeatureNote(feature.id)) }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; store.saveFeatureNote(feature.id, it) },
                    label = { Text("Catatan kamu") }, modifier = Modifier.fillMaxWidth(), minLines = 3
                )
            }
        }
    }
}

@Composable private fun ModificationCard(mod: BuildModification, onChange: (BuildModification) -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = mod.done, onCheckedChange = { onChange(mod.copy(done = it)) }, colors = CheckboxDefaults.colors(checkedColor = Cyan))
                Column(Modifier.weight(1f)) {
                    Text(mod.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(mod.category, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDelete) { Text("Hapus", color = Gold) }
            }
            OutlinedTextField(value = mod.target, onValueChange = { onChange(mod.copy(target = it.take(60))) },
                label = { Text("Target (nama item / karakter)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = mod.fromLevel.toString(), onValueChange = { onChange(mod.copy(fromLevel = it.filter(Char::isDigit).take(3).toIntOrNull() ?: 0)) },
                    label = { Text("Level awal") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = mod.toLevel.toString(), onValueChange = { onChange(mod.copy(toLevel = it.filter(Char::isDigit).take(3).toIntOrNull() ?: 0)) },
                    label = { Text("Level target") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            OutlinedTextField(value = mod.note, onValueChange = { onChange(mod.copy(note = it)) },
                label = { Text("Catatan / material") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
    }
}

@Composable private fun TemplateCard(template: ModTemplate, onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13283B)), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${template.name}  •  ${template.category}", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(template.description, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onAdd) { Text("+ Tambah", color = Cyan) }
        }
    }
}

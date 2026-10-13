package com.aurcus.companion.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import com.aurcus.companion.data.BuildModification
import com.aurcus.companion.data.DexFileReader
import com.aurcus.companion.data.DexFormatException
import com.aurcus.companion.data.DexKeyIndex
import com.aurcus.companion.data.DexScanSummary
import com.aurcus.companion.data.GameFeature
import com.aurcus.companion.data.GameFeatureCatalog
import com.aurcus.companion.data.LocalStore
import com.aurcus.companion.data.ModTemplate
import com.aurcus.companion.data.ModificationCatalog
import com.aurcus.companion.data.OverlayOptions
import com.aurcus.companion.data.QuestTask
import com.aurcus.companion.data.TabRequest
import com.aurcus.companion.data.Tools
import com.aurcus.companion.data.copyToClipboard
import com.aurcus.companion.data.formatDuration
import com.aurcus.companion.monitor.PerformanceMonitor
import com.aurcus.companion.overlay.FloatingOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF071321)
private val Panel = Color(0xFF10243A)
private val Cyan = Color(0xFF22B8F0)
private val Gold = Color(0xFFE8B84B)
private val Muted = Color(0xFFA8BED1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionScreen(tabRequest: TabRequest? = null, resumeCount: Int = 0) {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    val index = remember { DexKeyIndex.load(context) }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var damageHudScale by remember { mutableStateOf(store.loadScale(OverlayOptions.DAMAGE_SCALE).toFloat()) }
    var attackRangeScale by remember { mutableStateOf(store.loadScale(OverlayOptions.ATTACK_RANGE).toFloat()) }
    var skillAoEScale by remember { mutableStateOf(store.loadScale(OverlayOptions.AOE_RADIUS).toFloat()) }
    var skillEffectScale by remember { mutableStateOf(store.loadScale(OverlayOptions.EFFECT_SCALE).toFloat()) }
    var toggles by remember { mutableStateOf(OverlayOptions.toggles.keys.associateWith { store.loadToggle(it) }) }
    var speedStat by remember { mutableStateOf(store.loadSpeedCurrent().toString()) }
    var speedTarget by remember { mutableStateOf(store.loadSpeedTarget().toString()) }
    var mapNotes by remember { mutableStateOf(store.loadMapNotes()) }
    var mobs by remember { mutableStateOf(store.loadMobs()) }
    var selectedMob by remember { mutableStateOf(store.loadSelectedMob()) }
    var newMob by remember { mutableStateOf("") }
    var spawnNotes by remember { mutableStateOf(store.loadSpawnNotes()) }
    var farmRunning by remember { mutableStateOf(store.farmRunning()) }
    var farmSeconds by remember { mutableIntStateOf(store.farmElapsedSeconds()) }
    var tasks by remember { mutableStateOf(store.loadTasks()) }
    var mods by remember { mutableStateOf(store.loadMods()) }
    var favorites by remember { mutableStateOf(store.loadFavorites()) }
    var featureQuery by remember { mutableStateOf("") }
    var expandedFeature by remember { mutableStateOf<String?>(null) }
    var dexScan by remember { mutableStateOf(store.loadDexScan()) }
    var dexBusy by remember { mutableStateOf(false) }
    var dexError by remember { mutableStateOf<String?>(null) }
    val tabs = Tools.tabLabels
    val glyphs = Tools.tabGlyphs

    // Overlay mengambang berbagi data yang sama; muat ulang saat pindah tab dan saat aplikasi kembali dibuka.
    LaunchedEffect(selectedTab, resumeCount) {
        mods = store.loadMods()
        favorites = store.loadFavorites()
        tasks = store.loadTasks()
        mapNotes = store.loadMapNotes()
        spawnNotes = store.loadSpawnNotes()
        mobs = store.loadMobs()
        selectedMob = store.loadSelectedMob()
        speedStat = store.loadSpeedCurrent().toString()
        speedTarget = store.loadSpeedTarget().toString()
        damageHudScale = store.loadScale(OverlayOptions.DAMAGE_SCALE).toFloat()
        attackRangeScale = store.loadScale(OverlayOptions.ATTACK_RANGE).toFloat()
        skillAoEScale = store.loadScale(OverlayOptions.AOE_RADIUS).toFloat()
        skillEffectScale = store.loadScale(OverlayOptions.EFFECT_SCALE).toFloat()
        toggles = OverlayOptions.toggles.keys.associateWith { store.loadToggle(it) }
        farmRunning = store.farmRunning()
        farmSeconds = store.farmElapsedSeconds()
        dexScan = store.loadDexScan()
    }

    // Overlay boleh meminta tab tertentu (mis. dari detail menu).
    LaunchedEffect(tabRequest) {
        tabRequest?.let { selectedTab = it.tab.coerceIn(0, Tools.tabIds.lastIndex) }
    }

    LaunchedEffect(farmRunning) {
        while (farmRunning) {
            farmSeconds = store.farmElapsedSeconds()
            delay(500)
        }
    }

    val dexPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            dexBusy = true
            dexError = null
            scope.launch {
                val name = displayName(context, uri)
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val scan = context.contentResolver.openInputStream(uri)?.use { DexFileReader.scanStream(it) }
                            ?: throw DexFormatException("Berkas tidak bisa dibuka")
                        DexFileReader.summarize(index, scan, name, System.currentTimeMillis())
                    }
                }
                result.onSuccess { summary ->
                    store.saveDexScan(summary)
                    dexScan = summary
                }.onFailure { error ->
                    dexError = error.message ?: "Gagal membaca berkas"
                }
                dexBusy = false
            }
        }
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
                        Text("COMPANION  •  v1.5.0", style = MaterialTheme.typography.labelSmall, color = Muted)
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
                    item { FeatureCard("☰", "Menu Fitur Game", "${GameFeatureCatalog.features.size} kategori • ${index.keys.size} kunci UI dari dex, bisa dicari, disalin, dan diberi catatan.", Cyan) }
                    item { FeatureCard("▤", "Koneksi DEX", "Verifikasi classes.dex atau APK kamu terhadap indeks menu; hasilnya tampil di Menu dan overlay.", Color(0xFF63E6BE)) }
                    item { FeatureCard("◈", "Daftar Modifikasi Build", "Rencanakan Enhance, Evolve, Remodel, skill, dan crafting; tambah semua sekali ketuk.", Gold) }
                    item { FeatureCard("⚔", "Visual Mod Panel", "Atur skala HUD damage contoh, lingkaran jangkauan, dan pratinjau AoE.", Cyan) }
                    item { FeatureCard("⌖", "Map & Route Notes", "Simpan catatan area, jalur farming, dan titik pengamatan.", Gold) }
                    item { FeatureCard("◎", "Quest • Spawn • Farm", "Checklist, pencatat kemunculan monster, dan timer sesi.", Color(0xFFB69CFF)) }
                    item { InfoCard("Mode companion", "Panel ini tidak menginjeksi proses game, tidak membaca memori game, tidak mengirim input otomatis, dan tidak mengubah nilai di server. Fitur bernama speed/damage hanya monitor atau simulasi lokal. Pembaca dex hanya membaca tabel string, tidak menjalankan kode dex.") }
                }
                1 -> {
                    item { SectionTitle("Menu Fitur Game", "${GameFeatureCatalog.features.size} kategori • ${index.keys.size} kunci UI dari dex • favorit tampil di atas") }
                    item { OutlinedTextField(value = featureQuery, onValueChange = { featureQuery = it }, label = { Text("Cari menu atau kunci dex") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    val query = featureQuery.trim()
                    val visible = GameFeatureCatalog.sorted(favorites).filter {
                        query.isEmpty() || it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true)
                    }
                    val keyHits = if (query.length >= 2) index.search(query, limit = 40) else emptyList()
                    if (visible.isEmpty() && keyHits.isEmpty()) item { InfoCard("Tidak ditemukan", "Tidak ada menu atau kunci dex yang cocok dengan \"$query\".") }
                    items(visible, key = { it.id }) { feature ->
                        FeatureMenuCard(
                            feature = feature,
                            store = store,
                            index = index,
                            scan = dexScan,
                            favorite = feature.id in favorites,
                            expanded = expandedFeature == feature.id,
                            onToggleExpand = { expandedFeature = if (expandedFeature == feature.id) null else feature.id },
                            onToggleFavorite = { favorites = store.toggleFavorite(feature.id) },
                            onOpenTool = { toolId -> selectedTab = Tools.tabIndex(toolId) },
                            onCopyKey = { key ->
                                copyToClipboard(context, "Kunci dex", key)
                                Toast.makeText(context, "Disalin: $key", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    if (keyHits.isNotEmpty()) {
                        item { SectionTitle("Kunci dex yang cocok", "${keyHits.size} hasil (maks 40) • ketuk untuk menyalin") }
                        items(keyHits, key = { "key-$it" }) { key ->
                            KeyRow(key, GameFeatureCatalog.byId(index.classify(key))?.code ?: "GEN") {
                                copyToClipboard(context, "Kunci dex", key)
                                Toast.makeText(context, "Disalin: $key", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    item { InfoCard("Catatan", "Menu berisi referensi dan catatan lokal berdasarkan tabel string dex. Tidak membuka atau mengubah fitur di Aurcus Online.") }
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
                    item { ScalePreviewCard(damageHudScale, attackRangeScale, skillAoEScale) }
                    item { VisualSliderCard("Damage HUD scale", "Ukuran angka damage contoh", damageHudScale, { damageHudScale = it; store.saveScale(OverlayOptions.DAMAGE_SCALE, it.toInt()) }) }
                    item { VisualSliderCard("Attack range preview", "Ukuran lingkaran referensi", attackRangeScale, { attackRangeScale = it; store.saveScale(OverlayOptions.ATTACK_RANGE, it.toInt()) }) }
                    item { VisualSliderCard("Skill AoE radius", "Ukuran pratinjau area skill", skillAoEScale, { skillAoEScale = it; store.saveScale(OverlayOptions.AOE_RADIUS, it.toInt()) }) }
                    item { VisualSliderCard("Skill effect scale", "Ukuran efek visual contoh", skillEffectScale, { skillEffectScale = it; store.saveScale(OverlayOptions.EFFECT_SCALE, it.toInt()) }) }
                    item {
                        OutlinedButton(onClick = {
                            store.resetScales()
                            damageHudScale = 100f; attackRangeScale = 100f; skillAoEScale = 100f; skillEffectScale = 100f
                        }, modifier = Modifier.fillMaxWidth()) { Text("Atur ulang semua skala ke 100%") }
                    }
                    item { SectionTitle("Alat di overlay", "Aktifkan atau sembunyikan halaman di jendela mengambang") }
                    items(OverlayOptions.toggles.keys.toList(), key = { "toggle-$it" }) { name ->
                        ToggleCard(name, OverlayOptions.describe(name), toggles[name] == true) { checked ->
                            store.saveToggle(name, checked)
                            toggles = toggles + (name to checked)
                        }
                    }
                    item { InfoCard("Pratinjau lokal", "Slider dan toggle hanya mengubah tampilan companion dan overlay. Damage aktual, hitbox, jangkauan serangan, dan area skill di Aurcus Online tidak berubah.") }
                }
                4 -> {
                    item { SectionTitle("Map & Route", "Catatan lokasi pribadi") }
                    items(LocalStore.MAP_POINTS, key = { "map-${it.first}" }) { (title, body) ->
                        QuickLogCard(title, body) { mapNotes = store.appendMapLog(title) }
                    }
                    item { OutlinedTextField(value = mapNotes, onValueChange = { mapNotes = it; store.saveMapNotes(it) }, label = { Text("Catatan map pribadi") }, modifier = Modifier.fillMaxWidth(), minLines = 4) }
                    item { OutlinedButton(onClick = { mapNotes = ""; store.saveMapNotes("") }, modifier = Modifier.fillMaxWidth()) { Text("Hapus catatan map") } }
                    item { Text("Catatan tersimpan otomatis dan juga tampil di overlay (halaman MAP).", color = Muted, style = MaterialTheme.typography.bodySmall) }
                }
                5 -> {
                    item { SectionTitle("Spawn Tracker", "Catat observasi respawn secara manual") }
                    item { Text("Pilih target", color = Muted, style = MaterialTheme.typography.labelLarge) }
                    items(mobs, key = { "mob-$it" }) { mob ->
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("◎", color = Gold, style = MaterialTheme.typography.titleLarge)
                                Text(mob, Modifier.weight(1f).padding(start = 10.dp), color = Color.White)
                                RadioButton(selected = selectedMob == mob, onClick = { selectedMob = mob; store.saveSelectedMob(mob) }, colors = RadioButtonDefaults.colors(selectedColor = Cyan))
                                TextButton(onClick = { mobs = store.removeMob(mob); selectedMob = store.loadSelectedMob() }) { Text("Hapus", color = Gold) }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = newMob, onValueChange = { newMob = it.take(40) }, label = { Text("Nama monster baru") }, modifier = Modifier.weight(1f), singleLine = true)
                            Button(onClick = { mobs = store.addMob(newMob); newMob = "" }, enabled = newMob.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) { Text("+ Tambah") }
                        }
                    }
                    item { Button(onClick = { spawnNotes = store.appendSpawnLog(selectedMob) }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) { Text("Catat respawn sekarang: $selectedMob") } }
                    item { OutlinedTextField(value = spawnNotes, onValueChange = { spawnNotes = it; store.saveSpawnNotes(it) }, label = { Text("Log respawn & catatan") }, modifier = Modifier.fillMaxWidth(), minLines = 4) }
                    item { OutlinedButton(onClick = { spawnNotes = ""; store.saveSpawnNotes("") }, modifier = Modifier.fillMaxWidth()) { Text("Hapus log") } }
                    item { InfoCard("Target aktif", selectedMob) }
                }
                6 -> {
                    item { SectionTitle("Farm Session", "Timer dan checklist — tanpa auto-input ke game") }
                    item { ResultCard("DURASI SESI", formatDuration(farmSeconds), if (farmRunning) "Timer sedang berjalan" else "Timer dijeda") }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (farmRunning) store.farmPause() else store.farmStart()
                            farmRunning = store.farmRunning(); farmSeconds = store.farmElapsedSeconds()
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) { Text(if (farmRunning) "Jeda" else "Mulai timer") }
                        OutlinedButton(onClick = { store.farmReset(); farmRunning = false; farmSeconds = 0 }, modifier = Modifier.weight(1f)) { Text("Reset") }
                    } }
                    item { SectionTitle("Checklist", "${tasks.count { it.completed }}/${tasks.size} selesai • sama dengan halaman QUEST di overlay") }
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
                    item { OutlinedButton(onClick = { tasks = tasks.filterNot { it.completed }; store.saveTasks(tasks) }, modifier = Modifier.fillMaxWidth()) { Text("Hapus yang selesai") } }
                }
                7 -> {
                    item { SectionTitle("Speed Monitor", "Bandingkan nilai manual dan statistik perangkat") }
                    item { InfoCard("Monitor aman", "Tidak mengubah kecepatan karakter. Masukkan statistik yang kamu lihat sendiri untuk membandingkan target.") }
                    item { NumberField("Speed saat ini", speedStat) { speedStat = it; store.saveSpeedCurrent(it.toIntOrNull() ?: 0) } }
                    item { NumberField("Target / benchmark", speedTarget) { speedTarget = it; store.saveSpeedTarget(it.toIntOrNull() ?: 0) } }
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
                else -> {
                    val meta = index.meta
                    item { SectionTitle("Koneksi DEX", "Menu dibangun dari tabel string classes.dex klien Aurcus Online") }
                    item { ResultCard("KUNCI UI DARI DEX", "${index.keys.size}", "kelas ${meta.sourceClass} • ${meta.gamePackage}") }
                    item {
                        InfoCard("Sumber indeks bawaan", "Berkas: ${meta.file} (dex ${meta.dexVersion})\nUkuran: ${"%.1f".format(meta.sizeBytes / 1048576.0)} MB • ${meta.classCount} kelas\nSHA-256: ${meta.sha256.take(24)}…")
                    }
                    item { SectionTitle("Verifikasi dex kamu", "Pilih classes.dex atau APK untuk dicocokkan dengan indeks menu") }
                    item {
                        Button(onClick = { dexPicker.launch(arrayOf("*/*")) }, enabled = !dexBusy, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) {
                            Text(if (dexBusy) "Memindai…" else "Pilih classes.dex / APK", fontWeight = FontWeight.Bold)
                        }
                    }
                    dexError?.let { message -> item { InfoCard("Gagal membaca berkas", message) } }
                    dexScan?.let { scan ->
                        item { ResultCard("KUNCI COCOK", "${scan.bundledFound}/${scan.bundledTotal}", "${scan.fileName} • dex ${scan.dexVersion} • ${scan.dexFileCount} berkas • ${scan.stringCount} string") }
                        item {
                            InfoCard(
                                "Kandidat kunci baru: ${scan.candidateCount}",
                                if (scan.candidateSamples.isEmpty()) "Tidak ada string baru yang kategorinya dikenal."
                                else scan.candidateSamples.joinToString(", ") + (if (scan.candidateCount > scan.candidateSamples.size) ", …" else "") +
                                    "\nSebagian kandidat bisa berasal dari library, periksa manual."
                            )
                        }
                    }
                    item { SectionTitle("Jumlah kunci per kategori", "Dihitung dari data dex, bukan angka tulisan tangan") }
                    items(GameFeatureCatalog.features, key = { "dexf-${it.id}" }) { feature ->
                        val total = index.countOf(feature.id)
                        val found = dexScan?.foundByFeature?.get(feature.id)
                        CategoryRow(feature.code, feature.title, if (found == null) "$total kunci" else "$total kunci • cocok $found/$total di ${dexScan?.fileName}")
                    }
                    item { InfoCard("Catatan", "Pemindai hanya membaca tabel string dex secara lokal. Tidak memuat atau menjalankan kode dex, dan tidak terhubung ke game.") }
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull() ?: uri.lastPathSegment ?: "dex"

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
        listOf("TOOLS" to "09", "MODE" to "LOCAL", "ANDROID" to "9+").forEach { (label, value) ->
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

@Composable private fun CategoryRow(code: String, title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(code, color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable private fun KeyRow(key: String, code: String, onCopy: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(12.dp), modifier = Modifier.clickable { onCopy() }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(code, color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
            Text(key, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun ToggleCard(name: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Cyan))
        }
    }
}

@Composable private fun QuickLogCard(title: String, body: String, onLog: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 15.dp, top = 8.dp, bottom = 8.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(body, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onLog) { Text("+ Catat", color = Cyan) }
        }
    }
}

@Composable private fun ScalePreviewCard(damage: Float, range: Float, aoe: Float) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("PRATINJAU", color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("1,245   CRIT ×1.8", color = Gold, fontSize = (16f * damage / 100f).sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size((36f * range / 100f).dp).background(Gold.copy(alpha = 0.28f), CircleShape))
                Box(Modifier.size((36f * aoe / 100f).dp).background(Cyan.copy(alpha = 0.32f), CircleShape))
            }
            Text("Kuning: jangkauan • Biru: area skill (hanya contoh visual)", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun FeatureMenuCard(
    feature: GameFeature, store: LocalStore, index: DexKeyIndex, scan: DexScanSummary?,
    favorite: Boolean, expanded: Boolean,
    onToggleExpand: () -> Unit, onToggleFavorite: () -> Unit,
    onOpenTool: (String) -> Unit, onCopyKey: (String) -> Unit
) {
    val keys = index.keysOf(feature.id)
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().clickable { onToggleExpand() }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(Color(0xFF193A55), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Text(feature.code, color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(feature.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                    val verified = scan?.let { " • cocok ${it.foundByFeature[feature.id] ?: 0}/${keys.size}" } ?: ""
                    Text("${keys.size} kunci dari dex$verified", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onToggleFavorite) { Text(if (favorite) "★" else "☆", color = Gold, style = MaterialTheme.typography.titleLarge) }
            }
            if (expanded) {
                Text(feature.description, color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("Ide catatan: ${feature.noteHint}", color = Muted, style = MaterialTheme.typography.bodySmall)
                feature.tool?.let { toolId ->
                    OutlinedButton(onClick = { onOpenTool(toolId) }, modifier = Modifier.fillMaxWidth()) { Text("Buka alat ${Tools.tabLabel(toolId)}") }
                }
                var note by remember(feature.id) { mutableStateOf(store.loadFeatureNote(feature.id)) }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; store.saveFeatureNote(feature.id, it) },
                    label = { Text("Catatan kamu") }, modifier = Modifier.fillMaxWidth(), minLines = 3
                )
                var shown by remember(feature.id) { mutableIntStateOf(20) }
                Text("Kunci UI dari dex (ketuk untuk menyalin)", color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                keys.take(shown).forEach { key ->
                    Text(key, color = Color.White, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0C1D2F)).clickable { onCopyKey(key) }.padding(horizontal = 10.dp, vertical = 7.dp))
                }
                if (shown < keys.size) {
                    TextButton(onClick = { shown += 30 }) { Text("Tampilkan lebih banyak (${minOf(shown, keys.size)}/${keys.size})", color = Cyan) }
                }
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

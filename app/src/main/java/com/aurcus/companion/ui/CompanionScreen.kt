package com.aurcus.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aurcus.companion.data.*
import com.aurcus.companion.domain.BuildPlanner
import com.aurcus.companion.domain.EquipmentComparator
import com.aurcus.companion.monitor.PerformanceMonitor

private val sampleItems = listOf(
    Item("demo-001", "Sample Blade", "Weapon", attack = 24, notes = "Data demo"),
    Item("demo-002", "Sample Robe", "Body", defense = 12, magic = 8, notes = "Data demo"),
    Item("demo-003", "Sample Charm", "Accessory", attack = 3, magic = 10, notes = "Data demo")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionScreen() {
    val context = LocalContext.current
    val store = remember { LocalStore(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var strength by remember { mutableStateOf("10") }
    var vitality by remember { mutableStateOf("10") }
    var dexterity by remember { mutableStateOf("10") }
    var intelligence by remember { mutableStateOf("10") }
    var query by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(store.loadTasks()) }
    val tabs = listOf("Home", "Build", "Items", "Tracker", "Compare", "Monitor")

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("AURCUS COMPANION")
                    Text("Android 9+ • Local companion • v1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
            })
        },
        bottomBar = {
            NavigationBar {
                val icons = listOf(
                    Icons.Default.Dashboard, Icons.Default.Tune, Icons.Default.Inventory2,
                    Icons.Default.CheckCircle, Icons.Default.CompareArrows, Icons.Default.Memory
                )
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item { Text("Companion dashboard", style = MaterialTheme.typography.headlineSmall) }
                    item { InfoCard("Build planner", "Simulasikan distribusi atribut secara lokal.") }
                    item { InfoCard("Item database", "Item yang tersedia adalah contoh fiktif, bukan database resmi.") }
                    item { InfoCard("Server safety", "Tidak ada pembacaan memori game, packet capture, atau perintah ke server.") }
                }
                1 -> {
                    item { Text("Build planner", style = MaterialTheme.typography.headlineSmall) }
                    item { NumberField("Strength", strength) { strength = it } }
                    item { NumberField("Vitality", vitality) { vitality = it } }
                    item { NumberField("Dexterity", dexterity) { dexterity = it } }
                    item { NumberField("Intelligence", intelligence) { intelligence = it } }
                    item {
                        val result = BuildPlanner.analyze(
                            BuildPreset("Current", strength.toIntOrNull() ?: 0,
                                vitality.toIntOrNull() ?: 0, dexterity.toIntOrNull() ?: 0,
                                intelligence.toIntOrNull() ?: 0)
                        )
                        InfoCard("Summary", "Total: ${result.totalPoints}\nPhysical indicator: ${result.physicalFocusPercent}%\nMagic indicator: ${result.magicFocusPercent}%")
                    }
                }
                2 -> {
                    item { Text("Item database", style = MaterialTheme.typography.headlineSmall) }
                    item {
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            label = { Text("Search item") }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    items(sampleItems.filter { it.name.contains(query, ignoreCase = true) }) { item ->
                        InfoCard(item.name, "Slot: ${item.slot}\nATK ${item.attack} • DEF ${item.defense} • MAG ${item.magic}\n${item.notes}")
                    }
                }
                3 -> {
                    item { Text("Quest & farming tracker", style = MaterialTheme.typography.headlineSmall) }
                    items(tasks, key = { it.id }) { task ->
                        Card {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                                    Text("${task.category} • ${task.currentCount}/${task.targetCount}")
                                }
                                Checkbox(checked = task.completed, onCheckedChange = { checked ->
                                    tasks = tasks.map { if (it.id == task.id) it.copy(completed = checked) else it }
                                    store.saveTasks(tasks)
                                })
                            }
                        }
                    }
                    item {
                        Button(onClick = {
                            tasks = tasks + QuestTask("task-${System.currentTimeMillis()}",
                                "New manual task", "Farming")
                            store.saveTasks(tasks)
                        }) { Text("Add task") }
                    }
                }
                4 -> {
                    item { Text("Equipment comparison", style = MaterialTheme.typography.headlineSmall) }
                    item {
                        val delta = EquipmentComparator.compare(sampleItems[0], sampleItems[2])
                        InfoCard("Demo comparison", "ATK change: ${delta.attackDelta}\nDEF change: ${delta.defenseDelta}\nMAG change: ${delta.magicDelta}\nCompare items in the same slot; these are fictional sample values.")
                    }
                }
                else -> {
                    item { Text("Local performance", style = MaterialTheme.typography.headlineSmall) }
                    item {
                        val snapshot = remember { PerformanceMonitor.snapshot(context) }
                        InfoCard("Companion process", "App memory (PSS): ${snapshot.companionPssKb} KB\nAvailable device memory: ${snapshot.availableMemoryMb} MB\nLow-memory signal: ${snapshot.lowMemory}\n\nThis does not measure Aurcus FPS or inspect the game process.")
                    }
                    item { InfoCard("Compatibility", "minSdk 28 (Android 9). compileSdk 35 is only the build API level. No game/server integration is included.") }
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
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(6)) },
        label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true
    )
}

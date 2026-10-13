package com.aurcus.companion.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.aurcus.companion.MainActivity
import com.aurcus.companion.data.DexKeyIndex
import com.aurcus.companion.data.GameFeature
import com.aurcus.companion.data.GameFeatureCatalog
import com.aurcus.companion.data.LocalStore
import com.aurcus.companion.data.ModificationCatalog
import com.aurcus.companion.data.OverlayOptions
import com.aurcus.companion.data.QuestTask
import com.aurcus.companion.data.Tools
import com.aurcus.companion.data.copyToClipboard
import com.aurcus.companion.data.formatDuration

/**
 * Aurcus Companion local overlay. Semua halaman memakai data lokal yang sama dengan aplikasi utama
 * (catatan, checklist, timer, indeks kunci dex). Sliders and switches control companion UI previews only.
 * This service does not inject into the game, alter game memory, automate gameplay, or contact game servers.
 */
class FloatingOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var overlayRoot: LinearLayout? = null
    private var content: LinearLayout? = null
    private var titleView: TextView? = null
    private val navButtons = linkedMapOf<String, Button>()
    private var farmLabel: TextView? = null
    private var farmButton: Button? = null
    private var expanded = true
    private var selectedPage = "MOD"
    private var selectedFeatureId: String? = null
    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            refreshLive()
            handler.postDelayed(this, 1000L)
        }
    }
    private val store by lazy { LocalStore(this) }
    private val index by lazy { DexKeyIndex.load(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Aurcus floating companion", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        @Suppress("DEPRECATION")
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID)
        else Notification.Builder(this)
        return builder.setContentTitle("Aurcus Companion aktif")
            .setContentText("Pratinjau visual lokal sedang berjalan")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun createOverlay() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(9))
            background = rounded(Color.rgb(7, 19, 33), dp(18), CYAN, dp(1))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brandIcon = ImageView(this).apply {
            setImageResource(com.aurcus.companion.R.drawable.aurcus_floating_icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Aurcus Companion"
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
        val title = TextView(this).apply {
            text = "AURCUS COMPANION\nVISUAL TOOLKIT • READY"
            setTextColor(Color.WHITE); textSize = 11f; typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(5), dp(5), dp(3), dp(5))
        }
        titleView = title
        val fold = smallButton(if (expanded) "Lipat" else "Buka")
        val close = smallButton("×")
        header.addView(brandIcon, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(title, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(fold, LinearLayout.LayoutParams(dp(60), dp(36)))
        header.addView(close, LinearLayout.LayoutParams(dp(36), dp(36)))
        root.addView(header)

        // Navigasi bisa digeser horizontal; halaman yang toggle-nya mati disembunyikan (lihat applyGates).
        val navRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        PAGES.forEach { page ->
            val button = smallButton(page).apply { textSize = 8.5f }
            navButtons[page] = button
            navRow.addView(button, LinearLayout.LayoutParams(dp(48), dp(34)).apply { setMargins(dp(1), dp(3), dp(1), dp(3)) })
            button.setOnClickListener { selectPage(page) }
        }
        val nav = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(navRow) }
        root.addView(nav)

        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(2), dp(4), dp(2), dp(2)) }
        content = body
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(dp(302), dp(350)))

        fold.setOnClickListener {
            expanded = !expanded
            scroll.visibility = if (expanded) View.VISIBLE else View.GONE
            nav.visibility = if (expanded) View.VISIBLE else View.GONE
            fold.text = if (expanded) "Lipat" else "Buka"
            updateLayoutSize(root)
        }
        close.setOnClickListener { stopSelf() }
        title.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; touchX = event.rawX; touchY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    val metrics = resources.displayMetrics
                    params.x = (initialX + (event.rawX - touchX).toInt()).coerceIn(0, (metrics.widthPixels - dp(90)).coerceAtLeast(0))
                    params.y = (initialY + (event.rawY - touchY).toInt()).coerceIn(0, (metrics.heightPixels - dp(100)).coerceAtLeast(0))
                    updateLayoutSize(root); true
                }
                else -> false
            }
        }

        params = WindowManager.LayoutParams(
            dp(322), WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(10); y = dp(85) }
        overlayRoot = root
        windowManager.addView(root, params)
        renderPage()
        handler.post(tick)
    }

    // ---- Navigasi & gating ----

    private fun isOn(toggle: String): Boolean = store.loadToggle(toggle)

    private fun isPageEnabled(page: String): Boolean = GATES[page]?.let { isOn(it) } ?: true

    private fun applyGates() {
        navButtons.forEach { (page, button) -> button.visibility = if (isPageEnabled(page)) View.VISIBLE else View.GONE }
        if (!isPageEnabled(selectedPage)) { selectedPage = "MOD"; selectedFeatureId = null }
        navButtons.forEach { (page, button) ->
            val active = page == selectedPage
            button.background = rounded(if (active) Color.rgb(24, 92, 138) else Color.rgb(18, 57, 86), dp(9), if (active) GOLD else CYAN, dp(1))
        }
    }

    private fun selectPage(page: String) {
        selectedPage = page
        selectedFeatureId = null
        renderPage()
    }

    /** Buka alat: pindah halaman overlay bila ada dan aktif, jika tidak buka tab di aplikasi utama. */
    private fun openTool(toolId: String) {
        val page = Tools.overlayPage(toolId)
        if (page != null && isPageEnabled(page)) selectPage(page) else openMainApp(toolId)
    }

    private fun openMainApp(toolId: String) {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(Tools.EXTRA_TAB, Tools.tabIndex(toolId))
        try { startActivity(intent) } catch (_: Exception) { toast("Tidak bisa membuka aplikasi utama") }
    }

    private fun refreshLive() {
        val running = store.farmRunning()
        val seconds = store.farmElapsedSeconds()
        val sub = if (isOn(OverlayOptions.T_FARM)) "FARM ${formatDuration(seconds)}${if (running) "" else " • jeda"}" else "VISUAL TOOLKIT • READY"
        val text = "AURCUS COMPANION\n$sub"
        titleView?.let { if (it.text.toString() != text) it.text = text }
        farmLabel?.text = formatDuration(seconds)
        farmButton?.text = if (running) "Jeda" else "Mulai"
    }

    private fun renderPage() {
        val body = content ?: return
        body.removeAllViews()
        farmLabel = null
        farmButton = null
        applyGates()
        when (selectedPage) {
            "MOD" -> renderModPage(body)
            "MENU" -> renderMenuPage(body)
            "BUILD" -> renderBuildPage(body)
            "DMG" -> renderDamagePage(body)
            "RANGE" -> renderRangePage(body)
            "AOE" -> renderAoEPage(body)
            "QUEST" -> renderQuestPage(body)
            "MAP" -> renderMapPage(body)
            "SPAWN" -> renderSpawnPage(body)
            "FARM" -> renderFarmPage(body)
            "SPEED" -> renderSpeedPage(body)
            else -> renderDexPage(body)
        }
        body.addView(label("LOCAL PREVIEW ONLY • DOES NOT CHANGE GAME/SERVER", 8.5f, MUTED))
        refreshLive()
    }

    // ---- Halaman ----

    private fun renderModPage(body: LinearLayout) {
        body.addView(label("MODIFICATION PANEL", 15f, CYAN, true))
        body.addView(label("Toggle menampilkan atau menyembunyikan halaman alatnya di navigasi.", 10.5f, MUTED))
        OverlayOptions.toggles.keys.forEach { name ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(5), dp(6), dp(5))
                background = rounded(PANEL, dp(10))
            }
            val check = CheckBox(this).apply {
                text = name; isChecked = isOn(name); setTextColor(Color.WHITE); textSize = 11.5f
                buttonTintList = ColorStateList.valueOf(CYAN)
            }
            row.addView(check)
            row.addView(label(OverlayOptions.describe(name), 9.5f, MUTED))
            check.setOnCheckedChangeListener { _, checked ->
                store.saveToggle(name, checked)
                applyGates()
                refreshLive()
            }
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        val reset = smallButton("Atur ulang semua skala ke 100%")
        reset.setOnClickListener { store.resetScales(); toast("Skala pratinjau dikembalikan ke 100%") }
        body.addView(reset, LinearLayout.LayoutParams(-1, dp(36)).apply { setMargins(0, dp(6), 0, 0) })
        body.addView(infoCard("Cara pakai", "DMG, RANGE, dan AOE mengubah tampilan pratinjau di companion, bukan damage atau hitbox Aurcus Online."))
    }

    private fun renderMenuPage(body: LinearLayout) {
        val feature = selectedFeatureId?.let { GameFeatureCatalog.byId(it) }
        if (feature != null) { renderFeatureDetail(body, feature); return }
        val scan = store.loadDexScan()
        body.addView(label("MENU FITUR GAME", 15f, CYAN, true))
        body.addView(label("${GameFeatureCatalog.features.size} kategori • ${index.keys.size} kunci UI dari dex. Ketuk untuk detail, ★ untuk favorit.", 10.5f, MUTED))
        val favorites = store.loadFavorites()
        GameFeatureCatalog.sorted(favorites).forEach { entry ->
            val count = index.countOf(entry.id)
            val verified = scan?.let { " • cocok ${it.foundByFeature[entry.id] ?: 0}/$count" } ?: ""
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(4), dp(4), dp(4))
                background = rounded(PANEL, dp(10))
            }
            val badge = label(entry.code, 9f, GOLD, true)
            val name = label("${entry.title}\n$count kunci DEX$verified", 10.5f, Color.WHITE)
            val star = smallButton(if (entry.id in favorites) "★" else "☆")
            row.addView(badge, LinearLayout.LayoutParams(dp(38), -2))
            row.addView(name, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(star, LinearLayout.LayoutParams(dp(34), dp(34)))
            row.setOnClickListener { selectedFeatureId = entry.id; renderPage() }
            star.setOnClickListener { store.toggleFavorite(entry.id); renderPage() }
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        val more = smallButton("Cari kunci & edit catatan di aplikasi utama")
        more.setOnClickListener { openMainApp("menu") }
        body.addView(more, LinearLayout.LayoutParams(-1, dp(36)).apply { setMargins(0, dp(6), 0, 0) })
        body.addView(infoCard("Catatan", "Menu ini hanya daftar referensi dan catatan lokal. Tidak membuka atau mengubah fitur di Aurcus Online."))
    }

    private fun renderFeatureDetail(body: LinearLayout, feature: GameFeature) {
        val back = smallButton("← Kembali ke menu")
        back.setOnClickListener { selectedFeatureId = null; renderPage() }
        body.addView(back, LinearLayout.LayoutParams(-1, dp(34)))
        body.addView(label("${feature.code} • ${feature.title.uppercase()}", 13f, CYAN, true))
        body.addView(label(feature.description, 10.5f, Color.WHITE))
        body.addView(infoCard("Ide catatan", feature.noteHint))

        val keys = index.keysOf(feature.id)
        val scan = store.loadDexScan()
        val verified = scan?.let { "\nCocok dengan ${it.fileName}: ${it.foundByFeature[feature.id] ?: 0}/${keys.size}" } ?: "\nBelum ada dex yang diverifikasi (tab Dex di aplikasi utama)."
        body.addView(infoCard("Kunci UI dari dex: ${keys.size}", "Sumber: kelas ${index.meta.sourceClass} • ${index.meta.gamePackage}$verified"))

        feature.tool?.let { toolId ->
            val open = smallButton("Buka alat ${Tools.tabLabel(toolId)}")
            open.setOnClickListener { openTool(toolId) }
            body.addView(open, LinearLayout.LayoutParams(-1, dp(36)).apply { setMargins(0, dp(4), 0, dp(2)) })
        }

        body.addView(label("Ketuk kunci untuk menyalin", 10f, MUTED))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(list)
        var shown = 0
        val moreButton = smallButton("")
        fun showMore() {
            val next = (shown + KEY_PAGE).coerceAtMost(keys.size)
            for (i in shown until next) list.addView(keyRow(keys[i]), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(2), 0, dp(2)) })
            shown = next
            if (shown >= keys.size) moreButton.visibility = View.GONE
            else moreButton.text = "Tampilkan ${minOf(KEY_PAGE, keys.size - shown)} lagi ($shown/${keys.size})"
        }
        moreButton.setOnClickListener { showMore() }
        body.addView(moreButton, LinearLayout.LayoutParams(-1, dp(34)).apply { setMargins(0, dp(4), 0, dp(4)) })
        if (keys.isEmpty()) moreButton.visibility = View.GONE else showMore()

        val note = store.loadFeatureNote(feature.id)
        body.addView(infoCard("Catatan kamu", if (note.isBlank()) "(kosong) — tulis di aplikasi utama, tab Menu." else note))
        val edit = smallButton("Edit catatan di aplikasi utama")
        edit.setOnClickListener { openMainApp("menu") }
        body.addView(edit, LinearLayout.LayoutParams(-1, dp(34)).apply { setMargins(0, dp(2), 0, dp(4)) })
    }

    private fun keyRow(key: String): View = label(key, 9.5f, Color.WHITE).apply {
        background = rounded(PANEL, dp(8))
        setOnClickListener {
            copyToClipboard(this@FloatingOverlayService, "Kunci dex", key)
            toast("Disalin: $key")
        }
    }

    private fun renderBuildPage(body: LinearLayout) {
        body.addView(label("MODIFIKASI BUILD", 15f, CYAN, true))
        body.addView(label("Rencana modifikasi equipment & karakter. Edit detail di aplikasi utama, tab Build.", 10.5f, MUTED))
        val mods = store.loadMods().toMutableList()
        val progress = label("", 11f, GOLD, true)
        fun refreshProgress() { progress.text = "Progres: ${mods.count { it.done }}/${mods.size} selesai" }
        refreshProgress()
        body.addView(progress)
        if (mods.isEmpty()) body.addView(infoCard("Daftar kosong", "Ketuk \"Tambah semua modifikasi\" untuk memasukkan seluruh jenis modifikasi."))
        mods.forEachIndexed { index, mod ->
            val detail = buildList {
                add(mod.category)
                if (mod.target.isNotBlank()) add(mod.target)
                add("Lv ${mod.fromLevel} → ${mod.toLevel}")
            }.joinToString(" • ")
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(4), dp(6), dp(4))
                background = rounded(PANEL, dp(10))
            }
            val check = CheckBox(this).apply {
                text = mod.name; isChecked = mod.done; setTextColor(Color.WHITE); textSize = 11f
                buttonTintList = ColorStateList.valueOf(CYAN)
            }
            check.setOnCheckedChangeListener { _, checked ->
                mods[index] = mods[index].copy(done = checked)
                store.saveMods(mods)
                refreshProgress()
            }
            row.addView(check)
            row.addView(label(detail, 9.5f, MUTED))
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        val addAll = smallButton("Tambah semua modifikasi")
        addAll.setOnClickListener { store.saveMods(ModificationCatalog.addAll(store.loadMods())); renderPage() }
        body.addView(addAll, LinearLayout.LayoutParams(-1, dp(38)).apply { setMargins(0, dp(8), 0, 0) })
        val clearDone = smallButton("Hapus yang selesai")
        clearDone.setOnClickListener { store.saveMods(store.loadMods().filterNot { it.done }); renderPage() }
        body.addView(clearDone, LinearLayout.LayoutParams(-1, dp(38)).apply { setMargins(0, dp(4), 0, 0) })
        body.addView(infoCard("Catatan", "Hanya daftar rencana pribadi. Tidak melakukan enhance/remodel/evolve dan tidak mengubah item di game."))
    }

    private fun renderDamagePage(body: LinearLayout) {
        body.addView(label("DAMAGE HUD", 15f, CYAN, true))
        body.addView(label("Ubah skala angka contoh dan lama indikator tampil.", 10.5f, MUTED))
        val preview = TextView(this).apply {
            text = "1,245   CRIT  ×1.8"
            setTextColor(GOLD); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            textSize = BASE_DAMAGE_SP * store.loadScale(OverlayOptions.DAMAGE_SCALE) / 100f
            background = rounded(PANEL, dp(10))
        }
        addSlider(body, "Skala angka damage", OverlayOptions.DAMAGE_SCALE, 50, 300, 100, "%") { value ->
            preview.textSize = BASE_DAMAGE_SP * value / 100f
        }
        addSlider(body, "Durasi indikator", OverlayOptions.DAMAGE_DURATION, 1, 5, 2, " dtk")
        body.addView(preview, LinearLayout.LayoutParams(-1, dp(86)).apply { setMargins(0, dp(7), 0, dp(4)) })
        val play = smallButton("Putar contoh (muncul lalu memudar)")
        play.setOnClickListener {
            val seconds = store.loadScale(OverlayOptions.DAMAGE_DURATION, 2)
            preview.animate().cancel()
            preview.alpha = 1f
            preview.animate().alpha(0f).setStartDelay(seconds * 1000L).setDuration(600L).start()
        }
        body.addView(play, LinearLayout.LayoutParams(-1, dp(36)))
        body.addView(infoCard("Pratinjau angka", "Angka contoh untuk desain HUD; tidak berasal dari game."))
    }

    private fun renderRangePage(body: LinearLayout) {
        body.addView(label("ATTACK RANGE", 15f, CYAN, true))
        body.addView(label("Sesuaikan lingkaran referensi di panel companion.", 10.5f, MUTED))
        val preview = RingPreviewView(false, store.loadScale(OverlayOptions.ATTACK_RANGE), 100)
        addSlider(body, "Radius visual", OverlayOptions.ATTACK_RANGE, 50, 300, 100, "%") { preview.setScale(it) }
        body.addView(preview.apply { layoutParams = LinearLayout.LayoutParams(-1, dp(150)).apply { setMargins(0, dp(7), 0, dp(7)) } })
        body.addView(infoCard("Catatan", "Lingkaran ini hanya panduan visual di companion. Tidak mengubah jangkauan serangan atau hit detection game."))
    }

    private fun renderAoEPage(body: LinearLayout) {
        body.addView(label("SKILL AREA / AoE", 15f, CYAN, true))
        body.addView(label("Atur pratinjau radius dan ukuran efek visual.", 10.5f, MUTED))
        val preview = RingPreviewView(true, store.loadScale(OverlayOptions.AOE_RADIUS), store.loadScale(OverlayOptions.EFFECT_SCALE))
        addSlider(body, "Radius area visual", OverlayOptions.AOE_RADIUS, 50, 300, 100, "%") { preview.setScale(it) }
        if (isOn(OverlayOptions.T_EFFECT)) {
            addSlider(body, "Skala efek visual", OverlayOptions.EFFECT_SCALE, 50, 300, 100, "%") { preview.setEffect(it) }
        }
        body.addView(preview.apply { layoutParams = LinearLayout.LayoutParams(-1, dp(155)).apply { setMargins(0, dp(7), 0, dp(7)) } })
        body.addView(infoCard("Pratinjau", "Bentuk dan skala hanya ditampilkan pada panel companion, bukan area efek skill dalam game."))
    }

    private fun renderQuestPage(body: LinearLayout) {
        body.addView(label("QUEST & FARM", 15f, CYAN, true))
        body.addView(label("Checklist lokal; sama dengan tab Farm di aplikasi utama.", 10.5f, MUTED))
        val tasks = store.loadTasks().toMutableList()
        val progress = label("", 11f, GOLD, true)
        fun refreshProgress() { progress.text = "Progres: ${tasks.count { it.completed }}/${tasks.size} selesai" }
        refreshProgress()
        body.addView(progress)
        tasks.forEachIndexed { i, task ->
            val check = CheckBox(this).apply {
                text = "${task.title}\n${task.category} • ${task.currentCount}/${task.targetCount}"
                setTextColor(Color.WHITE); textSize = 11f
                buttonTintList = ColorStateList.valueOf(CYAN)
                isChecked = task.completed
            }
            check.setOnCheckedChangeListener { _, checked ->
                tasks[i] = tasks[i].copy(completed = checked)
                store.saveTasks(tasks)
                refreshProgress()
            }
            body.addView(check)
        }
        val add = smallButton("+ Tambah tugas")
        add.setOnClickListener {
            store.saveTasks(store.loadTasks() + QuestTask("task-${System.currentTimeMillis()}", "Tugas farming baru", "Farming"))
            renderPage()
        }
        body.addView(add, LinearLayout.LayoutParams(-1, dp(36)).apply { setMargins(0, dp(6), 0, 0) })
        val clear = smallButton("Hapus yang selesai")
        clear.setOnClickListener { store.saveTasks(store.loadTasks().filterNot { it.completed }); renderPage() }
        body.addView(clear, LinearLayout.LayoutParams(-1, dp(36)).apply { setMargins(0, dp(4), 0, 0) })
        body.addView(infoCard("Farm session", "Timer sesi ada di halaman FARM (aktifkan toggle Farm session timer). Tidak ada input otomatis ke game."))
    }

    private fun renderMapPage(body: LinearLayout) {
        body.addView(label("MAP & ROUTE", 15f, CYAN, true))
        body.addView(label("Catat titik dengan stempel waktu. Teks bebas diedit di aplikasi utama.", 10.5f, MUTED))
        val notesView = label("", 10f, Color.WHITE).apply { background = rounded(PANEL, dp(10)) }
        fun refreshNotes() { notesView.text = lastLines(store.loadMapNotes(), 12).ifBlank { "(belum ada catatan)" } }
        refreshNotes()
        body.addView(notesView, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(4)) })
        LocalStore.MAP_POINTS.forEach { (title, _) ->
            val button = smallButton("+ Catat: $title")
            button.setOnClickListener { store.appendMapLog(title); refreshNotes() }
            body.addView(button, LinearLayout.LayoutParams(-1, dp(34)).apply { setMargins(0, dp(2), 0, dp(2)) })
        }
        val clear = smallButton("Hapus semua catatan map")
        clear.setOnClickListener { store.saveMapNotes(""); refreshNotes() }
        body.addView(clear, LinearLayout.LayoutParams(-1, dp(34)).apply { setMargins(0, dp(4), 0, 0) })
        val open = smallButton("Edit di aplikasi utama")
        open.setOnClickListener { openMainApp("map") }
        body.addView(open, LinearLayout.LayoutParams(-1, dp(34)).apply { setMargins(0, dp(2), 0, 0) })
    }

    private fun renderSpawnPage(body: LinearLayout) {
        body.addView(label("SPAWN TRACKER", 15f, CYAN, true))
        body.addView(label("Pilih target lalu catat respawn yang kamu amati secara manual.", 10.5f, MUTED))
        val selected = store.loadSelectedMob()
        store.loadMobs().forEach { mob ->
            val button = smallButton(if (mob == selected) "◉ $mob" else "○ $mob")
            button.setOnClickListener { store.saveSelectedMob(mob); renderPage() }
            body.addView(button, LinearLayout.LayoutParams(-1, dp(34)).apply { setMargins(0, dp(2), 0, dp(2)) })
        }
        val logView = label("", 10f, Color.WHITE).apply { background = rounded(PANEL, dp(10)) }
        fun refreshLog() { logView.text = lastLines(store.loadSpawnNotes(), 8).ifBlank { "(belum ada catatan)" } }
        refreshLog()
        val record = smallButton("Catat respawn sekarang: $selected")
        record.setOnClickListener { store.appendSpawnLog(selected); refreshLog() }
        body.addView(record, LinearLayout.LayoutParams(-1, dp(38)).apply { setMargins(0, dp(6), 0, dp(4)) })
        body.addView(logView, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(2), 0, dp(4)) })
        val clear = smallButton("Hapus log")
        clear.setOnClickListener { store.saveSpawnNotes(""); refreshLog() }
        body.addView(clear, LinearLayout.LayoutParams(-1, dp(34)))
        body.addView(infoCard("Catatan", "Tambah atau hapus nama monster di aplikasi utama, tab Spawn."))
    }

    private fun renderFarmPage(body: LinearLayout) {
        body.addView(label("FARM SESSION", 15f, CYAN, true))
        body.addView(label("Timer sesi bersama dengan aplikasi utama. Tanpa auto-input ke game.", 10.5f, MUTED))
        val timer = label(formatDuration(store.farmElapsedSeconds()), 30f, Color.rgb(109, 227, 179), true).apply {
            gravity = Gravity.CENTER; background = rounded(PANEL, dp(12))
        }
        farmLabel = timer
        body.addView(timer, LinearLayout.LayoutParams(-1, dp(80)).apply { setMargins(0, dp(6), 0, dp(6)) })
        val toggle = smallButton(if (store.farmRunning()) "Jeda" else "Mulai")
        farmButton = toggle
        toggle.setOnClickListener {
            if (store.farmRunning()) store.farmPause() else store.farmStart()
            refreshLive()
        }
        val reset = smallButton("Reset")
        reset.setOnClickListener { store.farmReset(); refreshLive() }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(toggle, LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(0, 0, dp(3), 0) })
        row.addView(reset, LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(dp(3), 0, 0, 0) })
        body.addView(row)
        val tasks = store.loadTasks()
        body.addView(infoCard("Checklist", "${tasks.count { it.completed }}/${tasks.size} tugas selesai (kelola di halaman QUEST)."))
    }

    private fun renderSpeedPage(body: LinearLayout) {
        body.addView(label("SPEED MONITOR", 15f, CYAN, true))
        body.addView(label("Bandingkan angka yang kamu lihat sendiri. Tidak mengubah kecepatan karakter.", 10.5f, MUTED))
        val result = label("", 12f, Color.rgb(109, 227, 179), true).apply { background = rounded(PANEL, dp(10)); gravity = Gravity.CENTER }
        fun refreshResult() {
            val current = store.loadSpeedCurrent(); val target = store.loadSpeedTarget()
            result.text = "$current → $target\nSelisih: ${target - current}"
        }
        refreshResult()
        addStepper(body, "Speed saat ini", { store.loadSpeedCurrent() }, { store.saveSpeedCurrent(it) }, 10) { refreshResult() }
        addStepper(body, "Target / benchmark", { store.loadSpeedTarget() }, { store.saveSpeedTarget(it) }, 10) { refreshResult() }
        body.addView(result, LinearLayout.LayoutParams(-1, dp(62)).apply { setMargins(0, dp(6), 0, dp(4)) })
        body.addView(infoCard("Monitor aman", "Hanya membandingkan angka manual. Tidak mengukur FPS atau memori game."))
    }

    private fun renderDexPage(body: LinearLayout) {
        val meta = index.meta
        body.addView(label("DEX CONNECTION", 15f, CYAN, true))
        body.addView(label("Data menu berasal dari tabel string classes.dex klien Aurcus Online.", 10.5f, MUTED))
        body.addView(infoCard("Sumber indeks", buildString {
            append("Berkas: ${meta.file} (dex ${meta.dexVersion})\n")
            append("Paket: ${meta.gamePackage}\n")
            append("Kelas tabel kunci: ${meta.sourceClass}\n")
            append("Kunci UI: ${index.keys.size} • Kelas di dex: ${meta.classCount}\n")
            append("SHA-256: ${meta.sha256.take(16)}…")
        }))
        val scan = store.loadDexScan()
        body.addView(infoCard("Verifikasi terakhir", scan?.let {
            "${it.fileName}: ${it.bundledFound}/${it.bundledTotal} kunci cocok • ${it.candidateCount} kandidat baru"
        } ?: "Belum ada. Pilih classes.dex atau APK di aplikasi utama, tab Dex."))
        GameFeatureCatalog.features.forEach { f ->
            body.addView(label("${f.code}  ${f.title}: ${index.countOf(f.id)}", 10f, Color.WHITE))
        }
        val open = smallButton("Buka tab Dex di aplikasi utama")
        open.setOnClickListener { openMainApp("dex") }
        body.addView(open, LinearLayout.LayoutParams(-1, dp(38)).apply { setMargins(0, dp(8), 0, 0) })
        val stop = smallButton("MATIKAN OVERLAY")
        stop.setOnClickListener { stopSelf() }
        body.addView(stop, LinearLayout.LayoutParams(-1, dp(40)).apply { setMargins(0, dp(4), 0, 0) })
    }

    // ---- Komponen UI ----

    /** Creates a slider (persisten lewat LocalStore) and reports changes, for drawing a local preview. */
    private fun addSlider(
        body: LinearLayout, title: String, key: String,
        minValue: Int, maxValue: Int, defaultValue: Int, suffix: String,
        onValueChanged: ((Int) -> Unit)? = null
    ) {
        val saved = store.loadScale(key, defaultValue).coerceIn(minValue, maxValue)
        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        heading.addView(label(title, 10.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        val valueLabel = label("$saved$suffix", 10.5f, CYAN, true)
        heading.addView(valueLabel)
        body.addView(heading)
        val seek = SeekBar(this)
        seek.max = maxValue - minValue
        seek.progress = saved - minValue
        seek.progressTintList = ColorStateList.valueOf(CYAN)
        seek.thumbTintList = ColorStateList.valueOf(GOLD)
        seek.progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(42, 66, 87))
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = minValue + progress
                valueLabel.text = "$value$suffix"
                store.saveScale(key, value)
                onValueChanged?.invoke(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        body.addView(seek, LinearLayout.LayoutParams(-1, dp(38)))
    }

    /** Pengisi angka tanpa keyboard (overlay tidak bisa fokus), langkah tetap lewat tombol − dan +. */
    private fun addStepper(
        body: LinearLayout, title: String, read: () -> Int, write: (Int) -> Unit, step: Int, onChange: () -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(4), dp(4))
            background = rounded(PANEL, dp(10))
        }
        val value = label(read().toString(), 12f, CYAN, true).apply { gravity = Gravity.CENTER }
        val minus = smallButton("−")
        val plus = smallButton("+")
        minus.setOnClickListener { write((read() - step).coerceAtLeast(0)); value.text = read().toString(); onChange() }
        plus.setOnClickListener { write(read() + step); value.text = read().toString(); onChange() }
        row.addView(label(title, 10.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(minus, LinearLayout.LayoutParams(dp(34), dp(34)))
        row.addView(value, LinearLayout.LayoutParams(dp(62), -2))
        row.addView(plus, LinearLayout.LayoutParams(dp(34), dp(34)))
        body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(3), 0, dp(3)) })
    }

    private fun lastLines(text: String, count: Int): String = text.lines().filter { it.isNotBlank() }.takeLast(count).joinToString("\n")

    private fun toast(message: String) { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }

    private fun infoCard(title: String, body: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(9), dp(8), dp(9), dp(8))
        background = rounded(PANEL, dp(10))
        addView(label(title, 10.5f, GOLD, true))
        addView(label(body, 10f, Color.WHITE))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(4)) }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(6), dp(4), dp(6), dp(4))
    }

    private fun smallButton(text: String) = Button(this).apply {
        this.text = text; textSize = 9f; isAllCaps = false; setTextColor(Color.WHITE)
        background = rounded(Color.rgb(18, 57, 86), dp(9), CYAN, dp(1))
        minHeight = dp(30); minimumHeight = dp(30); minWidth = dp(0); minimumWidth = dp(0)
        setPadding(dp(2), 0, dp(2), 0)
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 1): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat(); if (stroke != null) setStroke(strokeWidth, stroke)
    }

    private fun updateLayoutSize(view: View) {
        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        overlayRoot?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        overlayRoot = null
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class RingPreviewView(private val aoe: Boolean, private var scale: Int, private var effect: Int) : View(this@FloatingOverlayService) {
        fun setScale(value: Int) { scale = value.coerceIn(50, 300); invalidate() }
        fun setEffect(value: Int) { effect = value.coerceIn(50, 300); invalidate() }
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PANEL; style = Paint.Style.FILL }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (aoe) 0x3322B8F0 else 0x22E8B84B; style = Paint.Style.FILL }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (aoe) CYAN else GOLD; style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat() }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x6622B8F0; style = Paint.Style.STROKE; strokeWidth = dp(1).toFloat() }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x557DA7C7; strokeWidth = dp(1).toFloat() }
        private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = dp(10).toFloat(); typeface = Typeface.DEFAULT_BOLD }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), dp(10).toFloat(), dp(10).toFloat(), bgPaint)
            val cx = width / 2f; val cy = height / 2f
            val maxRadius = minOf(width, height) * 0.37f
            val radius = maxRadius * (scale.coerceIn(50, 300) / 300f)
            canvas.drawLine(cx - maxRadius - dp(10), cy, cx + maxRadius + dp(10), cy, linePaint)
            canvas.drawLine(cx, cy - maxRadius - dp(10), cx, cy + maxRadius + dp(10), linePaint)
            canvas.drawCircle(cx, cy, maxRadius, linePaint)
            canvas.drawCircle(cx, cy, radius, fillPaint)
            canvas.drawCircle(cx, cy, radius, ringPaint)
            if (aoe) {
                // Efek visual: lingkar cahaya dan titik percik yang ukurannya mengikuti slider efek.
                val e = effect.coerceIn(50, 300) / 100f
                canvas.drawCircle(cx, cy, radius * (1f + 0.06f * e), glowPaint)
                for (i in 0 until 8) {
                    val angle = Math.toRadians(i * 45.0)
                    canvas.drawCircle(cx + (radius * Math.cos(angle)).toFloat(), cy + (radius * Math.sin(angle)).toFloat(), dp(2) * e, dotPaint)
                }
            }
            canvas.drawCircle(cx, cy, dp(4).toFloat(), dotPaint)
            canvas.drawText(if (aoe) "SKILL AoE PREVIEW" else "ATTACK RANGE PREVIEW", dp(8).toFloat(), dp(16).toFloat(), textPaint)
            canvas.drawText(if (aoe) "${scale}% radius • ${effect}% efek" else "${scale}% visual", dp(8).toFloat(), height - dp(8).toFloat(), textPaint)
        }
    }

    companion object {
        const val CHANNEL_ID = "aurcus_floating_overlay"
        const val NOTIFICATION_ID = 9201
        private const val KEY_PAGE = 30
        private const val BASE_DAMAGE_SP = 16f
        private val PAGES = listOf("MOD", "MENU", "BUILD", "DMG", "RANGE", "AOE", "QUEST", "MAP", "SPAWN", "FARM", "SPEED", "DEX")
        /** Halaman yang hanya muncul jika toggle di panel MOD aktif. */
        private val GATES = mapOf(
            "DMG" to OverlayOptions.T_DAMAGE, "RANGE" to OverlayOptions.T_RANGE, "AOE" to OverlayOptions.T_AOE,
            "QUEST" to OverlayOptions.T_QUEST, "SPEED" to OverlayOptions.T_SPEED, "FARM" to OverlayOptions.T_FARM
        )
        // Use regular vals because .toInt() is a function call, not a Kotlin const initializer.
        private val CYAN = 0xFF22B8F0.toInt()
        private val GOLD = 0xFFE8B84B.toInt()
        private val MUTED = 0xFFA8BED1.toInt()
        private val PANEL = 0xFF10243A.toInt()
    }
}

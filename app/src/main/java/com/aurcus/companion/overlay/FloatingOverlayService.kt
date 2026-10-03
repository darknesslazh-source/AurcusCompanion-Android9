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
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/**
 * Aurcus Companion local overlay. Sliders and switches control companion UI previews only.
 * This service does not inject into the game, alter game memory, automate gameplay, or contact game servers.
 */
class FloatingOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var overlayRoot: LinearLayout? = null
    private var content: LinearLayout? = null
    private var expanded = true
    private var selectedPage = "MOD"
    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f

    private val prefs by lazy { getSharedPreferences("aurcus_overlay", MODE_PRIVATE) }
    private val toggles = linkedMapOf(
        "Damage HUD preview" to true,
        "Attack range preview" to true,
        "Skill AoE preview" to true,
        "Skill effect preview" to true,
        "Speed monitor" to false,
        "Quest checklist" to true,
        "Farm session timer" to false
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        toggles.keys.toList().forEach { key -> toggles[key] = prefs.getBoolean(key, toggles[key] ?: false) }
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
        val title = TextView(this).apply {
            text = "✦  AURCUS COMPANION\n     VISUAL TOOLKIT • READY"
            setTextColor(Color.WHITE); textSize = 12f; typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(5), dp(5), dp(3), dp(5))
        }
        val fold = smallButton(if (expanded) "Lipat" else "Buka")
        val close = smallButton("×")
        header.addView(title, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(fold, LinearLayout.LayoutParams(dp(60), dp(36)))
        header.addView(close, LinearLayout.LayoutParams(dp(36), dp(36)))
        root.addView(header)

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("MOD", "DMG", "RANGE", "AOE", "QUEST", "MORE").forEach { page ->
            val button = smallButton(page).apply { textSize = 8.5f }
            nav.addView(button, LinearLayout.LayoutParams(0, dp(34), 1f).apply { setMargins(dp(1), dp(3), dp(1), dp(3)) })
            button.setOnClickListener { selectedPage = page; renderPage() }
        }
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
                MotionEvent.ACTION_MOVE -> { params.x = initialX + (event.rawX - touchX).toInt(); params.y = initialY + (event.rawY - touchY).toInt(); updateLayoutSize(root); true }
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
    }

    private fun renderPage() {
        val body = content ?: return
        body.removeAllViews()
        when (selectedPage) {
            "MOD" -> renderModPage(body)
            "DMG" -> renderDamagePage(body)
            "RANGE" -> renderRangePage(body)
            "AOE" -> renderAoEPage(body)
            "QUEST" -> renderQuestPage(body)
            else -> renderMorePage(body)
        }
        body.addView(label("LOCAL PREVIEW ONLY • DOES NOT CHANGE GAME/SERVER", 8.5f, MUTED))
    }

    private fun renderModPage(body: LinearLayout) {
        body.addView(label("MODIFICATION PANEL", 15f, CYAN, true))
        body.addView(label("Atur pratinjau visual dan alat pendamping.", 10.5f, MUTED))
        toggles.forEach { (name, enabled) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(5), dp(6), dp(5))
                background = rounded(PANEL, dp(10))
            }
            val check = CheckBox(this).apply {
                text = name; isChecked = enabled; setTextColor(Color.WHITE); textSize = 11.5f
                buttonTintList = ColorStateList.valueOf(CYAN)
            }
            row.addView(check)
            row.addView(label(descriptionFor(name), 9.5f, MUTED))
            check.setOnCheckedChangeListener { _, checked ->
                toggles[name] = checked
                prefs.edit().putBoolean(name, checked).apply()
            }
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        body.addView(infoCard("Cara pakai", "DMG, RANGE, dan AOE mengubah tampilan pratinjau di companion, bukan damage atau hitbox Aurcus Online."))
    }

    private fun renderDamagePage(body: LinearLayout) {
        body.addView(label("DAMAGE HUD", 15f, CYAN, true))
        body.addView(label("Ubah skala angka dan panel damage contoh.", 10.5f, MUTED))
        addSlider(body, "Skala angka damage", "damage_scale", 50, 200, 100, "%")
        addSlider(body, "Durasi indikator", "damage_duration", 1, 5, 2, " dtk")
        body.addView(infoCard("Pratinjau angka", "1,245   CRIT  ×1.8\nAngka contoh untuk desain HUD; tidak berasal dari game."))
    }

    private fun renderRangePage(body: LinearLayout) {
        body.addView(label("ATTACK RANGE", 15f, CYAN, true))
        body.addView(label("Sesuaikan lingkaran referensi di panel companion.", 10.5f, MUTED))
        val preview = RingPreviewView(this, false, prefs.getInt("attack_range", 100))
        addSlider(body, "Radius visual", "attack_range", 50, 300, 100, "%") { preview.setScale(it) }
        body.addView(preview.apply { layoutParams = LinearLayout.LayoutParams(-1, dp(150)).apply { setMargins(0, dp(7), 0, dp(7)) } })
        body.addView(infoCard("Catatan", "Lingkaran ini hanya panduan visual di companion. Tidak mengubah jangkauan serangan atau hit detection game."))
    }

    private fun renderAoEPage(body: LinearLayout) {
        body.addView(label("SKILL AREA / AoE", 15f, CYAN, true))
        body.addView(label("Atur pratinjau radius dan ukuran efek visual.", 10.5f, MUTED))
        val preview = RingPreviewView(this, true, prefs.getInt("aoe_radius", 100))
        addSlider(body, "Radius area visual", "aoe_radius", 50, 300, 100, "%") { preview.setScale(it) }
        addSlider(body, "Skala efek visual", "effect_scale", 50, 250, 100, "%") { preview.setScale(it) }
        body.addView(preview.apply { layoutParams = LinearLayout.LayoutParams(-1, dp(155)).apply { setMargins(0, dp(7), 0, dp(7)) } })
        body.addView(infoCard("Pratinjau", "Bentuk dan skala hanya ditampilkan pada panel companion, bukan area efek skill dalam game."))
    }

    private fun renderQuestPage(body: LinearLayout) {
        body.addView(label("QUEST & FARM", 15f, CYAN, true))
        body.addView(label("Checklist lokal untuk membantu mengingat tujuan.", 10.5f, MUTED))
        listOf("Periksa daily quest", "Catat objective aktif", "Kumpulkan item target", "Catat lokasi farming", "Review quest selesai").forEach { text ->
            val check = CheckBox(this).apply {
                this.text = text; setTextColor(Color.WHITE); textSize = 11f
                buttonTintList = ColorStateList.valueOf(CYAN)
                isChecked = prefs.getBoolean("quest_$text", false)
            }
            check.setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("quest_$text", checked).apply() }
            body.addView(check)
        }
        body.addView(infoCard("Farm session", "Gunakan timer pada aplikasi utama untuk mencatat durasi sesi. Tidak ada input otomatis ke game."))
    }

    private fun renderMorePage(body: LinearLayout) {
        body.addView(label("TOOLS LAINNYA", 15f, CYAN, true))
        body.addView(label("Catatan manual dan pengaturan companion.", 10.5f, MUTED))
        listOf(
            "MAP & ROUTE" to "Catat area favorit, jalur farming, dan titik rute.",
            "SPAWN TRACKER" to "Catat waktu dan lokasi monster yang diamati.",
            "SPEED MONITOR" to "Bandingkan statistik yang dimasukkan secara manual.",
            "HIDDEN QUEST NOTES" to "Catatan pribadi; tidak membuka quest yang dikunci server."
        ).forEach { (title, desc) -> body.addView(infoCard(title, desc)) }
        val stop = smallButton("MATIKAN OVERLAY")
        stop.setOnClickListener { stopSelf() }
        body.addView(stop, LinearLayout.LayoutParams(-1, dp(40)).apply { setMargins(0, dp(8), 0, 0) })
    }

    /** Creates a slider and returns its current value, for drawing a local preview. */
    private fun addSlider(body: LinearLayout, title: String, key: String, min: Int, max: Int, default: Int, suffix: String, onValueChanged: ((Int) -> Unit)? = null): Int {
        val saved = prefs.getInt(key, default).coerceIn(min, max)
        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        heading.addView(label(title, 10.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        val valueLabel = label("$saved$suffix", 10.5f, CYAN, true)
        heading.addView(valueLabel)
        body.addView(heading)
        val seek = SeekBar(this).apply {
            max = max - min
            progress = saved - min
            progressTintList = ColorStateList.valueOf(CYAN)
            thumbTintList = ColorStateList.valueOf(GOLD)
            progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(42, 66, 87))
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = min + progress
                valueLabel.text = "$value$suffix"
                prefs.edit().putInt(key, value).apply()
                onValueChanged?.invoke(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        body.addView(seek, LinearLayout.LayoutParams(-1, dp(38)))
        return min + seek.progress
    }

    private fun descriptionFor(name: String): String = when (name) {
        "Damage HUD preview" -> "Tampilan angka contoh dan skala panel damage"
        "Attack range preview" -> "Lingkaran referensi jangkauan visual"
        "Skill AoE preview" -> "Pratinjau radius area skill"
        "Skill effect preview" -> "Skala efek visual di panel companion"
        "Speed monitor" -> "Catatan statistik manual; tanpa ubah speed karakter"
        "Quest checklist" -> "Checklist tujuan pribadi"
        else -> "Timer sesi farming tanpa auto-input"
    }

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
        overlayRoot?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        overlayRoot = null
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class RingPreviewView(context: android.content.Context, private val aoe: Boolean, private var scale: Int) : View(context) {
        fun setScale(value: Int) { scale = value.coerceIn(50, 300); invalidate() }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (aoe) 0x3322B8F0 else 0x22E8B84B; style = Paint.Style.FILL }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (aoe) CYAN else GOLD; style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat() }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x557DA7C7; strokeWidth = dp(1).toFloat() }
        private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = dp(10).toFloat(); typeface = Typeface.DEFAULT_BOLD }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), dp(10).toFloat(), dp(10).toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PANEL; style = Paint.Style.FILL })
            val cx = width / 2f; val cy = height / 2f
            val maxRadius = minOf(width, height) * 0.37f
            val radius = maxRadius * (scale.coerceIn(50, 300) / 300f)
            canvas.drawLine(cx - maxRadius - dp(10), cy, cx + maxRadius + dp(10), cy, linePaint)
            canvas.drawLine(cx, cy - maxRadius - dp(10), cx, cy + maxRadius + dp(10), linePaint)
            canvas.drawCircle(cx, cy, maxRadius, linePaint)
            canvas.drawCircle(cx, cy, radius, fillPaint)
            canvas.drawCircle(cx, cy, radius, ringPaint)
            canvas.drawCircle(cx, cy, dp(4).toFloat(), dotPaint)
            canvas.drawText(if (aoe) "SKILL AoE PREVIEW" else "ATTACK RANGE PREVIEW", dp(8).toFloat(), dp(16).toFloat(), textPaint)
            canvas.drawText("${scale}% visual", dp(8).toFloat(), height - dp(8).toFloat(), textPaint)
        }
    }

    companion object {
        const val CHANNEL_ID = "aurcus_floating_overlay"
        const val NOTIFICATION_ID = 9201
        // Use regular vals because .toInt() is a function call, not a Kotlin const initializer.
        private val CYAN = 0xFF22B8F0.toInt()
        private val GOLD = 0xFFE8B84B.toInt()
        private val MUTED = 0xFFA8BED1.toInt()
        private val PANEL = 0xFF10243A.toInt()
    }
}

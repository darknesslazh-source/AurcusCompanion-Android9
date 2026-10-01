package com.aurcus.companion.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Small draggable overlay companion. Android requires the user to explicitly
 * grant "Display over other apps" before this service can be started.
 */
class FloatingOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayRoot: LinearLayout? = null
    private var expanded = false
    private lateinit var params: WindowManager.LayoutParams
    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Aurcus floating companion",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Aurcus Companion aktif")
            .setContentText("Jendela mengambang sedang berjalan")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    @Suppress("DEPRECATION")\n    private fun createOverlay() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.rgb(25, 31, 40))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "AURCUS"
            setTextColor(Color.rgb(101, 214, 196))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), dp(4), dp(12), dp(4))
        }
        val toggle = Button(this).apply { text = "Buka" }
        val close = Button(this).apply { text = "×" }
        header.addView(title, LinearLayout.LayoutParams(0, dp(44), 1f))
        header.addView(toggle, LinearLayout.LayoutParams(dp(76), dp(44)))
        header.addView(close, LinearLayout.LayoutParams(dp(48), dp(44)))
        root.addView(header)

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            visibility = View.GONE
        }
        details.addView(TextView(this).apply {
            text = "Aurcus Companion\nPanel mengambang aktif. Geser bagian judul untuk memindahkan panel.\nGunakan aplikasi utama untuk Build, Items, Tracker, Compare, dan Monitor."
            setTextColor(Color.WHITE)
            textSize = 13f
        })
        root.addView(details)
        toggle.setOnClickListener {
            expanded = !expanded
            details.visibility = if (expanded) View.VISIBLE else View.GONE
            toggle.text = if (expanded) "Lipat" else "Buka"
            updateLayoutSize(root)
        }
        close.setOnClickListener { stopSelf() }

        params = WindowManager.LayoutParams(
            dp(250), WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(120)
        }

        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    try { windowManager.updateViewLayout(root, params) } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }

        overlayRoot = root
        windowManager.addView(root, params)
    }

    private fun updateLayoutSize(view: View) {
        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        overlayRoot?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayRoot = null
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "aurcus_floating_overlay"
        const val NOTIFICATION_ID = 9201
    }
}

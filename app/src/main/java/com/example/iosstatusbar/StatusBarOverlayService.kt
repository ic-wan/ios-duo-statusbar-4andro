package com.example.iosstatusbar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class StatusBarOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var monitor: SystemStatusMonitor
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.layout_stacked_status, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val initialSize = DEFAULT_SIZE.dp()
        params = WindowManager.LayoutParams(
            initialSize,
            initialSize,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = DEFAULT_Y.dp()
        }

        applyPreferences()
        windowManager.addView(overlayView, params)

        monitor = SystemStatusMonitor(this) { pct, charging, wifiConnected, wifiLevel, sim1, sim2, hasSim2 ->
            overlayView.post {
                val v = overlayView.findViewById<CircularStatusView>(R.id.circularStatusView)
                v.batteryPct = pct
                v.isCharging = charging
                v.isWifiConnected = wifiConnected
                v.wifiSignalLevel = wifiLevel
                v.cellularPrimaryLevel = sim1
                v.cellularSecondaryLevel = sim2
                v.hasSecondarySim = hasSim2
            }
        }
        monitor.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE) {
            applyPreferences()
            params?.let {
                try {
                    windowManager.updateViewLayout(overlayView, it)
                } catch (_: Exception) {
                }
            }
        }
        return START_STICKY
    }

    private fun applyPreferences() {
        if (!::overlayView.isInitialized) return

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sizeDp = prefs.getInt("size_dp", DEFAULT_SIZE).coerceIn(MIN_SIZE, MAX_SIZE)
        val posX = prefs.getInt("pos_x", DEFAULT_X).coerceIn(0, 100)
        val posY = prefs.getInt("pos_y", DEFAULT_Y).coerceIn(MIN_Y, MAX_Y)
        val alpha = prefs.getInt("alpha", 100).coerceIn(20, 100)
        val lineThickness = prefs.getInt("line_thickness_x10", 26) / 10f
        val textSize = prefs.getInt("text_size_x10", 95) / 10f
        val textWeight = prefs.getInt("text_weight", 800)
        val wifiStroke = prefs.getInt("wifi_stroke_x10", 21) / 10f
        val wifiScale = prefs.getInt("wifi_scale_x100", 125) / 100f
        val wifiOutside = prefs.getBoolean("wifi_outside", false)
        val wifiOffset = prefs.getInt("wifi_offset_x10", 0) / 10f
        val dotSize = prefs.getInt("dot_size_x10", 30) / 10f

        val p = params ?: return
        val sizePx = sizeDp.dp()
        val outsideMultiplier = if (wifiOutside) 2.15f else 1.0f
        val desiredWidthPx = (sizePx * outsideMultiplier).toInt().coerceAtLeast(sizePx)

        // In Camera Hole Mode the renderer needs horizontal room for
        // [battery text] + [camera/ring center] + [Wi-Fi]. Because the
        // WindowManager window is right-anchored, compensate X so the ring
        // center itself stays at the same screen location after width expands.
        val newHalfExtra = if (wifiOutside) {
            (desiredWidthPx - sizePx) / 2
        } else {
            0
        }

        p.width = desiredWidthPx
        p.height = sizePx
        p.x = (posX.dp() - newHalfExtra).coerceIn(-desiredWidthPx / 2, 3000)
        p.y = posY.dp()
        overlayView.alpha = alpha / 100f

        val view = overlayView.findViewById<CircularStatusView>(R.id.circularStatusView)
        view.lineThicknessDp = lineThickness
        view.batteryTextSizeSp = textSize
        view.batteryTextWeight = textWeight
        view.wifiStrokeDp = wifiStroke
        view.wifiIconScale = wifiScale
        view.wifiOutsideRing = wifiOutside
        view.wifiOutsideOffsetDp = wifiOffset
        view.cellularDotDp = dotSize

    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "iOS Duo Status Bar",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("iOS Duo Status Bar")
            .setContentText("System telemetry overlay aktif")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    override fun onDestroy() {
        if (::monitor.isInitialized) monitor.stop()
        if (::overlayView.isInitialized) {
            try {
                windowManager.removeView(overlayView)
            } catch (_: Exception) {
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_UPDATE = "com.example.iosstatusbar.UPDATE_CONFIG"
        private const val CHANNEL_ID = "ios_duo_overlay"
        private const val NOTIFICATION_ID = 7001
        private const val PREFS = "ios_prefs"
        private const val MIN_SIZE = 24
        private const val MAX_SIZE = 100
        private const val MIN_Y = -48
        private const val MAX_Y = 80
        private const val DEFAULT_SIZE = 34
        private const val DEFAULT_X = 10
        private const val DEFAULT_Y = -8
    }
}

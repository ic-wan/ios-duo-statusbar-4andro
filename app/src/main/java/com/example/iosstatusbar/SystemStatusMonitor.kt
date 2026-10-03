package com.example.iosstatusbar

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Collects the minimum system telemetry required by the Duo overlay.
 *
 * The implementation keeps minSdk 24 and deliberately avoids deprecated
 * connectivity broadcasts. Cellular monitoring uses PhoneStateListener as a
 * compatibility path; the deprecation is limited to that legacy Android API.
 */
@Suppress("DEPRECATION")
class SystemStatusMonitor(
    private val context: Context,
    private val onStatusChanged: (
        batteryPct: Int,
        isCharging: Boolean,
        isWifiConnected: Boolean,
        wifiSignalLevel: Int,
        cellularSignalLevel: Int
    ) -> Unit
) {
    private var cellularLevel: Int = 0
    private var started = false

    private val phoneListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            cellularLevel = signalStrength?.level?.coerceIn(0, 4) ?: 0
            update()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            update()
        }
    }

    fun start() {
        if (started) return
        started = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        registerCellularListenerIfAllowed()
        update()
    }

    @SuppressLint("MissingPermission")
    private fun registerCellularListenerIfAllowed() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cellularLevel = 0
            return
        }

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        tm?.listen(phoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    @SuppressLint("MissingPermission")
    private fun update() {
        val battery = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val pct = if (level >= 0 && scale > 0) {
            (level * 100f / scale).toInt().coerceIn(0, 100)
        } else 0

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        val wifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        val wifiLevel = if (wifiConnected) {
            getWifiSignalLevel(caps, wifiManager)
        } else {
            0
        }

        onStatusChanged(pct, charging, wifiConnected, wifiLevel, cellularLevel)
    }

    @SuppressLint("MissingPermission")
    private fun getWifiSignalLevel(
        capabilities: NetworkCapabilities?,
        wifiManager: WifiManager
    ): Int {
        // Android 10+ exposes signal strength through the active network without
        // requiring MediaProjection or a location-based Wi-Fi scan.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val dbm = capabilities?.signalStrength ?: Int.MIN_VALUE
            if (dbm != Int.MIN_VALUE) {
                // Typical Wi-Fi RSSI range is roughly -100..-30 dBm. Map it to 0..4.
                return when {
                    dbm >= -55 -> 4
                    dbm >= -67 -> 3
                    dbm >= -75 -> 2
                    dbm >= -85 -> 1
                    else -> 0
                }
            }
        }

        // Compatibility path for Android 7-9, where NetworkCapabilities.signalStrength
        // is not available. ACCESS_WIFI_STATE is declared in the manifest.
        val rssi = wifiManager.connectionInfo.rssi
        @Suppress("DEPRECATION")
        return WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!started) return
        started = false

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.listen(phoneListener, PhoneStateListener.LISTEN_NONE)
        }

        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered.
        }
    }
}

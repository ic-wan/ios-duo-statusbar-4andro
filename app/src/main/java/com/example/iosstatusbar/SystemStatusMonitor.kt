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
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Collects battery, Wi-Fi and cellular telemetry for the overlay.
 *
 * Cellular is intentionally represented as two levels so dual-SIM devices can
 * render SIM 1 above SIM 2. The legacy PhoneStateListener route is kept for
 * minSdk 24 compatibility and is isolated here.
 */
@Suppress("DEPRECATION")
class SystemStatusMonitor(
    private val context: Context,
    private val onStatusChanged: (
        batteryPct: Int,
        isCharging: Boolean,
        isWifiConnected: Boolean,
        wifiSignalLevel: Int,
        cellularPrimaryLevel: Int,
        cellularSecondaryLevel: Int,
        hasSecondarySim: Boolean
    ) -> Unit
) {
    private var cellularPrimaryLevel = 0
    private var cellularSecondaryLevel = 0
    private var hasSecondarySim = false
    private var started = false

    private val phoneManagers = mutableListOf<TelephonyManager>()
    private val phoneListeners = mutableListOf<PhoneStateListener>()

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

        registerCellularListeners()
        update()
    }

    @SuppressLint("MissingPermission")
    private fun activeSubscriptions(): List<SubscriptionInfo> {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        return try {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            sm?.activeSubscriptionInfoList
                ?.sortedWith(compareBy<SubscriptionInfo> { it.simSlotIndex }.thenBy { it.subscriptionId })
                ?.take(2)
                ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerCellularListeners() {
        stopCellularListeners()

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cellularPrimaryLevel = 0
            cellularSecondaryLevel = 0
            hasSecondarySim = false
            return
        }

        val subscriptions = activeSubscriptions()
        hasSecondarySim = subscriptions.size >= 2

        if (subscriptions.isEmpty()) {
            // Fallback for devices that temporarily do not expose an active subscription list.
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                val listener = createListener(isPrimary = true)
                phoneManagers += tm
                phoneListeners += listener
                tm.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            }
            return
        }

        subscriptions.forEachIndexed { index, info ->
            val baseTm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return@forEachIndexed
            val tm = baseTm.createForSubscriptionId(info.subscriptionId)

            val listener = createListener(isPrimary = index == 0)
            phoneManagers += tm
            phoneListeners += listener
            tm.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }
    }

    private fun createListener(isPrimary: Boolean): PhoneStateListener =
        object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                val level = signalStrength?.level?.coerceIn(0, 4) ?: 0
                if (isPrimary) {
                    cellularPrimaryLevel = level
                } else {
                    cellularSecondaryLevel = level
                }
                update()
            }
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
        } else {
            0
        }

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

        onStatusChanged(
            pct,
            charging,
            wifiConnected,
            wifiLevel,
            cellularPrimaryLevel,
            cellularSecondaryLevel,
            hasSecondarySim
        )
    }

    @SuppressLint("MissingPermission")
    private fun getWifiSignalLevel(
        capabilities: NetworkCapabilities?,
        wifiManager: WifiManager
    ): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val dbm = capabilities?.signalStrength ?: Int.MIN_VALUE
            if (dbm != Int.MIN_VALUE) {
                return when {
                    dbm >= -55 -> 4
                    dbm >= -67 -> 3
                    dbm >= -75 -> 2
                    dbm >= -85 -> 1
                    else -> 0
                }
            }
        }

        val rssi = wifiManager.connectionInfo.rssi
        return WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
    }

    @SuppressLint("MissingPermission")
    private fun stopCellularListeners() {
        phoneManagers.forEachIndexed { index, tm ->
            phoneListeners.getOrNull(index)?.let { listener ->
                try {
                    tm.listen(listener, PhoneStateListener.LISTEN_NONE)
                } catch (_: Exception) {
                }
            }
        }
        phoneManagers.clear()
        phoneListeners.clear()
    }

    fun stop() {
        if (!started) return
        started = false
        stopCellularListeners()
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
    }
}

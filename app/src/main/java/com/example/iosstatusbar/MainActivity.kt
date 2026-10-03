package com.example.iosstatusbar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var size: SeekBar
    private lateinit var x: SeekBar
    private lateinit var y: SeekBar
    private lateinit var alpha: SeekBar
    private lateinit var sizeLabel: TextView
    private lateinit var xLabel: TextView
    private lateinit var yLabel: TextView
    private lateinit var alphaLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.tvStatus)
        size = findViewById(R.id.seekSize)
        x = findViewById(R.id.seekX)
        y = findViewById(R.id.seekY)
        alpha = findViewById(R.id.seekAlpha)
        sizeLabel = findViewById(R.id.tvLabelSize)
        xLabel = findViewById(R.id.tvLabelX)
        yLabel = findViewById(R.id.tvLabelY)
        alphaLabel = findViewById(R.id.tvLabelAlpha)

        val prefs = getSharedPreferences("ios_prefs", Context.MODE_PRIVATE)
        size.progress = prefs.getInt("size_dp", 72)
        x.progress = prefs.getInt("pos_x", 12)
        y.progress = prefs.getInt("pos_y", 6)
        alpha.progress = prefs.getInt("alpha", 100)
        refreshLabels()

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) = refreshLabels()
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        size.setOnSeekBarChangeListener(listener)
        x.setOnSeekBarChangeListener(listener)
        y.setOnSeekBarChangeListener(listener)
        alpha.setOnSeekBarChangeListener(listener)

        findViewById<Button>(R.id.btnStart).setOnClickListener { startFlow() }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, StatusBarOverlayService::class.java))
            status.text = "Status: Nonaktif"
        }
        findViewById<Button>(R.id.btnApply).setOnClickListener {
            saveConfig()
            if (Settings.canDrawOverlays(this)) {
                val i = Intent(this, StatusBarOverlayService::class.java)
                    .setAction(StatusBarOverlayService.ACTION_UPDATE)
                ContextCompat.startForegroundService(this, i)
                status.text = "Status: Config diterapkan"
            }
        }
    }

    private fun startFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_PHONE_STATE)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }
        saveConfig()
        val intent = Intent(this, StatusBarOverlayService::class.java)
        ContextCompat.startForegroundService(this, intent)
        status.text = "Status: Aktif"
        Toast.makeText(this, "iOS Duo Status Bar aktif", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PHONE_STATE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startFlow()
        } else if (requestCode == REQUEST_PHONE_STATE) {
            Toast.makeText(this, "Izin sinyal seluler diperlukan untuk indikator cellular.", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveConfig() {
        getSharedPreferences("ios_prefs", Context.MODE_PRIVATE).edit()
            .putInt("size_dp", size.progress.coerceIn(44, 120))
            .putInt("pos_x", x.progress)
            .putInt("pos_y", y.progress)
            .putInt("alpha", alpha.progress.coerceIn(20, 100))
            .apply()
    }

    private fun refreshLabels() {
        sizeLabel.text = "Ukuran: ${size.progress.coerceIn(44, 120)} dp"
        xLabel.text = "Offset X: ${x.progress} dp"
        yLabel.text = "Offset Y: ${y.progress} dp"
        alphaLabel.text = "Opacity: ${alpha.progress}%"
    }

    companion object {
        private const val REQUEST_PHONE_STATE = 5001
    }
}

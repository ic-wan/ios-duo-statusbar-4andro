package com.example.iosstatusbar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
    private lateinit var line: SeekBar
    private lateinit var textSize: SeekBar
    private lateinit var textWeight: SeekBar
    private lateinit var wifiStroke: SeekBar
    private lateinit var wifiLayout: android.widget.Switch
    private lateinit var wifiOffset: SeekBar
    private lateinit var dotSize: SeekBar
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
        line = findViewById(R.id.seekLine)
        textSize = findViewById(R.id.seekTextSize)
        textWeight = findViewById(R.id.seekTextWeight)
        wifiStroke = findViewById(R.id.seekWifiStroke)
        wifiLayout = findViewById(R.id.switchWifiOutside)
        wifiOffset = findViewById(R.id.seekWifiOffset)
        dotSize = findViewById(R.id.seekDotSize)
        sizeLabel = findViewById(R.id.tvLabelSize)
        xLabel = findViewById(R.id.tvLabelX)
        yLabel = findViewById(R.id.tvLabelY)
        alphaLabel = findViewById(R.id.tvLabelAlpha)

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedSize = prefs.getInt("size_dp", DEFAULT_SIZE)
        val savedX = prefs.getInt("pos_x", DEFAULT_X)
        val savedY = prefs.getInt("pos_y", DEFAULT_Y)
        val savedAlpha = prefs.getInt("alpha", DEFAULT_ALPHA)
        val savedLine = prefs.getInt("line_thickness_x10", 22)
        val savedTextSize = prefs.getInt("text_size_x10", 85)
        val savedTextWeight = prefs.getInt("text_weight", 700)
        val savedWifiStroke = prefs.getInt("wifi_stroke_x10", 19)
        val savedWifiOutside = prefs.getBoolean("wifi_outside", false)
        val savedWifiOffset = prefs.getInt("wifi_offset_x10", 0)
        val savedDotSize = prefs.getInt("dot_size_x10", 20)

        size.progress = (savedSize - MIN_SIZE).coerceIn(0, MAX_SIZE - MIN_SIZE)
        x.progress = savedX.coerceIn(MIN_X, MAX_X)
        y.progress = (savedY - MIN_Y).coerceIn(0, MAX_Y - MIN_Y)
        alpha.progress = savedAlpha.coerceIn(20, 100)
        line.progress = ((savedLine - 10) / 1).coerceIn(0, 30)
        textSize.progress = ((savedTextSize - 50) / 1).coerceIn(0, 90)
        textWeight.progress = ((savedTextWeight - 400) / 10).coerceIn(0, 50)
        wifiStroke.progress = ((savedWifiStroke - 10) / 1).coerceIn(0, 25)
        wifiLayout.isChecked = savedWifiOutside
        wifiOffset.progress = (savedWifiOffset + 240).coerceIn(0, 480)
        dotSize.progress = ((savedDotSize - 10) / 1).coerceIn(0, 30)
        refreshLabels()

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) = refreshLabels()
            override fun onStartTrackingTouch(s: SeekBar?) = Unit
            override fun onStopTrackingTouch(s: SeekBar?) = Unit
        }
        size.setOnSeekBarChangeListener(listener)
        x.setOnSeekBarChangeListener(listener)
        y.setOnSeekBarChangeListener(listener)
        alpha.setOnSeekBarChangeListener(listener)
        line.setOnSeekBarChangeListener(listener)
        textSize.setOnSeekBarChangeListener(listener)
        textWeight.setOnSeekBarChangeListener(listener)
        wifiStroke.setOnSeekBarChangeListener(listener)
        wifiOffset.setOnSeekBarChangeListener(listener)
        dotSize.setOnSeekBarChangeListener(listener)
        wifiLayout.setOnCheckedChangeListener { _, _ -> refreshLabels() }

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
            } else {
                Toast.makeText(this, "Aktifkan izin 'Tampil di atas aplikasi lain' terlebih dahulu.", Toast.LENGTH_LONG).show()
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
        ContextCompat.startForegroundService(this, Intent(this, StatusBarOverlayService::class.java))
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
        val actualSize = (size.progress + MIN_SIZE).coerceIn(MIN_SIZE, MAX_SIZE)
        val actualY = (y.progress + MIN_Y).coerceIn(MIN_Y, MAX_Y)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("size_dp", actualSize)
            .putInt("pos_x", x.progress.coerceIn(MIN_X, MAX_X))
            .putInt("pos_y", actualY)
            .putInt("alpha", alpha.progress.coerceIn(20, 100))
            .putInt("line_thickness_x10", 10 + line.progress.coerceIn(0, 30))
            .putInt("text_size_x10", 50 + textSize.progress.coerceIn(0, 90))
            .putInt("text_weight", 400 + textWeight.progress.coerceIn(0, 50) * 10)
            .putInt("wifi_stroke_x10", 10 + wifiStroke.progress.coerceIn(0, 25))
            .putBoolean("wifi_outside", wifiLayout.isChecked)
            .putInt("wifi_offset_x10", wifiOffset.progress.coerceIn(0, 480) - 240)
            .putInt("dot_size_x10", 10 + dotSize.progress.coerceIn(0, 30))
            .apply()
    }

    private fun refreshLabels() {
        val actualSize = (size.progress + MIN_SIZE).coerceIn(MIN_SIZE, MAX_SIZE)
        val actualY = (y.progress + MIN_Y).coerceIn(MIN_Y, MAX_Y)
        sizeLabel.text = "Ukuran ikon: $actualSize dp"
        xLabel.text = "Offset kanan (X): ${x.progress} dp"
        yLabel.text = "Offset vertikal (Y): $actualY dp"
        alphaLabel.text = "Opasitas: ${alpha.progress}%"
        findViewById<TextView>(R.id.tvLabelLine).text =
            "Ketebalan arc: ${(10 + line.progress) / 10f} dp"
        findViewById<TextView>(R.id.tvLabelTextSize).text =
            "Ukuran angka: ${(50 + textSize.progress) / 10f} sp"
        findViewById<TextView>(R.id.tvLabelTextWeight).text =
            "Ketebalan angka: ${400 + textWeight.progress * 10}"
        findViewById<TextView>(R.id.tvLabelWifiStroke).text =
            "Ketebalan Wi-Fi: ${(10 + wifiStroke.progress) / 10f} dp"
        findViewById<TextView>(R.id.tvLabelWifiOffset).text =
            "Offset Wi-Fi: ${(wifiOffset.progress - 240) / 10f} dp"
        findViewById<TextView>(R.id.tvLabelDotSize).text =
            "Ukuran titik operator: ${(10 + dotSize.progress) / 10f} dp"
    }

    companion object {
        private const val PREFS = "ios_prefs"
        private const val REQUEST_PHONE_STATE = 5001
        private const val MIN_SIZE = 24
        private const val MAX_SIZE = 100
        private const val MIN_X = 0
        private const val MAX_X = 100
        private const val MIN_Y = -48
        private const val MAX_Y = 80
        private const val DEFAULT_SIZE = 34
        private const val DEFAULT_X = 10
        private const val DEFAULT_Y = -8
        private const val DEFAULT_ALPHA = 100
    }
}

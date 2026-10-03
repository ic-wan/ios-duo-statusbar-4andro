package com.example.iosstatusbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.content.res.Configuration
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CircularStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var batteryPct: Int = 83
        set(value) { field = value.coerceIn(0, 100); invalidate() }

    var isWifiConnected: Boolean = true
        set(value) { field = value; invalidate() }

    var wifiSignalLevel: Int = 4
        set(value) { field = value.coerceIn(0, 4); invalidate() }

    var cellularSignalLevel: Int = 4
        set(value) { field = value.coerceIn(0, 4); invalidate() }

    var isCharging: Boolean = false
        set(value) { field = value; invalidate() }

    // White foreground + black halo = high contrast on both dark and light backgrounds.
    private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val haloFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(105, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dimFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(105, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    private val textHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    private val arcRect = RectF()

    private fun updateContrastPalette() {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // System appearance is a safe, zero-permission proxy for the dominant UI tone.
        // A black/white halo is always retained so mixed wallpapers remain readable.
        val foreground = if (night) Color.WHITE else Color.BLACK
        val outline = if (night) Color.BLACK else Color.WHITE
        fg.color = foreground
        fill.color = foreground
        text.color = foreground
        halo.color = outline
        haloFill.color = outline
        textHalo.color = outline
        dim.color = Color.argb(90, Color.red(foreground), Color.green(foreground), Color.blue(foreground))
        dimFill.color = Color.argb(90, Color.red(foreground), Color.green(foreground), Color.blue(foreground))
    }

    override fun onDraw(canvas: Canvas) {
        updateContrastPalette()

        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) * 0.405f
        val stroke = maxOf(1.5f, radius * 0.105f)

        // Battery number
        val textSize = radius * 0.50f
        text.textSize = textSize
        textHalo.textSize = textSize
        textHalo.strokeWidth = maxOf(1.5f, stroke * 0.55f)
        val textY = cy - radius * 0.91f
        val label = batteryPct.toString()
        canvas.drawText(label, cx, textY, textHalo)
        canvas.drawText(label, cx, textY, text)

        // Side brackets, deliberately separated like the reference image.
        val inset = radius * 0.06f
        arcRect.set(
            cx - radius + inset,
            cy - radius * 0.52f,
            cx + radius - inset,
            cy + radius * 0.88f
        )

        val sweep = 94f
        val active = 22f + 72f * (batteryPct / 100f)

        drawArcWithHalo(canvas, 140f, sweep, stroke)
        drawArcWithHalo(canvas, -50f, sweep, stroke)

        // Active battery portion. Keep both sides visually balanced.
        drawActiveArc(canvas, 140f, min(active, sweep), stroke)
        drawActiveArc(canvas, 40f, -min(active, sweep), stroke)

        // Wi-Fi icon
        val wifiY = cy + radius * 0.05f
        val wifiStroke = stroke * 0.82f
        for (i in 1..3) {
            val r = radius * (0.115f + i * 0.095f)
            arcRect.set(cx - r, wifiY - r, cx + r, wifiY + r)
            val activeWifi = isWifiConnected && i <= wifiSignalLevel
            drawArcWithHalo(
                canvas,
                225f,
                90f,
                wifiStroke,
                active = activeWifi
            )
        }

        val dotR = radius * 0.052f
        val dotY = wifiY + radius * 0.075f
        canvas.drawCircle(cx, dotY, dotR * 1.65f, haloFill)
        canvas.drawCircle(cx, dotY, dotR, if (isWifiConnected && wifiSignalLevel > 0) fill else dimFill)

        // Cellular dots. Four dots, gently curved under Wi-Fi.
        val dotRadius = radius * 0.061f
        val dotArcRadius = radius * 0.91f
        val angles = floatArrayOf(58f, 76f, 94f, 112f)
        for (i in angles.indices) {
            val rad = Math.toRadians(angles[i].toDouble())
            val x = cx + dotArcRadius * cos(rad).toFloat()
            val y = cy + dotArcRadius * sin(rad).toFloat()
            canvas.drawCircle(x, y, dotRadius * 1.55f, haloFill)
            canvas.drawCircle(
                x, y, dotRadius,
                if (i < cellularSignalLevel) fill else dimFill
            )
        }
    }

    private fun drawArcWithHalo(
        canvas: Canvas,
        start: Float,
        sweep: Float,
        stroke: Float,
        active: Boolean = true
    ) {
        halo.strokeWidth = stroke * 1.75f
        fg.strokeWidth = stroke
        if (active) {
            canvas.drawArc(arcRect, start, sweep, false, halo)
            canvas.drawArc(arcRect, start, sweep, false, fg)
        } else {
            dim.strokeWidth = stroke
            canvas.drawArc(arcRect, start, sweep, false, halo)
            canvas.drawArc(arcRect, start, sweep, false, dim)
        }
    }

    private fun drawActiveArc(canvas: Canvas, start: Float, sweep: Float, stroke: Float) {
        if (sweep <= 0f) return
        halo.strokeWidth = stroke * 1.75f
        fg.strokeWidth = stroke
        canvas.drawArc(arcRect, start, sweep, false, halo)
        canvas.drawArc(arcRect, start, sweep, false, fg)
    }
}

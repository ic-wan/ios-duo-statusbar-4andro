package com.example.iosstatusbar

import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Premium compact telemetry renderer.
 *
 * Geometry is intentionally drawn from primitives rather than bitmap assets so the
 * overlay stays sharp at every density and scale.
 */
class CircularStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var batteryPct: Int = 83
        set(value) { field = value.coerceIn(0, 100); invalidate() }

    var isWifiConnected: Boolean = true
        set(value) { field = value; invalidate() }

    /** 0..4 */
    var wifiSignalLevel: Int = 4
        set(value) { field = value.coerceIn(0, 4); invalidate() }

    /** 0..4 */
    var cellularSignalLevel: Int = 4
        set(value) { field = value.coerceIn(0, 4); invalidate() }

    var isCharging: Boolean = false
        set(value) { field = value; invalidate() }

    private val arcRect = RectF()

    private val foreground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val foregroundFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val haloFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val trackFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val number = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        isSubpixelText = true
    }
    private val numberHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        isSubpixelText = true
    }

    private fun updatePalette() {
        // Zero-permission adaptive contrast. Prefer Android's WallpaperColors when
        // available, then fall back to the system light/dark appearance. This is
        // deliberately not MediaProjection: the overlay never captures the screen.
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val wallpaperLuma = wallpaperLuminance()
        val useLightForeground = wallpaperLuma?.let { it < 0.48f } ?: night
        val fg = if (useLightForeground) Color.WHITE else Color.BLACK
        val outline = if (useLightForeground) Color.BLACK else Color.WHITE

        foreground.color = fg
        foregroundFill.color = fg
        number.color = fg
        halo.color = outline
        haloFill.color = outline
        numberHalo.color = outline

        track.color = Color.argb(72, Color.red(fg), Color.green(fg), Color.blue(fg))
        trackFill.color = Color.argb(72, Color.red(fg), Color.green(fg), Color.blue(fg))
    }

    private fun wallpaperLuminance(): Float? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return null
        return try {
            val colors = WallpaperManager.getInstance(context).getWallpaperColors(
                WallpaperManager.FLAG_SYSTEM
            ) ?: return null
            val swatches = listOfNotNull(
                colors.primaryColor?.toArgb(),
                colors.secondaryColor?.toArgb(),
                colors.tertiaryColor?.toArgb()
            )
            if (swatches.isEmpty()) null
            else swatches.map(::relativeLuminance).average().toFloat()
        } catch (_: Exception) {
            null
        }
    }

    private fun relativeLuminance(color: Int): Float {
        fun channel(value: Int): Float {
            val c = value / 255f
            return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        }
        return 0.2126f * channel(Color.red(color)) +
            0.7152f * channel(Color.green(color)) +
            0.0722f * channel(Color.blue(color))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        updatePalette()

        val size = min(width, height).toFloat()
        val cx = width / 2f
        // The composition is deliberately top-biased: this gives the icon a clean
        // relationship with a punch-hole/notch when the overlay is moved upward.
        val cy = height * 0.54f
        val radius = size * 0.385f
        val stroke = (size * 0.047f).coerceIn(1.35f, 3.0f)
        val haloStroke = stroke * 1.70f

        // ─────────────────────────────────────────────
        // 1) BATTERY: percentage above + two premium side arcs.
        //    Each side fills upward from the lower tip according to batteryPct.
        // ─────────────────────────────────────────────
        val textSize = (size * 0.155f).coerceIn(5.0f, 10.5f)
        number.textSize = textSize
        numberHalo.textSize = textSize
        numberHalo.strokeWidth = maxOf(1.0f, stroke * 0.52f)
        val textY = cy - radius * 0.78f
        drawTextWithHalo(canvas, batteryPct.toString(), cx, textY)

        val batteryRect = RectF(
            cx - radius,
            cy - radius * 0.88f,
            cx + radius,
            cy + radius * 0.92f
        )
        arcRect.set(batteryRect)

        val sideSweep = 76f
        val activeSweep = sideSweep * (batteryPct / 100f)

        // Dim complete tracks establish the elegant bracket silhouette.
        drawArcTrack(canvas, 140f, sideSweep, stroke, haloStroke)
        drawArcTrack(canvas, 40f, -sideSweep, stroke, haloStroke)

        // Active battery energy, mirrored left/right for a balanced Duo shape.
        drawArcActive(canvas, 140f, activeSweep, stroke, haloStroke)
        drawArcActive(canvas, 40f, -activeSweep, stroke, haloStroke)

        // ─────────────────────────────────────────────
        // 2) WI-FI: three nested arcs + center dot.
        //    When disconnected every bar becomes hollow/dim.
        // ─────────────────────────────────────────────
        val wifiCy = cy + radius * 0.08f
        val wifiStroke = (stroke * 0.82f).coerceAtLeast(1.2f)
        val wifiSweep = 104f
        for (i in 1..3) {
            val r = radius * (0.105f + i * 0.075f)
            arcRect.set(cx - r, wifiCy - r, cx + r, wifiCy + r)
            val active = isWifiConnected && i <= wifiSignalLevel
            if (active) {
                drawArcActive(canvas, 218f, wifiSweep, wifiStroke, wifiStroke * 1.55f)
            } else {
                drawArcTrack(canvas, 218f, wifiSweep, wifiStroke, wifiStroke * 1.55f)
            }
        }

        val wifiDotRadius = (radius * 0.038f).coerceAtLeast(1.15f)
        val wifiDotY = wifiCy + radius * 0.10f
        canvas.drawCircle(cx, wifiDotY, wifiDotRadius * 1.55f, haloFill)
        canvas.drawCircle(
            cx,
            wifiDotY,
            wifiDotRadius,
            if (isWifiConnected && wifiSignalLevel > 0) foregroundFill else trackFill
        )

        // ─────────────────────────────────────────────
        // 3) CELLULAR: four dots on a shallow lower arc.
        //    Number of bright dots equals signal strength 0..4.
        // ─────────────────────────────────────────────
        val dotRadius = (radius * 0.043f).coerceAtLeast(1.2f)
        val dotArcRadius = radius * 0.77f
        val angles = floatArrayOf(58f, 76f, 94f, 112f)
        for (i in angles.indices) {
            val rad = Math.toRadians(angles[i].toDouble())
            val x = cx + dotArcRadius * cos(rad).toFloat()
            val y = cy + dotArcRadius * sin(rad).toFloat()
            canvas.drawCircle(x, y, dotRadius * 1.55f, haloFill)
            canvas.drawCircle(x, y, dotRadius, if (i < cellularSignalLevel) foregroundFill else trackFill)
        }
    }

    private fun drawTextWithHalo(canvas: Canvas, value: String, x: Float, y: Float) {
        canvas.drawText(value, x, y, numberHalo)
        canvas.drawText(value, x, y, number)
    }

    private fun drawArcTrack(
        canvas: Canvas,
        start: Float,
        sweep: Float,
        stroke: Float,
        haloStroke: Float
    ) {
        halo.strokeWidth = haloStroke
        track.strokeWidth = stroke
        canvas.drawArc(arcRect, start, sweep, false, halo)
        canvas.drawArc(arcRect, start, sweep, false, track)
    }

    private fun drawArcActive(
        canvas: Canvas,
        start: Float,
        sweep: Float,
        stroke: Float,
        haloStroke: Float
    ) {
        if (sweep <= 0f) return
        halo.strokeWidth = haloStroke
        foreground.strokeWidth = stroke
        canvas.drawArc(arcRect, start, sweep, false, halo)
        canvas.drawArc(arcRect, start, sweep, false, foreground)
    }
}

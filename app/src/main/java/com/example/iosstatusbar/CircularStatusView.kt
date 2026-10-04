package com.example.iosstatusbar

import android.app.WallpaperManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
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

    /** User-tunable renderer settings. */
    var lineThicknessDp: Float = 2.2f
        set(value) { field = value.coerceIn(1.0f, 4.0f); invalidate() }

    var batteryTextSizeSp: Float = 8.5f
        set(value) { field = value.coerceIn(5.0f, 14.0f); invalidate() }

    /** 400..900, mapped to Android font weights where available. */
    var batteryTextWeight: Int = 700
        set(value) { field = value.coerceIn(400, 900); updateNumberTypeface(); invalidate() }

    /** Wi-Fi can be moved outside the battery ring to leave the camera-hole center clear. */
    var wifiOutsideRing: Boolean = false
        set(value) { field = value; invalidate() }

    /** Horizontal offset, in dp, relative to the center of the overlay. */
    var wifiOutsideOffsetDp: Float = 0f
        set(value) { field = value.coerceIn(-24f, 24f); invalidate() }

    var wifiStrokeDp: Float = 1.9f
        set(value) { field = value.coerceIn(1.0f, 3.5f); invalidate() }

    var cellularDotDp: Float = 2.0f
        set(value) { field = value.coerceIn(1.0f, 4.0f); invalidate() }

    private val arcRect = RectF()

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun updateNumberTypeface() {
        number.typeface = Typeface.create("sans-serif", batteryTextWeight)
        numberHalo.typeface = Typeface.create("sans-serif", batteryTextWeight)
    }

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

    private var cachedWallpaperLuminance: Float? = null
    private var paletteInitialized = false
    private var paletteRefreshStarted = false

    private fun applyPalette(wallpaperLuma: Float?) {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
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
        paletteInitialized = true
    }

    private fun refreshPaletteAsync() {
        if (paletteRefreshStarted) return
        paletteRefreshStarted = true

        Thread {
            val luma = wallpaperLuminance()
            post {
                cachedWallpaperLuminance = luma
                paletteRefreshStarted = false
                applyPalette(cachedWallpaperLuminance)
                invalidate()
            }
        }.apply { name = "ios-duo-wallpaper-colors" }.start()
    }

    @SuppressLint("NewApi")
    private fun wallpaperLuminance(): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyPalette(null)
        refreshPaletteAsync()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        if (!paletteInitialized) applyPalette(null)

        val size = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height * 0.54f
        val radius = size * 0.385f

        // Keep the core stroke visually strong even when the whole overlay is small.
        val stroke = dp(lineThicknessDp).coerceIn(dp(1.0f), dp(4.0f))
        val haloStroke = (stroke * 1.18f).coerceAtLeast(dp(2.35f))

        // Battery percentage: independent size + weight controls.
        val textSize = dp(batteryTextSizeSp * 0.82f).coerceIn(dp(5.0f), dp(14.0f))
        updateNumberTypeface()
        number.textSize = textSize
        numberHalo.textSize = textSize
        numberHalo.strokeWidth = maxOf(dp(0.45f), stroke * 0.22f)

        // In camera-hole mode the Wi-Fi indicator shares the same horizontal row
        // as the battery percentage, rather than stacking above it.
        val textY = cy - radius * 0.82f
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

        drawArcTrack(canvas, 140f, sideSweep, stroke, haloStroke)
        drawArcTrack(canvas, 40f, -sideSweep, stroke, haloStroke)

        drawArcActive(canvas, 140f, activeSweep, stroke, haloStroke)
        drawArcActive(canvas, 40f, -activeSweep, stroke, haloStroke)

        // ─────────────────────────────────────────────
        // WI-FI
        // Standard mode: inside the battery ring.
        // Camera-hole mode: outside the ring, horizontally aligned
        // with the battery percentage.
        // ─────────────────────────────────────────────
        val wifiStroke = dp(wifiStrokeDp).coerceIn(dp(1.0f), dp(3.5f))

        val wifiCx: Float
        val wifiCy: Float

        if (wifiOutsideRing) {
            val horizontalGap = radius * 0.95f
            wifiCx = cx + horizontalGap + dp(wifiOutsideOffsetDp)
            wifiCy = textY - dp(1.0f)
        } else {
            wifiCx = cx
            wifiCy = cy + radius * 0.05f
        }

        // Three clearly readable nested arcs.
        val wifiBaseR = if (wifiOutsideRing) radius * 0.26f else radius * 0.17f
        val wifiStep = if (wifiOutsideRing) radius * 0.075f else radius * 0.065f
        val wifiSweep = 104f

        for (i in 1..3) {
            val r = wifiBaseR + (i - 1) * wifiStep
            arcRect.set(
                wifiCx - r,
                wifiCy - r,
                wifiCx + r,
                wifiCy + r
            )

            val active = isWifiConnected && i <= wifiSignalLevel

            if (active) {
                drawArcActive(
                    canvas,
                    218f,
                    wifiSweep,
                    wifiStroke,
                    wifiStroke * 1.22f
                )
            } else {
                drawArcTrack(
                    canvas,
                    218f,
                    wifiSweep,
                    wifiStroke,
                    wifiStroke * 1.22f
                )
            }
        }

        val wifiDotRadius = if (wifiOutsideRing) {
            dp(1.35f)
        } else {
            maxOf(radius * 0.038f, dp(1.25f))
        }

        val wifiDotY = wifiCy + if (wifiOutsideRing) dp(2.2f) else radius * 0.10f

        canvas.drawCircle(
            wifiCx,
            wifiDotY,
            wifiDotRadius * 1.30f,
            haloFill
        )

        canvas.drawCircle(
            wifiCx,
            wifiDotY,
            wifiDotRadius,
            if (isWifiConnected && wifiSignalLevel > 0) {
                foregroundFill
            } else {
                trackFill
            }
        )

        // ─────────────────────────────────────────────
        // CELLULAR: four dots on a shallow lower arc.
        // ─────────────────────────────────────────────
        val dotRadius = dp(cellularDotDp * 0.5f)
        val dotArcRadius = radius * 0.77f
        val angles = floatArrayOf(58f, 76f, 94f, 112f)

        for (i in angles.indices) {
            val rad = Math.toRadians(angles[i].toDouble())
            val x = cx + dotArcRadius * cos(rad).toFloat()
            val y = cy + dotArcRadius * sin(rad).toFloat()

            canvas.drawCircle(
                x,
                y,
                dotRadius * 1.28f,
                haloFill
            )

            canvas.drawCircle(
                x,
                y,
                dotRadius,
                if (i < cellularSignalLevel) foregroundFill else trackFill
            )
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

package com.example.iosstatusbar

import android.annotation.SuppressLint
import android.app.WallpaperManager
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Native Canvas renderer for the iOS Duo style telemetry mark.
 *
 * Visual hierarchy:
 *  - battery percentage text at the top;
 *  - left/right arcs form one battery indicator;
 *  - Wi-Fi is centered inside the ring in Standard mode;
 *  - Camera Hole mode moves Wi-Fi outside the ring and keeps the battery text
 *    and Wi-Fi symmetrically centered around the physical camera opening;
 *  - two cellular dot rows represent SIM 1 (top) and SIM 2 (bottom).
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

    /** SIM 1 / primary subscription, 0..4. */
    var cellularPrimaryLevel: Int = 4
        set(value) { field = value.coerceIn(0, 4); invalidate() }

    /** SIM 2 / secondary subscription, 0..4. */
    var cellularSecondaryLevel: Int = 0
        set(value) { field = value.coerceIn(0, 4); invalidate() }

    var hasSecondarySim: Boolean = false
        set(value) { field = value; invalidate() }

    var isCharging: Boolean = false
        set(value) { field = value; invalidate() }

    /** User-tunable renderer settings. */
    var lineThicknessDp: Float = 2.6f
        set(value) { field = value.coerceIn(1.0f, 4.5f); invalidate() }

    var batteryTextSizeSp: Float = 9.5f
        set(value) { field = value.coerceIn(5.0f, 16.0f); invalidate() }

    /** 400..900. Mapped to available Android weight families. */
    var batteryTextWeight: Int = 800
        set(value) { field = value.coerceIn(400, 900); updateNumberTypeface(); invalidate() }

    /** Wi-Fi can be moved outside the battery ring for a camera-hole layout. */
    var wifiOutsideRing: Boolean = false
        set(value) { field = value; invalidate() }

    /** Signed adjustment to the horizontal separation of text and Wi-Fi. */
    var wifiOutsideOffsetDp: Float = 0f
        set(value) { field = value.coerceIn(-24f, 24f); invalidate() }

    var wifiStrokeDp: Float = 2.1f
        set(value) { field = value.coerceIn(1.0f, 3.8f); invalidate() }

    /** Visual scale of the Wi-Fi glyph only. */
    var wifiIconScale: Float = 1.25f
        set(value) { field = value.coerceIn(0.75f, 1.75f); invalidate() }

    /** Diameter of each cellular dot, not radius. */
    var cellularDotDp: Float = 3.0f
        set(value) { field = value.coerceIn(1.5f, 4.5f); invalidate() }

    private val arcRect = RectF()

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity

    private fun typefaceForWeight(weight: Int): Typeface = when {
        weight >= 850 -> Typeface.create("sans-serif-black", Typeface.NORMAL)
        weight >= 650 -> Typeface.create("sans-serif", Typeface.BOLD)
        weight >= 500 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        else -> Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private fun updateNumberTypeface() {
        val face = typefaceForWeight(batteryTextWeight)
        number.typeface = face
        numberHalo.typeface = face
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
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        isSubpixelText = true
    }

    private val numberHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
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
        val useLightForeground = wallpaperLuma?.let { it < 0.50f } ?: night
        val fg = if (useLightForeground) Color.WHITE else Color.BLACK
        val outline = if (useLightForeground) Color.BLACK else Color.WHITE

        foreground.color = fg
        foregroundFill.color = fg
        number.color = fg
        halo.color = outline
        haloFill.color = outline
        numberHalo.color = outline

        val trackAlpha = 74
        track.color = Color.argb(trackAlpha, Color.red(fg), Color.green(fg), Color.blue(fg))
        trackFill.color = Color.argb(trackAlpha, Color.red(fg), Color.green(fg), Color.blue(fg))
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
            return if (c <= 0.03928f) {
                c / 12.92f
            } else {
                ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
            }
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
        if (size <= 0f) return

        val cx = width / 2f
        val cy = height * 0.56f
        val radius = size * 0.36f

        // Strong, readable line weight that survives small status-bar sizes.
        val stroke = dp(lineThicknessDp)
            .coerceIn(dp(1.0f), dp(4.5f))
        val haloStroke = max(stroke * 1.10f, dp(1.4f))

        // Battery percentage uses actual scaledDensity; the old renderer incorrectly
        // treated sp as dp, which made the number look disproportionately small.
        val textSize = sp(batteryTextSizeSp)
            .coerceIn(sp(5.0f), sp(16.0f))
        updateNumberTypeface()
        number.textSize = textSize
        numberHalo.textSize = textSize
        numberHalo.strokeWidth = max(dp(0.30f), stroke * 0.15f)

        val batteryTextY = cy - radius * 0.90f

        if (wifiOutsideRing) {
            // Camera Hole Mode: battery text and Wi-Fi form one centered horizontal row.
            val halfGap = max(
                radius * 0.92f + dp(wifiOutsideOffsetDp) * 0.5f,
                dp(7f)
            )
            drawTextWithHalo(canvas, batteryPct.toString(), cx - halfGap, batteryTextY)
        } else {
            drawTextWithHalo(canvas, batteryPct.toString(), cx, batteryTextY)
        }

        val batteryRect = RectF(
            cx - radius,
            cy - radius * 0.75f,
            cx + radius,
            cy + radius * 0.96f
        )
        arcRect.set(batteryRect)

        // Left + right arcs are one battery indicator. Both respond to battery %.
        val sideSweep = 79f
        val activeSweep = sideSweep * (batteryPct / 100f)

        drawArcTrack(canvas, 132f, sideSweep, stroke, haloStroke)
        drawArcTrack(canvas, 48f, -sideSweep, stroke, haloStroke)
        drawArcActive(canvas, 132f, activeSweep, stroke, haloStroke)
        drawArcActive(canvas, 48f, -activeSweep, stroke, haloStroke)

        // Wi-Fi layout.
        val wifiStroke = dp(wifiStrokeDp)
            .coerceIn(dp(1.0f), dp(3.8f))

        val wifiCx: Float
        val wifiCy: Float

        if (wifiOutsideRing) {
            val halfGap = max(
                radius * 0.92f + dp(wifiOutsideOffsetDp) * 0.5f,
                dp(7f)
            )
            wifiCx = cx + halfGap
            wifiCy = batteryTextY - dp(0.3f)
        } else {
            // Exact ring-center placement: this is intentionally not cy + 5%.
            wifiCx = cx
            wifiCy = cy
        }

        val scale = wifiIconScale
        val wifiBaseR = radius * (if (wifiOutsideRing) 0.22f else 0.185f) * scale
        val wifiStep = radius * (if (wifiOutsideRing) 0.090f else 0.075f) * scale
        val wifiSweep = 106f

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
                drawArcActive(canvas, 217f, wifiSweep, wifiStroke, wifiStroke * 0.45f)
            } else {
                drawArcTrack(canvas, 217f, wifiSweep, wifiStroke, wifiStroke * 0.45f)
            }
        }

        val wifiDotRadius = max(
            dp(0.9f),
            radius * 0.028f * scale
        )
        val wifiDotY = wifiCy + radius * (if (wifiOutsideRing) 0.34f else 0.28f) * scale

        canvas.drawCircle(wifiCx, wifiDotY, wifiDotRadius * 1.18f, haloFill)
        canvas.drawCircle(
            wifiCx,
            wifiDotY,
            wifiDotRadius,
            if (isWifiConnected && wifiSignalLevel > 0) foregroundFill else trackFill
        )

        // Dual SIM cellular signal: four dots per SIM, vertically separated.
        // SIM 1 = upper row, SIM 2 = lower row.
        val dotDiameter = dp(cellularDotDp).coerceIn(dp(1.5f), dp(4.5f))
        val dotRadius = dotDiameter * 0.5f
        val dotSpacing = max(dotDiameter * 1.55f, radius * 0.22f)
        val topY = cy + radius * 0.55f
        val bottomY = cy + radius * 0.90f

        drawCellularRow(canvas, cx, topY, dotRadius, dotSpacing, cellularPrimaryLevel)

        val secondaryLevelToDraw = if (hasSecondarySim) cellularSecondaryLevel else 0
        drawCellularRow(canvas, cx, bottomY, dotRadius, dotSpacing, secondaryLevelToDraw)
    }

    private fun drawCellularRow(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        dotRadius: Float,
        spacing: Float,
        level: Int
    ) {
        for (i in 0 until 4) {
            val x = cx + (i - 1.5f) * spacing
            canvas.drawCircle(x, cy, dotRadius * 1.18f, haloFill)
            canvas.drawCircle(
                x,
                cy,
                dotRadius,
                if (i < level) foregroundFill else trackFill
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
        haloPadding: Float
    ) {
        if (sweep <= 0f) return
        halo.strokeWidth = max(dp(1.0f), stroke + haloPadding)
        foreground.strokeWidth = stroke
        canvas.drawArc(arcRect, start, sweep, false, halo)
        canvas.drawArc(arcRect, start, sweep, false, foreground)
    }
}

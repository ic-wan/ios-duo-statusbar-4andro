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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Compact, normalized renderer for the iOS Duo-style telemetry mark.
 *
 * The entire composition is built from one normalized ring coordinate system.
 * This is intentional: reducing the overlay size must scale the relationships
 * between battery text, battery arcs, Wi-Fi and dual-SIM dots rather than merely
 * shrinking individual elements until they collide.
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

    /** 0..4. Three visible Wi-Fi arcs plus the center dot. */
    var wifiSignalLevel: Int = 4
        set(value) { field = value.coerceIn(0, 4); invalidate() }

    var cellularPrimaryLevel: Int = 4
        set(value) { field = value.coerceIn(0, 4); invalidate() }

    var cellularSecondaryLevel: Int = 0
        set(value) { field = value.coerceIn(0, 4); invalidate() }

    var hasSecondarySim: Boolean = false
        set(value) { field = value; invalidate() }

    var isCharging: Boolean = false
        set(value) { field = value; invalidate() }

    var lineThicknessDp: Float = 2.6f
        set(value) { field = value.coerceIn(1.0f, 4.5f); invalidate() }

    var batteryTextSizeSp: Float = 9.5f
        set(value) { field = value.coerceIn(5.0f, 16.0f); invalidate() }

    var batteryTextWeight: Int = 800
        set(value) { field = value.coerceIn(400, 900); updateNumberTypeface(); invalidate() }

    var wifiOutsideRing: Boolean = false
        set(value) { field = value; invalidate() }

    var wifiOutsideOffsetDp: Float = 0f
        set(value) { field = value.coerceIn(-24f, 24f); invalidate() }

    var wifiStrokeDp: Float = 2.1f
        set(value) { field = value.coerceIn(1.0f, 3.8f); invalidate() }

    var wifiIconScale: Float = 1.25f
        set(value) { field = value.coerceIn(0.75f, 1.75f); invalidate() }

    var cellularDotDp: Float = 3.0f
        set(value) { field = value.coerceIn(1.5f, 4.5f); invalidate() }

    private val ringRect = RectF()
    private val wifiRect = RectF()

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity

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
        number.isFakeBoldText = batteryTextWeight in 650..849
        numberHalo.isFakeBoldText = batteryTextWeight in 650..849
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

        val trackAlpha = 72
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
            return if (c <= 0.03928f) c / 12.92f
            else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
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

        val viewSize = min(width, height).toFloat()
        if (viewSize <= 0f) return

        // One normalized coordinate system. Every internal element is positioned
        // from the same ring radius, preventing overlap when the whole mark shrinks.
        val cx = width / 2f
        val cy = height * 0.54f
        val radius = viewSize * 0.34f
        ringRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        val scale = (radius / dp(11.56f)).coerceIn(0.70f, 1.70f)

        val arcStroke = max(dp(lineThicknessDp), radius * 0.145f)
            .coerceIn(dp(1.0f), dp(4.5f))
        val haloStroke = max(dp(0.75f), arcStroke * 0.28f)

        updateNumberTypeface()
        val textSize = max(dp(5.5f), sp(batteryTextSizeSp))
            .coerceIn(sp(5.0f), sp(16.0f))
        number.textSize = textSize
        numberHalo.textSize = textSize
        numberHalo.strokeWidth = max(dp(0.25f), dp(0.22f))

        if (wifiOutsideRing) {
            drawCameraHoleHeader(canvas, cx, cy, radius, textSize, scale)
        } else {
            // Battery percentage hugs the top of the ring instead of floating far above it.
            val fm = number.fontMetrics
            val textBaseline = cy - radius - dp(1.5f) - fm.descent
            drawTextWithHalo(canvas, batteryPct.toString(), cx, textBaseline)
        }

        // Left + right arcs are one battery ring. The two sides share the same progress.
        val sideSweep = 76f
        val activeSweep = sideSweep * (batteryPct / 100f)
        drawArcTrack(canvas, 132f, sideSweep, arcStroke, haloStroke)
        drawArcTrack(canvas, 48f, -sideSweep, arcStroke, haloStroke)
        drawArcActive(canvas, 132f, activeSweep, arcStroke, haloStroke)
        drawArcActive(canvas, 48f, -activeSweep, arcStroke, haloStroke)

        // Wi-Fi is built around its center dot, just like a native Wi-Fi glyph.
        // This keeps the dot visually close to the innermost bar at every size.
        val wifiScale = wifiIconScale
        val wifiStroke = max(dp(wifiStrokeDp), radius * 0.105f)
            .coerceIn(dp(1.0f), dp(3.8f))

        val wifiCx: Float
        val wifiCenterY: Float
        if (wifiOutsideRing) {
            val pairHalfSpan = radius * 0.82f + dp(wifiOutsideOffsetDp)
            wifiCx = cx + pairHalfSpan
            wifiCenterY = cy
        } else {
            wifiCx = cx
            wifiCenterY = cy + radius * 0.015f
        }
        drawWifiGlyph(canvas, wifiCx, wifiCenterY, radius, wifiScale, wifiStroke)

        // Two compact cellular rows live inside the lower portion of the same ring.
        // The rows follow a shallow curve, visually tying them back to the ring.
        val dotDiameter = max(dp(cellularDotDp), radius * 0.085f)
            .coerceIn(dp(1.5f), dp(4.5f))
        val dotRadius = dotDiameter / 2f
        val spacing = max(dotDiameter * 1.65f, radius * 0.19f)
        val topBase = cy + radius * 0.52f
        val bottomBase = cy + radius * 0.72f
        val maxX = spacing * 1.5f

        drawCellularRow(canvas, cx, topBase, dotRadius, spacing, maxX, cellularPrimaryLevel)
        drawCellularRow(
            canvas,
            cx,
            bottomBase,
            dotRadius,
            spacing,
            maxX,
            if (hasSecondarySim) cellularSecondaryLevel else 0
        )
    }

    private fun drawCameraHoleHeader(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        textSize: Float,
        scale: Float
    ) {
        // The physical camera is the center anchor. Battery text and Wi-Fi are
        // horizontally mirrored around that anchor and vertically centered to it.
        val pairHalfSpan = radius * 0.82f + dp(wifiOutsideOffsetDp)
        val fm = number.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2f
        drawTextWithHalo(canvas, batteryPct.toString(), cx - pairHalfSpan, baseline)
        // Wi-Fi uses the exact same cy, so the camera sits between the two indicators.
    }

    private fun drawWifiGlyph(
        canvas: Canvas,
        cx: Float,
        centerY: Float,
        radius: Float,
        scale: Float,
        stroke: Float
    ) {
        val inner = radius * 0.095f * scale
        val step = radius * 0.070f * scale
        val sweep = 100f

        for (i in 0..2) {
            val r = inner + i * step
            wifiRect.set(cx - r, centerY - r, cx + r, centerY + r)
            val active = isWifiConnected && i < wifiSignalLevel.coerceIn(0, 3)
            if (active) {
                halo.strokeWidth = max(dp(0.7f), stroke * 0.34f)
                foreground.strokeWidth = stroke
                canvas.drawArc(wifiRect, 220f, sweep, false, halo)
                canvas.drawArc(wifiRect, 220f, sweep, false, foreground)
            } else {
                halo.strokeWidth = max(dp(0.6f), stroke * 0.24f)
                track.strokeWidth = stroke
                canvas.drawArc(wifiRect, 220f, sweep, false, halo)
                canvas.drawArc(wifiRect, 220f, sweep, false, track)
            }
        }

        val dotRadius = max(dp(0.8f), radius * 0.035f * scale)
        val dotPaint = if (isWifiConnected && wifiSignalLevel > 0) foregroundFill else trackFill
        canvas.drawCircle(cx, centerY, dotRadius * 1.10f, haloFill)
        canvas.drawCircle(cx, centerY, dotRadius, dotPaint)
    }

    private fun drawCellularRow(
        canvas: Canvas,
        cx: Float,
        baseY: Float,
        dotRadius: Float,
        spacing: Float,
        maxX: Float,
        level: Int
    ) {
        for (i in 0 until 4) {
            val x = cx + (i - 1.5f) * spacing
            val curve = (abs(x - cx) / maxX).coerceIn(0f, 1f)
            val y = baseY + curve * dotRadius * 1.35f
            canvas.drawCircle(x, y, dotRadius * 1.10f, haloFill)
            canvas.drawCircle(
                x,
                y,
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
        canvas.drawArc(ringRect, start, sweep, false, halo)
        canvas.drawArc(ringRect, start, sweep, false, track)
    }

    private fun drawArcActive(
        canvas: Canvas,
        start: Float,
        sweep: Float,
        stroke: Float,
        haloPadding: Float
    ) {
        if (sweep <= 0f) return
        halo.strokeWidth = max(dp(0.75f), haloPadding)
        foreground.strokeWidth = stroke
        canvas.drawArc(ringRect, start, sweep, false, halo)
        canvas.drawArc(ringRect, start, sweep, false, foreground)
    }
}

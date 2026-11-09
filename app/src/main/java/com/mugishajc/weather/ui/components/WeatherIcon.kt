package com.mugishajc.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mugishajc.weather.domain.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

private val Sun = Color(0xFFFFB300)
private val Moon = Color(0xFFCFD8DC)
private val Cloud = Color(0xFF90A4AE)
private val DarkCloud = Color(0xFF607D8B)
private val Rain = Color(0xFF42A5F5)
private val Snow = Color(0xFFB3E5FC)
private val Bolt = Color(0xFFFFD54F)

/**
 * Small hand-drawn weather glyph. Drawing it on a Canvas keeps the app free of
 * an icon pack and lets it scale to any size.
 */
@Composable
fun WeatherIcon(
    condition: WeatherCondition,
    isDay: Boolean = true,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = condition.label },
    ) {
        when (condition) {
            WeatherCondition.CLEAR -> sky(isDay, full = true)
            WeatherCondition.MAINLY_CLEAR, WeatherCondition.PARTLY_CLOUDY -> {
                sky(isDay, full = false)
                cloud(Cloud, scale = 0.75f, dy = 0.12f)
            }
            WeatherCondition.OVERCAST -> cloud(Cloud)
            WeatherCondition.FOG -> fog()
            WeatherCondition.DRIZZLE -> {
                cloud(Cloud)
                drops(Rain, count = 2)
            }
            WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS, WeatherCondition.FREEZING_RAIN -> {
                cloud(DarkCloud)
                drops(Rain, count = 3)
            }
            WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS -> {
                cloud(Cloud)
                flakes()
            }
            WeatherCondition.THUNDERSTORM -> {
                cloud(DarkCloud)
                bolt()
            }
            WeatherCondition.UNKNOWN -> cloud(Cloud.copy(alpha = 0.5f))
        }
    }
}

private fun DrawScope.sky(isDay: Boolean, full: Boolean) {
    val w = size.minDimension
    val center = if (full) Offset(w / 2, w / 2) else Offset(w * 0.38f, w * 0.36f)
    val radius = if (full) w * 0.22f else w * 0.17f
    if (!isDay) {
        drawCircle(Moon, radius * 1.1f, center)
        return
    }
    drawCircle(Sun, radius, center)
    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val inner = radius * 1.35f
        val outer = radius * 1.75f
        drawLine(
            Sun,
            Offset(center.x + inner * cos(angle).toFloat(), center.y + inner * sin(angle).toFloat()),
            Offset(center.x + outer * cos(angle).toFloat(), center.y + outer * sin(angle).toFloat()),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.cloud(color: Color, scale: Float = 1f, dy: Float = 0f) {
    val w = size.minDimension * scale
    val ox = (size.minDimension - w) / 2 + size.minDimension * (1 - scale) * 0.3f
    val oy = size.minDimension * (0.12f + dy)
    drawCircle(color, w * 0.18f, Offset(ox + w * 0.32f, oy + w * 0.42f))
    drawCircle(color, w * 0.24f, Offset(ox + w * 0.55f, oy + w * 0.34f))
    drawCircle(color, w * 0.16f, Offset(ox + w * 0.76f, oy + w * 0.46f))
    drawRect(color, Offset(ox + w * 0.32f, oy + w * 0.42f), androidx.compose.ui.geometry.Size(w * 0.44f, w * 0.2f))
}

private fun DrawScope.drops(color: Color, count: Int) {
    val w = size.minDimension
    val xs = listOf(0.35f, 0.52f, 0.69f).take(count)
    xs.forEach { x ->
        drawLine(color, Offset(w * x, w * 0.78f), Offset(w * (x - 0.05f), w * 0.92f), w * 0.06f, StrokeCap.Round)
    }
}

private fun DrawScope.flakes() {
    val w = size.minDimension
    listOf(0.35f, 0.52f, 0.69f).forEach { x -> drawCircle(Snow, w * 0.045f, Offset(w * x, w * 0.86f)) }
}

private fun DrawScope.bolt() {
    val w = size.minDimension
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(w * 0.55f, w * 0.68f)
        lineTo(w * 0.44f, w * 0.84f)
        lineTo(w * 0.53f, w * 0.84f)
        lineTo(w * 0.47f, w * 0.98f)
        lineTo(w * 0.64f, w * 0.78f)
        lineTo(w * 0.55f, w * 0.78f)
        close()
    }
    drawPath(path, Bolt)
}

private fun DrawScope.fog() {
    val w = size.minDimension
    listOf(0.35f, 0.5f, 0.65f).forEachIndexed { i, y ->
        val inset = if (i == 1) 0.12f else 0.2f
        drawLine(Cloud, Offset(w * inset, w * y), Offset(w * (1 - inset), w * y), w * 0.07f, StrokeCap.Round)
    }
}

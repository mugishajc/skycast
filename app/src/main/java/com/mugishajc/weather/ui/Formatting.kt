package com.mugishajc.weather.ui

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Whole degrees; avoids showing "-0°" for values like -0.3. */
fun formatTemperature(celsius: Double): String {
    val rounded = celsius.roundToInt()
    return "${if (rounded == 0) 0 else rounded}°"
}

fun formatWind(kmh: Double): String = "${kmh.roundToInt()} km/h"

/** "Today", "Tomorrow", then short weekday names. */
fun dayLabel(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
}

/**
 * Position of a day's range inside the week's overall range, as fractions from
 * 0 to 1. Used to draw the temperature bars in the 7-day list.
 */
fun rangeFractions(min: Double, max: Double, weekMin: Double, weekMax: Double): Pair<Float, Float> {
    val span = weekMax - weekMin
    if (span <= 0.0) return 0f to 1f
    val start = ((min - weekMin) / span).coerceIn(0.0, 1.0).toFloat()
    val end = ((max - weekMin) / span).coerceIn(0.0, 1.0).toFloat()
    return start to maxOf(start, end)
}

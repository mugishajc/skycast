package com.mugishajc.weather.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class FormattingTest {

    @Test
    fun `temperatures round to whole degrees`() {
        assertEquals("19°", formatTemperature(19.4))
        assertEquals("20°", formatTemperature(19.5))
        assertEquals("-3°", formatTemperature(-2.6))
    }

    @Test
    fun `small negatives do not show minus zero`() {
        assertEquals("0°", formatTemperature(-0.3))
    }

    @Test
    fun `wind is rounded and labelled`() {
        assertEquals("7 km/h", formatWind(6.8))
    }

    @Test
    fun `day labels use today and tomorrow then weekday names`() {
        val today = LocalDate.of(2026, 11, 9) // a Monday
        assertEquals("Today", dayLabel(today, today, Locale.ENGLISH))
        assertEquals("Tomorrow", dayLabel(today.plusDays(1), today, Locale.ENGLISH))
        assertEquals("Wed", dayLabel(today.plusDays(2), today, Locale.ENGLISH))
    }

    @Test
    fun `range fractions place the day inside the week`() {
        assertEquals(0.25f to 0.75f, rangeFractions(min = 15.0, max = 25.0, weekMin = 10.0, weekMax = 30.0))
    }

    @Test
    fun `range fractions handle a flat week and out of range values`() {
        assertEquals(0f to 1f, rangeFractions(20.0, 20.0, 20.0, 20.0))
        assertEquals(0f to 1f, rangeFractions(5.0, 40.0, 10.0, 30.0))
    }
}

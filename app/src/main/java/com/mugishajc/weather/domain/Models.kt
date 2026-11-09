package com.mugishajc.weather.domain

import java.time.LocalDate

data class City(
    val id: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
) {
    /** "Kigali, Rwanda" or "Springfield, Illinois, United States" style label. */
    val displayName: String
        get() = listOfNotNull(name, region?.takeIf { it != name }, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
}

data class CurrentWeather(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val windKmh: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
)

data class DailyForecast(
    val date: LocalDate,
    val condition: WeatherCondition,
    val maxC: Double,
    val minC: Double,
    val precipitationChance: Int?,
)

data class Forecast(
    val city: City,
    val timezone: String,
    val current: CurrentWeather,
    val days: List<DailyForecast>,
)

package com.mugishajc.weather.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null,
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
)

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    val timezone: String,
    val current: CurrentDto,
    val daily: DailyDto,
)

@JsonClass(generateAdapter = true)
data class CurrentDto(
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "relative_humidity_2m") val humidity: Int,
    @Json(name = "wind_speed_10m") val windSpeed: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "is_day") val isDay: Int,
)

/** Open-Meteo returns daily values as parallel arrays indexed by day. */
@JsonClass(generateAdapter = true)
data class DailyDto(
    val time: List<String>,
    @Json(name = "weather_code") val weatherCode: List<Int?>,
    @Json(name = "temperature_2m_max") val maxTemperature: List<Double?>,
    @Json(name = "temperature_2m_min") val minTemperature: List<Double?>,
    @Json(name = "precipitation_probability_max") val precipitationProbability: List<Int?>? = null,
)

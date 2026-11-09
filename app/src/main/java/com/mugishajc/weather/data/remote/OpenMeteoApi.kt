package com.mugishajc.weather.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** https://geocoding-api.open-meteo.com/ */
interface GeocodingApi {

    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 8,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponse
}

/** https://api.open-meteo.com/ */
interface ForecastApi {

    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 7,
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
    ): ForecastResponse

    companion object {
        const val CURRENT_FIELDS =
            "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day"
        const val DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
    }
}

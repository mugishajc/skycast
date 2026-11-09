package com.mugishajc.weather.data.remote

import com.mugishajc.weather.domain.City
import com.mugishajc.weather.domain.CurrentWeather
import com.mugishajc.weather.domain.DailyForecast
import com.mugishajc.weather.domain.Forecast
import com.mugishajc.weather.domain.WeatherCondition
import java.time.LocalDate

fun GeocodingResult.toCity() = City(
    id = id,
    name = name,
    region = admin1,
    country = country,
    latitude = latitude,
    longitude = longitude,
)

fun ForecastResponse.toForecast(city: City) = Forecast(
    city = city,
    timezone = timezone,
    current = CurrentWeather(
        temperatureC = current.temperature,
        feelsLikeC = current.apparentTemperature,
        humidityPercent = current.humidity,
        windKmh = current.windSpeed,
        condition = WeatherCondition.fromWmoCode(current.weatherCode),
        isDay = current.isDay == 1,
    ),
    days = daily.toDays(),
)

/**
 * Zips the parallel daily arrays into one object per day. Days with a missing
 * min or max temperature are dropped rather than shown as zero.
 */
internal fun DailyDto.toDays(): List<DailyForecast> = time.indices.mapNotNull { i ->
    val max = maxTemperature.getOrNull(i) ?: return@mapNotNull null
    val min = minTemperature.getOrNull(i) ?: return@mapNotNull null
    DailyForecast(
        date = LocalDate.parse(time[i]),
        condition = WeatherCondition.fromWmoCode(weatherCode.getOrNull(i)),
        maxC = max,
        minC = min,
        precipitationChance = precipitationProbability?.getOrNull(i),
    )
}

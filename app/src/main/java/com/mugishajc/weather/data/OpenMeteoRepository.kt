package com.mugishajc.weather.data

import com.mugishajc.weather.data.remote.ForecastApi
import com.mugishajc.weather.data.remote.GeocodingApi
import com.mugishajc.weather.data.remote.toCity
import com.mugishajc.weather.data.remote.toForecast
import com.mugishajc.weather.domain.City
import com.mugishajc.weather.domain.Forecast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenMeteoRepository(
    private val geocodingApi: GeocodingApi,
    private val forecastApi: ForecastApi,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : WeatherRepository {

    override suspend fun searchCities(query: String): List<City> {
        val trimmed = query.trim()
        // The geocoding API needs at least two characters to return anything useful.
        if (trimmed.length < MIN_QUERY_LENGTH) return emptyList()
        return withContext(io) {
            geocodingApi.search(trimmed).results.orEmpty().map { it.toCity() }
        }
    }

    override suspend fun forecast(city: City): Forecast = withContext(io) {
        forecastApi.forecast(city.latitude, city.longitude).toForecast(city)
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}

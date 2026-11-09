package com.mugishajc.weather.data

import com.mugishajc.weather.domain.City
import com.mugishajc.weather.domain.Forecast

interface WeatherRepository {

    /** Cities matching [query]. Empty when nothing matches. */
    suspend fun searchCities(query: String): List<City>

    suspend fun forecast(city: City): Forecast
}

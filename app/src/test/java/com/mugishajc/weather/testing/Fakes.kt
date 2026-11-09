package com.mugishajc.weather.testing

import com.mugishajc.weather.data.WeatherRepository
import com.mugishajc.weather.data.local.SavedCities
import com.mugishajc.weather.data.local.SavedCitiesStore
import com.mugishajc.weather.domain.City
import com.mugishajc.weather.domain.CurrentWeather
import com.mugishajc.weather.domain.DailyForecast
import com.mugishajc.weather.domain.Forecast
import com.mugishajc.weather.domain.WeatherCondition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

val KIGALI = City(1, "Kigali", null, "Rwanda", -1.95, 30.06)
val NAIROBI = City(2, "Nairobi", "Nairobi County", "Kenya", -1.29, 36.82)

fun forecastFor(city: City, temperature: Double = 21.0) = Forecast(
    city = city,
    timezone = "Africa/Kigali",
    current = CurrentWeather(temperature, temperature - 1, 60, 8.0, WeatherCondition.PARTLY_CLOUDY, isDay = true),
    days = List(7) { i ->
        DailyForecast(LocalDate.of(2026, 11, 9).plusDays(i.toLong()), WeatherCondition.RAIN, 25.0, 15.0, 40)
    },
)

class FakeWeatherRepository : WeatherRepository {

    val searchQueries = mutableListOf<String>()
    var searchResults: List<City> = emptyList()
    var searchError: Exception? = null

    val forecastCalls = mutableListOf<City>()
    var forecastError: Exception? = null
    var temperature = 21.0

    /** When set, forecast() waits on it, which lets tests look at the loading state. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun searchCities(query: String): List<City> {
        searchQueries += query
        searchError?.let { throw it }
        return searchResults
    }

    override suspend fun forecast(city: City): Forecast {
        forecastCalls += city
        gate?.await()
        forecastError?.let { throw it }
        return forecastFor(city, temperature)
    }
}

class FakeSavedCitiesStore(initial: SavedCities = SavedCities()) : SavedCitiesStore {

    private val state = MutableStateFlow(initial)
    override val saved: Flow<SavedCities> = state

    override suspend fun addAndSelect(city: City) = state.update { current ->
        val cities = if (current.cities.any { it.id == city.id }) current.cities else current.cities + city
        current.copy(cities = cities, selectedId = city.id)
    }

    override suspend fun select(cityId: Long) = state.update { it.copy(selectedId = cityId) }

    override suspend fun remove(cityId: Long) = state.update { current ->
        val remaining = current.cities.filterNot { it.id == cityId }
        val selected = if (current.selectedId == cityId) remaining.firstOrNull()?.id else current.selectedId
        SavedCities(remaining, selected)
    }
}

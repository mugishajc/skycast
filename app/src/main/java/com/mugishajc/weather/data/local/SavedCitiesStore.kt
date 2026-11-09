package com.mugishajc.weather.data.local

import com.mugishajc.weather.domain.City
import kotlinx.coroutines.flow.Flow

/** The user's saved cities plus which one is currently selected. */
data class SavedCities(
    val cities: List<City> = emptyList(),
    val selectedId: Long? = null,
) {
    val selected: City?
        get() = cities.firstOrNull { it.id == selectedId } ?: cities.firstOrNull()
}

interface SavedCitiesStore {

    val saved: Flow<SavedCities>

    /** Adds the city if it is new and makes it the selected one. */
    suspend fun addAndSelect(city: City)

    suspend fun select(cityId: Long)

    suspend fun remove(cityId: Long)
}

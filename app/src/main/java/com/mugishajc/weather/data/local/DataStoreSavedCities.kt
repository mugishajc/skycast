package com.mugishajc.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mugishajc.weather.domain.City
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores saved cities as a small JSON array in Preferences DataStore. The list
 * is short (a handful of cities), so a single string value is simpler than a
 * proto schema or a database.
 */
class DataStoreSavedCities(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi = Moshi.Builder().build(),
) : SavedCitiesStore {

    private val adapter = moshi.adapter<List<StoredCity>>(
        Types.newParameterizedType(List::class.java, StoredCity::class.java),
    )

    override val saved: Flow<SavedCities> = dataStore.data.map { prefs ->
        SavedCities(
            cities = decode(prefs[KEY_CITIES]),
            selectedId = prefs[KEY_SELECTED],
        )
    }

    override suspend fun addAndSelect(city: City) {
        dataStore.edit { prefs ->
            val cities = decode(prefs[KEY_CITIES])
            if (cities.none { it.id == city.id }) {
                prefs[KEY_CITIES] = encode(cities + city)
            }
            prefs[KEY_SELECTED] = city.id
        }
    }

    override suspend fun select(cityId: Long) {
        dataStore.edit { it[KEY_SELECTED] = cityId }
    }

    override suspend fun remove(cityId: Long) {
        dataStore.edit { prefs ->
            val remaining = decode(prefs[KEY_CITIES]).filterNot { it.id == cityId }
            prefs[KEY_CITIES] = encode(remaining)
            if (prefs[KEY_SELECTED] == cityId) {
                val next = remaining.firstOrNull()
                if (next == null) prefs.remove(KEY_SELECTED) else prefs[KEY_SELECTED] = next.id
            }
        }
    }

    private fun decode(json: String?): List<City> {
        if (json.isNullOrBlank()) return emptyList()
        // A corrupt value should not crash the app; start over with an empty list.
        val stored = runCatching { adapter.fromJson(json) }.getOrNull() ?: return emptyList()
        return stored.map { it.toCity() }
    }

    private fun encode(cities: List<City>): String = adapter.toJson(cities.map { StoredCity.from(it) })

    @JsonClass(generateAdapter = true)
    internal data class StoredCity(
        val id: Long,
        val name: String,
        val region: String?,
        val country: String?,
        val latitude: Double,
        val longitude: Double,
    ) {
        fun toCity() = City(id, name, region, country, latitude, longitude)

        companion object {
            fun from(city: City) =
                StoredCity(city.id, city.name, city.region, city.country, city.latitude, city.longitude)
        }
    }

    private companion object {
        val KEY_CITIES = stringPreferencesKey("saved_cities")
        val KEY_SELECTED = longPreferencesKey("selected_city_id")
    }
}

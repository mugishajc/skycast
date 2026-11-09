package com.mugishajc.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mugishajc.weather.domain.City
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSavedCitiesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val kigali = City(1, "Kigali", null, "Rwanda", -1.95, 30.06)
    private val kampala = City(2, "Kampala", "Central Region", "Uganda", 0.31, 32.58)

    // DataStore runs in backgroundScope, which runTest cancels when the test ends.
    private fun TestScope.newStore(): Pair<DataStoreSavedCities, DataStore<Preferences>> {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tmp.newFile("cities.preferences_pb").also { it.delete() } },
        )
        return DataStoreSavedCities(dataStore) to dataStore
    }

    @Test
    fun `starts empty`() = runTest {
        val (store, _) = newStore()
        val saved = store.saved.first()

        assertTrue(saved.cities.isEmpty())
        assertNull(saved.selected)
    }

    @Test
    fun `adding cities keeps order and selects the newest`() = runTest {
        val (store, _) = newStore()
        store.addAndSelect(kigali)
        store.addAndSelect(kampala)

        val saved = store.saved.first()
        assertEquals(listOf(kigali, kampala), saved.cities)
        assertEquals(kampala, saved.selected)
    }

    @Test
    fun `adding an existing city only selects it`() = runTest {
        val (store, _) = newStore()
        store.addAndSelect(kigali)
        store.addAndSelect(kampala)
        store.addAndSelect(kigali)

        val saved = store.saved.first()
        assertEquals(2, saved.cities.size)
        assertEquals(kigali, saved.selected)
    }

    @Test
    fun `removing the selected city falls back to the first remaining`() = runTest {
        val (store, _) = newStore()
        store.addAndSelect(kigali)
        store.addAndSelect(kampala)

        store.remove(kampala.id)
        assertEquals(kigali, store.saved.first().selected)

        store.remove(kigali.id)
        val saved = store.saved.first()
        assertTrue(saved.cities.isEmpty())
        assertNull(saved.selectedId)
    }

    @Test
    fun `corrupt json is treated as no saved cities`() = runTest {
        val (store, dataStore) = newStore()
        dataStore.edit { it[stringPreferencesKey("saved_cities")] = "{not json" }

        assertTrue(store.saved.first().cities.isEmpty())
    }
}

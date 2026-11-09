package com.mugishajc.weather.ui

import com.mugishajc.weather.data.local.SavedCities
import com.mugishajc.weather.testing.FakeSavedCitiesStore
import com.mugishajc.weather.testing.FakeWeatherRepository
import com.mugishajc.weather.testing.KIGALI
import com.mugishajc.weather.testing.MainDispatcherRule
import com.mugishajc.weather.testing.NAIROBI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeWeatherRepository()

    private fun viewModel(saved: SavedCities = SavedCities()) =
        WeatherViewModel(repository, FakeSavedCitiesStore(saved))

    @Test
    fun `no saved cities shows the empty state`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(WeatherUiState.NoCitySelected, vm.weather.value)
        assertTrue(repository.forecastCalls.isEmpty())
    }

    @Test
    fun `selected city loads on start`() = runTest(mainRule.dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        val vm = viewModel(SavedCities(listOf(KIGALI, NAIROBI), selectedId = NAIROBI.id))
        runCurrent()

        assertEquals(WeatherUiState.Loading(NAIROBI), vm.weather.value)

        gate.complete(Unit)
        advanceUntilIdle()
        val state = vm.weather.value as WeatherUiState.Success
        assertEquals(NAIROBI, state.forecast.city)
        assertEquals(7, state.forecast.days.size)
    }

    @Test
    fun `picking a search result saves it and loads its forecast`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(SavedCities(listOf(KIGALI), KIGALI.id))
        advanceUntilIdle()

        vm.onCityPicked(NAIROBI)
        advanceUntilIdle()

        assertEquals(listOf(KIGALI, NAIROBI), vm.savedCities.value.cities)
        assertEquals(NAIROBI, (vm.weather.value as WeatherUiState.Success).forecast.city)
        assertEquals(SearchUiState(), vm.search.value)
    }

    @Test
    fun `network failure becomes an error state and retry recovers`() = runTest(mainRule.dispatcher) {
        repository.forecastError = IOException("offline")
        val vm = viewModel(SavedCities(listOf(KIGALI), KIGALI.id))
        advanceUntilIdle()

        val error = vm.weather.value as WeatherUiState.Error
        assertEquals(KIGALI, error.city)
        assertTrue(error.message.contains("connection"))

        repository.forecastError = null
        vm.refresh()
        advanceUntilIdle()
        assertTrue(vm.weather.value is WeatherUiState.Success)
    }

    @Test
    fun `http errors mention the status code`() = runTest(mainRule.dispatcher) {
        repository.forecastError = HttpException(Response.error<Any>(502, "".toResponseBody()))
        val vm = viewModel(SavedCities(listOf(KIGALI), KIGALI.id))
        advanceUntilIdle()

        assertTrue((vm.weather.value as WeatherUiState.Error).message.contains("502"))
    }

    @Test
    fun `refresh keeps old data visible while loading`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(SavedCities(listOf(KIGALI), KIGALI.id))
        advanceUntilIdle()

        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        repository.temperature = 25.0
        vm.refresh()
        runCurrent()

        val refreshing = vm.weather.value as WeatherUiState.Success
        assertTrue(refreshing.isRefreshing)
        assertEquals(21.0, refreshing.forecast.current.temperatureC, 0.0)

        gate.complete(Unit)
        advanceUntilIdle()
        val done = vm.weather.value as WeatherUiState.Success
        assertFalse(done.isRefreshing)
        assertEquals(25.0, done.forecast.current.temperatureC, 0.0)
    }

    @Test
    fun `search is debounced and only the last query is sent`() = runTest(mainRule.dispatcher) {
        repository.searchResults = listOf(KIGALI)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onQueryChange("K")
        vm.onQueryChange("Ki")
        vm.onQueryChange("Kig")
        advanceTimeBy(WeatherViewModel.SEARCH_DEBOUNCE_MS - 50)
        runCurrent()
        assertTrue(repository.searchQueries.isEmpty())

        advanceUntilIdle()
        assertEquals(listOf("Kig"), repository.searchQueries)
        assertEquals(listOf(KIGALI), vm.search.value.results)
        assertEquals("Kig", vm.search.value.query)
        assertFalse(vm.search.value.isSearching)
    }

    @Test
    fun `one letter queries are not sent`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.onQueryChange("K")
        advanceUntilIdle()

        assertTrue(repository.searchQueries.isEmpty())
        assertTrue(vm.search.value.results.isEmpty())
    }

    @Test
    fun `search failure is shown and cleared on the next edit`() = runTest(mainRule.dispatcher) {
        repository.searchError = IOException()
        val vm = viewModel()
        vm.onQueryChange("Kampala")
        advanceUntilIdle()

        assertTrue(vm.search.value.error != null)

        vm.onQueryChange("Kampal")
        assertEquals(null, vm.search.value.error)
    }

    @Test
    fun `removing the last city returns to the empty state`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(SavedCities(listOf(KIGALI), KIGALI.id))
        advanceUntilIdle()

        vm.onRemoveCity(KIGALI)
        advanceUntilIdle()

        assertEquals(WeatherUiState.NoCitySelected, vm.weather.value)
        assertTrue(vm.savedCities.value.cities.isEmpty())
    }

    @Test
    fun `switching saved city loads the new one`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(SavedCities(listOf(KIGALI, NAIROBI), KIGALI.id))
        advanceUntilIdle()

        vm.onSavedCitySelected(NAIROBI)
        advanceUntilIdle()

        assertEquals(listOf(KIGALI, NAIROBI), repository.forecastCalls)
        assertEquals(NAIROBI, (vm.weather.value as WeatherUiState.Success).forecast.city)
    }
}

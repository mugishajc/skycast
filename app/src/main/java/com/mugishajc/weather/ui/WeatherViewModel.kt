package com.mugishajc.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mugishajc.weather.WeatherApplication
import com.mugishajc.weather.data.WeatherRepository
import com.mugishajc.weather.data.local.SavedCities
import com.mugishajc.weather.data.local.SavedCitiesStore
import com.mugishajc.weather.domain.City
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@OptIn(FlowPreview::class)
class WeatherViewModel(
    private val repository: WeatherRepository,
    private val store: SavedCitiesStore,
) : ViewModel() {

    private val _weather = MutableStateFlow<WeatherUiState>(WeatherUiState.NoCitySelected)
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    val savedCities: StateFlow<SavedCities> =
        store.saved.stateIn(viewModelScope, SharingStarted.Eagerly, SavedCities())

    private val queries = MutableStateFlow("")
    private var loadJob: Job? = null

    init {
        // Reload whenever the selected city changes, including on first launch.
        viewModelScope.launch {
            store.saved
                .map { it.selected }
                .distinctUntilChanged()
                .collect { city ->
                    if (city == null) {
                        loadJob?.cancel()
                        _weather.value = WeatherUiState.NoCitySelected
                    } else {
                        load(city, refreshing = false)
                    }
                }
        }

        viewModelScope.launch {
            queries
                .debounce(SEARCH_DEBOUNCE_MS)
                .map { it.trim() }
                .distinctUntilChanged()
                .collectLatest { runSearch(it) }
        }
    }

    fun onQueryChange(query: String) {
        _search.update { it.copy(query = query, error = null) }
        queries.value = query
    }

    fun clearSearch() {
        _search.value = SearchUiState()
        queries.value = ""
    }

    fun onCityPicked(city: City) {
        clearSearch()
        viewModelScope.launch { store.addAndSelect(city) }
    }

    fun onSavedCitySelected(city: City) {
        viewModelScope.launch { store.select(city.id) }
    }

    fun onRemoveCity(city: City) {
        viewModelScope.launch { store.remove(city.id) }
    }

    fun refresh() {
        val city = when (val state = _weather.value) {
            is WeatherUiState.Success -> state.forecast.city
            is WeatherUiState.Error -> state.city
            is WeatherUiState.Loading -> return
            WeatherUiState.NoCitySelected -> return
        }
        load(city, refreshing = true)
    }

    private fun load(city: City, refreshing: Boolean) {
        loadJob?.cancel()
        val current = _weather.value
        _weather.value = if (refreshing && current is WeatherUiState.Success) {
            // Keep the old numbers on screen while refreshing.
            current.copy(isRefreshing = true)
        } else {
            WeatherUiState.Loading(city)
        }
        loadJob = viewModelScope.launch {
            _weather.value = try {
                WeatherUiState.Success(repository.forecast(city))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                WeatherUiState.Error(city, e.toUserMessage())
            }
        }
    }

    private suspend fun runSearch(query: String) {
        if (query.length < MIN_QUERY_LENGTH) {
            _search.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        _search.update { it.copy(isSearching = true, error = null) }
        try {
            val results = repository.searchCities(query)
            _search.update { it.copy(results = results, isSearching = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _search.update { it.copy(results = emptyList(), isSearching = false, error = e.toUserMessage()) }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
        private const val MIN_QUERY_LENGTH = 2

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WeatherApplication).container
                WeatherViewModel(container.weatherRepository, container.savedCitiesStore)
            }
        }
    }
}

package com.mugishajc.weather.ui

import com.mugishajc.weather.domain.City
import com.mugishajc.weather.domain.Forecast

sealed interface WeatherUiState {

    /** First launch, nothing saved yet. */
    data object NoCitySelected : WeatherUiState

    data class Loading(val city: City) : WeatherUiState

    data class Success(
        val forecast: Forecast,
        val isRefreshing: Boolean = false,
    ) : WeatherUiState

    data class Error(val city: City, val message: String) : WeatherUiState
}

data class SearchUiState(
    val query: String = "",
    val results: List<City> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
) {
    val showResults: Boolean
        get() = query.isNotBlank()
}

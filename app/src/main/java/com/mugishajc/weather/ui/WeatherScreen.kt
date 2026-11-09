package com.mugishajc.weather.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mugishajc.weather.data.local.SavedCities
import com.mugishajc.weather.domain.City
import com.mugishajc.weather.domain.CurrentWeather
import com.mugishajc.weather.domain.DailyForecast
import com.mugishajc.weather.domain.Forecast
import com.mugishajc.weather.domain.WeatherCondition
import com.mugishajc.weather.ui.components.WeatherIcon
import com.mugishajc.weather.ui.theme.WeatherTheme
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val saved by viewModel.savedCities.collectAsStateWithLifecycle()

    WeatherContent(
        weather = weather,
        search = search,
        saved = saved,
        actions = WeatherActions(
            onQueryChange = viewModel::onQueryChange,
            onClearSearch = viewModel::clearSearch,
            onCityPicked = viewModel::onCityPicked,
            onSavedCitySelected = viewModel::onSavedCitySelected,
            onRemoveCity = viewModel::onRemoveCity,
            onRefresh = viewModel::refresh,
        ),
    )
}

/** Groups the screen callbacks so the composables below stay readable. */
data class WeatherActions(
    val onQueryChange: (String) -> Unit = {},
    val onClearSearch: () -> Unit = {},
    val onCityPicked: (City) -> Unit = {},
    val onSavedCitySelected: (City) -> Unit = {},
    val onRemoveCity: (City) -> Unit = {},
    val onRefresh: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherContent(
    weather: WeatherUiState,
    search: SearchUiState,
    saved: SavedCities,
    actions: WeatherActions,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather") },
                actions = {
                    if (weather is WeatherUiState.Success || weather is WeatherUiState.Error) {
                        IconButton(onClick = actions.onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            SearchField(search.query, actions.onQueryChange, actions.onClearSearch)

            if (search.showResults) {
                SearchResults(search, actions.onCityPicked)
                return@Column
            }

            if (saved.cities.isNotEmpty()) {
                SavedCitiesRow(saved, actions.onSavedCitySelected, actions.onRemoveCity)
            }

            when (weather) {
                WeatherUiState.NoCitySelected -> Message(
                    title = "No city yet",
                    body = "Search for a city above to see its current weather and the week ahead.",
                )
                is WeatherUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is WeatherUiState.Error -> Message(
                    title = weather.city.name,
                    body = weather.message,
                    actionLabel = "Try again",
                    onAction = actions.onRefresh,
                )
                is WeatherUiState.Success -> ForecastList(weather)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { Text("Search for a city") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
private fun SearchResults(search: SearchUiState, onCityPicked: (City) -> Unit) {
    when {
        search.isSearching -> LinearProgressIndicator(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        search.error != null -> Message(title = "Search failed", body = search.error)
        search.results.isEmpty() && search.query.trim().length >= 2 ->
            Message(title = "No matches", body = "Check the spelling or try a nearby larger town.")
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(search.results, key = { it.id }) { city ->
            ListItem(
                headlineContent = { Text(city.name) },
                supportingContent = { Text(city.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.clickable { onCityPicked(city) },
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedCitiesRow(saved: SavedCities, onSelect: (City) -> Unit, onRemove: (City) -> Unit) {
    val selectedId = saved.selected?.id
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(saved.cities, key = { it.id }) { city ->
            InputChip(
                selected = city.id == selectedId,
                onClick = { onSelect(city) },
                label = { Text(city.name) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove ${city.name}",
                        modifier = Modifier
                            .size(InputChipDefaults.AvatarSize)
                            .clickable { onRemove(city) },
                    )
                },
            )
        }
    }
}

@Composable
private fun ForecastList(state: WeatherUiState.Success) {
    val forecast = state.forecast
    val today = remember(forecast.timezone) {
        val zone = runCatching { ZoneId.of(forecast.timezone) }.getOrDefault(ZoneId.systemDefault())
        LocalDate.now(zone)
    }
    val weekMin = forecast.days.minOfOrNull { it.minC } ?: 0.0
    val weekMax = forecast.days.maxOfOrNull { it.maxC } ?: 0.0

    Column {
        if (state.isRefreshing) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CurrentCard(forecast.city, forecast.current) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(
                            "7-day forecast",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        forecast.days.forEach { day ->
                            DayRow(day, today, weekMin, weekMax)
                        }
                    }
                }
            }
            item {
                Text(
                    "Weather data by Open-Meteo.com",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CurrentCard(city: City, current: CurrentWeather) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(city.displayName, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTemperature(current.temperatureC), style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.weight(1f))
                WeatherIcon(current.condition, current.isDay, size = 88.dp)
            }
            Text(current.condition.label, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Feels like", formatTemperature(current.feelsLikeC))
                Stat("Humidity", "${current.humidityPercent}%")
                Stat("Wind", formatWind(current.windKmh))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DayRow(day: DailyForecast, today: LocalDate, weekMin: Double, weekMax: Double) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(dayLabel(day.date, today), Modifier.width(84.dp), style = MaterialTheme.typography.bodyLarge)
        WeatherIcon(day.condition, size = 28.dp)
        Text(
            text = day.precipitationChance?.takeIf { it > 0 }?.let { "$it%" } ?: "",
            modifier = Modifier.width(44.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
        )
        Text(
            formatTemperature(day.minC),
            Modifier.width(40.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RangeBar(day, weekMin, weekMax, Modifier.weight(1f).padding(horizontal = 8.dp))
        Text(formatTemperature(day.maxC), Modifier.width(36.dp))
    }
}

@Composable
private fun RangeBar(day: DailyForecast, weekMin: Double, weekMax: Double, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.secondary
    val (start, end) = rangeFractions(day.minC, day.maxC, weekMin, weekMax)
    Canvas(modifier.height(6.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        drawRoundRect(
            fill,
            topLeft = Offset(size.width * start, 0f),
            size = Size((size.width * (end - start)).coerceAtLeast(size.height), size.height),
            cornerRadius = radius,
        )
    }
}

@Composable
private fun Message(title: String, body: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeatherContentPreview() {
    val city = City(1, "Kigali", null, "Rwanda", -1.95, 30.06)
    val days = WeatherCondition.entries.take(7).mapIndexed { i, condition ->
        DailyForecast(LocalDate.now().plusDays(i.toLong()), condition, 24.0 + i % 3, 15.0 + i % 2, i * 10)
    }
    WeatherTheme(dynamicColor = false) {
        WeatherContent(
            weather = WeatherUiState.Success(
                Forecast(city, "Africa/Kigali", CurrentWeather(21.4, 20.8, 64, 9.0, WeatherCondition.PARTLY_CLOUDY, true), days),
            ),
            search = SearchUiState(),
            saved = SavedCities(listOf(city), city.id),
            actions = WeatherActions(),
        )
    }
}

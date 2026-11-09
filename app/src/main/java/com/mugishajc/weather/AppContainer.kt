package com.mugishajc.weather

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.mugishajc.weather.data.OpenMeteoRepository
import com.mugishajc.weather.data.WeatherRepository
import com.mugishajc.weather.data.local.DataStoreSavedCities
import com.mugishajc.weather.data.local.SavedCitiesStore
import com.mugishajc.weather.data.remote.ForecastApi
import com.mugishajc.weather.data.remote.GeocodingApi
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private val Context.savedCitiesDataStore by preferencesDataStore(name = "saved_cities")

/**
 * Manual dependency wiring. The graph is small: one HTTP client, two Retrofit
 * services and a DataStore, so a DI framework would add more than it saves.
 */
class AppContainer(context: Context) {

    private val moshi = Moshi.Builder().build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    private fun retrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val weatherRepository: WeatherRepository = OpenMeteoRepository(
        geocodingApi = retrofit(GEOCODING_URL).create(GeocodingApi::class.java),
        forecastApi = retrofit(FORECAST_URL).create(ForecastApi::class.java),
    )

    val savedCitiesStore: SavedCitiesStore =
        DataStoreSavedCities(context.applicationContext.savedCitiesDataStore, moshi)

    private companion object {
        const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/"
        const val FORECAST_URL = "https://api.open-meteo.com/"
    }
}

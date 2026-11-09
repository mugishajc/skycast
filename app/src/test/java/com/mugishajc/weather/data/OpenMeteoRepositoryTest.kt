package com.mugishajc.weather.data

import com.mugishajc.weather.data.remote.ForecastApi
import com.mugishajc.weather.data.remote.GeocodingApi
import com.mugishajc.weather.domain.City
import com.mugishajc.weather.domain.WeatherCondition
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDate

class OpenMeteoRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: OpenMeteoRepository

    private val kigali = City(202061, "Kigali", "Kigali", "Rwanda", -1.94995, 30.05885)

    @Before
    fun setUp() {
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
        repository = OpenMeteoRepository(
            geocodingApi = retrofit.create(GeocodingApi::class.java),
            forecastApi = retrofit.create(ForecastApi::class.java),
            io = UnconfinedTestDispatcher(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)) { "missing fixture $name" }.readText()

    @Test
    fun `search sends the trimmed name and maps results`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("geocoding_kigali.json")))

        val cities = repository.searchCities("  Kigali ")

        val request = server.takeRequest()
        assertEquals("/v1/search", request.requestUrl?.encodedPath)
        assertEquals("Kigali", request.requestUrl?.queryParameter("name"))
        assertEquals(2, cities.size)
        assertEquals(kigali, cities[0])
        assertNull(cities[1].region)
        assertEquals("Kigali Hill, Rwanda", cities[1].displayName)
    }

    @Test
    fun `search with no matches returns an empty list`() = runTest {
        // Open-Meteo leaves out "results" entirely when nothing matches.
        server.enqueue(MockResponse().setBody("""{"generationtime_ms":0.3}"""))

        assertTrue(repository.searchCities("zzzzqx").isEmpty())
    }

    @Test
    fun `very short queries do not hit the network`() = runTest {
        assertTrue(repository.searchCities(" k ").isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `forecast requests the expected fields and maps the response`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("forecast_kigali.json")))

        val forecast = repository.forecast(kigali)

        val url = server.takeRequest().requestUrl!!
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals("-1.94995", url.queryParameter("latitude"))
        assertEquals("30.05885", url.queryParameter("longitude"))
        assertEquals("auto", url.queryParameter("timezone"))
        assertEquals("7", url.queryParameter("forecast_days"))
        assertEquals(ForecastApi.DAILY_FIELDS, url.queryParameter("daily"))

        assertEquals("Africa/Kigali", forecast.timezone)
        with(forecast.current) {
            assertEquals(19.4, temperatureC, 0.001)
            assertEquals(18.9, feelsLikeC, 0.001)
            assertEquals(78, humidityPercent)
            assertEquals(WeatherCondition.PARTLY_CLOUDY, condition)
            assertTrue(isDay)
        }

        // The last day has no minimum temperature, so it is dropped.
        assertEquals(6, forecast.days.size)
        with(forecast.days[1]) {
            assertEquals(LocalDate.of(2026, 11, 10), date)
            assertEquals(WeatherCondition.RAIN, condition)
            assertEquals(24.3, maxC, 0.001)
            assertEquals(75, precipitationChance)
        }
        assertEquals(WeatherCondition.THUNDERSTORM, forecast.days[3].condition)
    }

    @Test(expected = HttpException::class)
    fun `server errors surface as HttpException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        repository.forecast(kigali)
    }

    @Test
    fun `night time is reported`() = runTest {
        val night = fixture("forecast_kigali.json").replace("\"is_day\": 1", "\"is_day\": 0")
        server.enqueue(MockResponse().setBody(night))

        assertFalse(repository.forecast(kigali).current.isDay)
    }
}

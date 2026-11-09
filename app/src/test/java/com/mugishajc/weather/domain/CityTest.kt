package com.mugishajc.weather.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CityTest {

    private fun city(name: String, region: String?, country: String?) =
        City(1, name, region, country, 0.0, 0.0)

    @Test
    fun `display name joins the available parts`() {
        assertEquals("Huye, Southern Province, Rwanda", city("Huye", "Southern Province", "Rwanda").displayName)
        assertEquals("Nairobi, Kenya", city("Nairobi", null, "Kenya").displayName)
    }

    @Test
    fun `region equal to the name is not repeated`() {
        assertEquals("Kigali, Rwanda", city("Kigali", "Kigali", "Rwanda").displayName)
    }

    @Test
    fun `blank parts are skipped`() {
        assertEquals("Atlantis", city("Atlantis", "", " ").displayName)
    }
}

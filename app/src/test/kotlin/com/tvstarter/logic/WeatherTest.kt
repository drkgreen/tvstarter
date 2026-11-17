package com.tvstarter.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherTest {
    @Test
    fun kodlarDogruTureCevrilir() {
        assertEquals(WeatherKind.CLEAR, WeatherCodes.describe(0).kind)
        assertEquals(WeatherKind.PARTLY, WeatherCodes.describe(2).kind)
        assertEquals(WeatherKind.FOG, WeatherCodes.describe(45).kind)
        assertEquals(WeatherKind.RAIN, WeatherCodes.describe(63).kind)
        assertEquals(WeatherKind.RAIN, WeatherCodes.describe(81).kind)
        assertEquals(WeatherKind.SNOW, WeatherCodes.describe(73).kind)
        assertEquals(WeatherKind.STORM, WeatherCodes.describe(95).kind)
        assertEquals("Parçalı bulutlu", WeatherCodes.describe(2).text)
    }

    @Test
    fun bilinmeyenKodBulutluOlur() {
        assertEquals(WeatherKind.CLOUD, WeatherCodes.describe(-1).kind)
        assertEquals(WeatherKind.CLOUD, WeatherCodes.describe(1234).kind)
    }

    @Test
    fun konumYanitiCozulur() {
        val geo = WeatherParser.parseGeo(
            """{"success":true,"city":"İstanbul","region":"İstanbul","latitude":41.01,"longitude":28.97}"""
        )
        assertNotNull(geo)
        assertEquals("İstanbul", geo!!.city)
        assertEquals(41.01, geo.latitude, 1e-9)
        assertEquals(28.97, geo.longitude, 1e-9)
    }

    @Test
    fun konumBasarisizsaNullDoner() {
        assertNull(WeatherParser.parseGeo("""{"success":false,"message":"limit"}"""))
        assertNull(WeatherParser.parseGeo("""{"success":true}"""))
    }

    @Test
    fun sehirYoksaBolgeKullanilir() {
        val geo = WeatherParser.parseGeo("""{"success":true,"city":"","region":"Ankara","latitude":39.9,"longitude":32.8}""")
        assertEquals("Ankara", geo!!.city)
    }

    @Test
    fun tahminYanitiCozulur() {
        val w = WeatherParser.parseForecast(
            """{"current":{"temperature_2m":18.4,"apparent_temperature":16.6,"weather_code":2,"wind_speed_10m":12.5}}"""
        )
        assertNotNull(w)
        assertEquals(18, w!!.tempC)
        assertEquals(17, w.feelsC)
        assertEquals(13, w.windKmh)
        assertEquals(WeatherKind.PARTLY, w.kind)
    }

    @Test
    fun sicaklikYoksaNullDoner() {
        assertNull(WeatherParser.parseForecast("""{"current":{"weather_code":0}}"""))
        assertNull(WeatherParser.parseForecast("""{}"""))
    }
}

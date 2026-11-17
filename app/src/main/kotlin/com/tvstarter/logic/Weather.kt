package com.tvstarter.logic

import org.json.JSONObject
import kotlin.math.roundToInt

enum class WeatherKind { CLEAR, PARTLY, CLOUD, FOG, RAIN, SNOW, STORM }

data class WeatherDescription(val kind: WeatherKind, val text: String)

/** WMO hava durumu kodlarını (Open-Meteo `weather_code`) Türkçe metne çevirir. */
object WeatherCodes {
    fun describe(code: Int): WeatherDescription = when (code) {
        0 -> WeatherDescription(WeatherKind.CLEAR, "Açık")
        1 -> WeatherDescription(WeatherKind.CLEAR, "Çoğunlukla açık")
        2 -> WeatherDescription(WeatherKind.PARTLY, "Parçalı bulutlu")
        3 -> WeatherDescription(WeatherKind.CLOUD, "Kapalı")
        45, 48 -> WeatherDescription(WeatherKind.FOG, "Sisli")
        in 51..57 -> WeatherDescription(WeatherKind.RAIN, "Çiseleme")
        in 61..67 -> WeatherDescription(WeatherKind.RAIN, "Yağmurlu")
        in 71..77 -> WeatherDescription(WeatherKind.SNOW, "Karlı")
        in 80..82 -> WeatherDescription(WeatherKind.RAIN, "Sağanak yağmur")
        85, 86 -> WeatherDescription(WeatherKind.SNOW, "Kar sağanağı")
        95, 96, 99 -> WeatherDescription(WeatherKind.STORM, "Gök gürültülü fırtına")
        else -> WeatherDescription(WeatherKind.CLOUD, "Bulutlu")
    }
}

data class GeoPoint(val city: String, val latitude: Double, val longitude: Double)

data class CurrentWeather(
    val tempC: Int,
    val feelsC: Int,
    val windKmh: Int,
    val kind: WeatherKind,
    val text: String
)

object WeatherParser {
    /** ipwho.is yanıtı: {"success":true,"city":"...","latitude":..,"longitude":..}. */
    fun parseGeo(json: String): GeoPoint? {
        val o = JSONObject(json)
        if (!o.optBoolean("success", false)) return null
        if (!o.has("latitude") || !o.has("longitude")) return null
        val city = o.optString("city", "").ifBlank { o.optString("region", "") }
        return GeoPoint(city, o.getDouble("latitude"), o.getDouble("longitude"))
    }

    /** Open-Meteo yanıtı: {"current":{"temperature_2m":..,"weather_code":..}}. */
    fun parseForecast(json: String): CurrentWeather? {
        val cur = JSONObject(json).optJSONObject("current") ?: return null
        if (!cur.has("temperature_2m")) return null
        val temp = cur.getDouble("temperature_2m")
        val desc = WeatherCodes.describe(cur.optInt("weather_code", -1))
        return CurrentWeather(
            tempC = temp.roundToInt(),
            feelsC = cur.optDouble("apparent_temperature", temp).roundToInt(),
            windKmh = cur.optDouble("wind_speed_10m", 0.0).roundToInt(),
            kind = desc.kind,
            text = desc.text
        )
    }
}

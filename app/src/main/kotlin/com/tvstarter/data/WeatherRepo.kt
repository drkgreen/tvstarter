package com.tvstarter.data

import com.tvstarter.logic.CurrentWeather
import com.tvstarter.logic.GeoPoint
import com.tvstarter.logic.WeatherParser

data class WeatherResult(val city: String, val weather: CurrentWeather)

/**
 * Konum IP adresinden bulunur (izin gerekmez, yaklaşık şehir). Hava durumu Open-Meteo'dan gelir.
 * İkisi de anahtarsızdır.
 */
class WeatherRepo {
    private var geo: GeoPoint? = null

    /** Arka planda çalışır; sonucu ana iş parçacığında [onResult] ile verir. Hata olursa null. */
    fun load(onResult: (WeatherResult?) -> Unit) {
        Bg.run {
            val result = try {
                val g = geo ?: WeatherParser.parseGeo(Net.getString(GEO_URL)).also { geo = it }
                if (g == null) {
                    null
                } else {
                    val url = "https://api.open-meteo.com/v1/forecast?latitude=${g.latitude}&longitude=${g.longitude}" +
                        "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&timezone=auto"
                    val w = WeatherParser.parseForecast(Net.getString(url))
                    if (w == null) null else WeatherResult(g.city, w)
                }
            } catch (e: Exception) {
                null
            }
            Bg.post { onResult(result) }
        }
    }

    private companion object {
        const val GEO_URL = "https://ipwho.is/"
    }
}

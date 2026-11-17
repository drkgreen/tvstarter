package com.tvstarter.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.tvstarter.R
import com.tvstarter.data.WeatherRepo
import com.tvstarter.data.WeatherResult
import com.tvstarter.databinding.PanelHomeBinding
import com.tvstarter.logic.WeatherKind

/** HOME sekmesi: büyük saat, tarih ve bulunduğun yerin hava durumu. Arka resmi [MainActivity] gösterir. */
class HomePanel(context: Context) : FrameLayout(context), TabPanel {
    private val b = PanelHomeBinding.inflate(LayoutInflater.from(context), this, true)
    private val repo = WeatherRepo()
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = Runnable { loadWeather(force = true) }
    private var visible = false
    private var lastOkMs = 0L

    override val view: View get() = this

    override fun onShow() {
        visible = true
        loadWeather(force = false)
    }

    override fun onHide() {
        visible = false
        handler.removeCallbacks(refresh)
    }

    override fun focusContent(): Boolean = false

    private fun loadWeather(force: Boolean) {
        val age = System.currentTimeMillis() - lastOkMs
        if (!force && age < FRESH_MS) {
            handler.removeCallbacks(refresh)
            handler.postDelayed(refresh, FRESH_MS - age)
            return
        }
        handler.removeCallbacks(refresh)
        repo.load { result ->
            if (!visible) return@load
            if (result != null) {
                lastOkMs = System.currentTimeMillis()
                render(result)
            }
            handler.postDelayed(refresh, if (result == null) RETRY_MS else FRESH_MS)
        }
    }

    private fun render(r: WeatherResult) {
        val w = r.weather
        b.weatherRow.visibility = View.VISIBLE
        b.weatherIcon.setImageResource(iconFor(w.kind))
        b.weatherTemp.text = "${w.tempC}°"
        b.weatherCity.text = if (r.city.isBlank()) w.text else "${r.city} · ${w.text}"
        b.weatherDetail.text = context.getString(R.string.weather_feels, w.feelsC) + " · " +
            context.getString(R.string.weather_wind, w.windKmh)
    }

    private fun iconFor(kind: WeatherKind): Int = when (kind) {
        WeatherKind.CLEAR -> R.drawable.ic_wx_clear
        WeatherKind.PARTLY -> R.drawable.ic_wx_partly
        WeatherKind.CLOUD -> R.drawable.ic_wx_cloud
        WeatherKind.FOG -> R.drawable.ic_wx_fog
        WeatherKind.RAIN -> R.drawable.ic_wx_rain
        WeatherKind.SNOW -> R.drawable.ic_wx_snow
        WeatherKind.STORM -> R.drawable.ic_wx_storm
    }

    private companion object {
        const val FRESH_MS = 30L * 60 * 1000
        const val RETRY_MS = 60L * 1000
    }
}

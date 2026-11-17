package com.tvstarter.data

import android.content.Context
import com.tvstarter.BuildConfig

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("tvstarter", Context.MODE_PRIVATE)

    /** Favori uygulamaların paket adları. */
    var favorites: Set<String>
        get() = sp.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
        set(value) {
            sp.edit().putStringSet(KEY_FAVORITES, HashSet(value)).apply()
        }

    /** TV'de girilen portföy adresi. Boşsa derleme anındaki varsayılan kullanılır. */
    var portfolioUrl: String
        get() = sp.getString(KEY_PORTFOLIO_URL, "") ?: ""
        set(value) {
            sp.edit().putString(KEY_PORTFOLIO_URL, value.trim()).apply()
        }

    val effectivePortfolioUrl: String
        get() = portfolioUrl.ifBlank { BuildConfig.PORTFOLIO_URL }

    var portfolioHistory: String
        get() = sp.getString(KEY_HISTORY, "") ?: ""
        set(value) {
            sp.edit().putString(KEY_HISTORY, value).apply()
        }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_PORTFOLIO_URL = "portfolio_url"
        const val KEY_HISTORY = "portfolio_history"
    }
}

package com.tvstarter.logic

import org.json.JSONObject

data class Wallpaper(val url: String, val caption: String)

/**
 * Arka plan resim kaynağı. Varsayılan: Bing günlük resim arşivi (son 8 resim).
 * Resmi bir API değildir. Kaynak değişirse yalnızca bu nesne ve [FEED_URL] değişir.
 */
object WallpaperFeed {
    const val FEED_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=tr-TR"
    private const val BASE = "https://www.bing.com"

    fun parseBing(json: String): List<Wallpaper> {
        val arr = JSONObject(json).optJSONArray("images") ?: return emptyList()
        val out = ArrayList<Wallpaper>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val path = o.optString("url", "")
            if (path.isBlank()) continue
            val url = if (path.startsWith("http")) path else BASE + path
            out.add(Wallpaper(url, caption(o.optString("copyright", ""))))
        }
        return out
    }

    /** "Kapadokya, Türkiye (© Fotoğrafçı/Ajans)" -> "Kapadokya, Türkiye" */
    fun caption(copyright: String): String {
        val cut = copyright.indexOf(" (©")
        return (if (cut >= 0) copyright.substring(0, cut) else copyright).trim()
    }
}

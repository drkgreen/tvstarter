package com.tvstarter.logic

import org.json.JSONObject

enum class Direction { UP, DOWN, NEUTRAL }

enum class NewsDirection {
    UP, DOWN, MIXED, UNCLEAR;

    companion object {
        fun from(raw: String?): NewsDirection = when (raw) {
            "up" -> UP
            "down" -> DOWN
            "mixed" -> MIXED
            else -> UNCLEAR
        }
    }
}

enum class NewsLevel {
    HIGH, MEDIUM, LOW;

    companion object {
        fun from(raw: String?): NewsLevel = when (raw) {
            "high" -> HIGH
            "medium" -> MEDIUM
            else -> LOW
        }
    }
}

data class Ounce(val price: String?, val direction: Direction, val changePercent: String?)

data class NewsItem(
    val title: String,
    val direction: NewsDirection,
    val level: NewsLevel,
    val timeIso: String?
)

data class PortfolioSnapshot(
    val total: Double,
    val goldAmount: Double,
    val goldPrice: Double,
    val silverAmount: Double,
    val silverPrice: Double,
    val goldOunce: Ounce,
    val silverOunce: Ounce,
    val news: List<NewsItem>,
    val updatedAt: String
) {
    val goldValue: Double get() = goldAmount * goldPrice
    val silverValue: Double get() = silverAmount * silverPrice
}

/** `altin-gumus-tracker` `/api/widget-data` yanıtını çözer. */
object PortfolioParser {
    /** Yanıt `success: false` ise [IllegalArgumentException] fırlatır. */
    fun parse(json: String): PortfolioSnapshot {
        val o = JSONObject(json)
        if (!o.optBoolean("success", false)) {
            throw IllegalArgumentException(o.optString("error", "bilinmeyen hata"))
        }
        val goldAmount = o.optDouble("gold_amount", 0.0)
        val silverAmount = o.optDouble("silver_amount", 0.0)
        val goldPrice = o.optDouble("gold_price", 0.0)
        val silverPrice = o.optDouble("silver_price", 0.0)
        val total = if (o.has("portfolio_value") && !o.isNull("portfolio_value")) {
            o.optDouble("portfolio_value", 0.0)
        } else {
            goldAmount * goldPrice + silverAmount * silverPrice
        }
        return PortfolioSnapshot(
            total = total,
            goldAmount = goldAmount,
            goldPrice = goldPrice,
            silverAmount = silverAmount,
            silverPrice = silverPrice,
            goldOunce = parseOunce(o.optJSONObject("gold_ounce")),
            silverOunce = parseOunce(o.optJSONObject("silver_ounce")),
            news = parseNews(o),
            updatedAt = o.optString("updated_at", "")
        )
    }

    private fun parseOunce(o: JSONObject?): Ounce {
        if (o == null) return Ounce(null, Direction.NEUTRAL, null)
        val dir = when (o.optString("direction", "")) {
            "up" -> Direction.UP
            "down" -> Direction.DOWN
            else -> Direction.NEUTRAL
        }
        return Ounce(text(o, "price"), dir, text(o, "change_percent"))
    }

    private fun text(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        val v = o.get(key).toString()
        return if (v.isBlank()) null else v
    }

    private fun parseNews(o: JSONObject): List<NewsItem> {
        val arr = o.optJSONArray("gold_news") ?: return emptyList()
        val out = ArrayList<NewsItem>()
        for (i in 0 until arr.length()) {
            val n = arr.optJSONObject(i) ?: continue
            val title = n.optString("title", "").trim()
            if (title.isEmpty()) continue
            val time = text(n, "published_at") ?: text(n, "fetched_at")
            out.add(
                NewsItem(
                    title = title,
                    direction = NewsDirection.from(text(n, "impact_direction")),
                    level = NewsLevel.from(text(n, "impact_level")),
                    timeIso = time
                )
            )
        }
        return out
    }
}

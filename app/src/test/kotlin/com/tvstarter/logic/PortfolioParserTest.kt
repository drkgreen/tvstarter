package com.tvstarter.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PortfolioParserTest {
    private val sample = """
    {
      "success": true,
      "gold_price": 3250.5,
      "silver_price": 38.5,
      "gold_amount": 100,
      "silver_amount": 200,
      "portfolio_value": 332750,
      "updated_at": "20:44",
      "gold_ounce": {"price": "2650.4", "direction": "up", "change_percent": "+0.4%"},
      "silver_ounce": {"price": null, "direction": "neutral", "change_percent": null},
      "gold_news": [
        {"title": "Altın onsta kâr satışı", "impact_direction": "down", "impact_level": "high",
         "fetched_at": "2026-11-17T10:23:45.123456+00:00", "published_at": null},
        {"title": "  ", "impact_direction": "up"},
        {"title": "Merkez bankaları alıyor", "impact_direction": "weird", "impact_level": "medium",
         "published_at": "2026-11-17T08:00:00Z"}
      ]
    }
    """.trimIndent()

    @Test
    fun alanlarCozulur() {
        val s = PortfolioParser.parse(sample)
        assertEquals(332750.0, s.total, 1e-6)
        assertEquals(100.0, s.goldAmount, 1e-6)
        assertEquals(3250.5, s.goldPrice, 1e-6)
        assertEquals(325050.0, s.goldValue, 1e-6)
        assertEquals(7700.0, s.silverValue, 1e-6)
        assertEquals("20:44", s.updatedAt)
    }

    @Test
    fun onsCozulur() {
        val s = PortfolioParser.parse(sample)
        assertEquals("2650.4", s.goldOunce.price)
        assertEquals(Direction.UP, s.goldOunce.direction)
        assertEquals("+0.4%", s.goldOunce.changePercent)
        assertNull(s.silverOunce.price)
        assertEquals(Direction.NEUTRAL, s.silverOunce.direction)
    }

    @Test
    fun haberlerCozulurVeBosBaslikAtlanir() {
        val s = PortfolioParser.parse(sample)
        assertEquals(2, s.news.size)
        assertEquals(NewsDirection.DOWN, s.news[0].direction)
        assertEquals(NewsLevel.HIGH, s.news[0].level)
        assertEquals("2026-11-17T10:23:45.123456+00:00", s.news[0].timeIso)
        assertEquals(NewsDirection.UNCLEAR, s.news[1].direction)
        assertEquals("2026-11-17T08:00:00Z", s.news[1].timeIso)
    }

    @Test
    fun toplamYoksaHesaplanir() {
        val s = PortfolioParser.parse(
            """{"success":true,"gold_price":10,"silver_price":2,"gold_amount":3,"silver_amount":5}"""
        )
        assertEquals(40.0, s.total, 1e-6)
        assertTrue(s.news.isEmpty())
    }

    @Test
    fun basarisizYanitHataFirlatir() {
        try {
            PortfolioParser.parse("""{"success":false,"error":"Unauthorized"}""")
            fail("hata bekleniyordu")
        } catch (e: IllegalArgumentException) {
            assertEquals("Unauthorized", e.message)
        }
    }
}

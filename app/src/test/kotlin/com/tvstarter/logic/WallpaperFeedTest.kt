package com.tvstarter.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperFeedTest {
    @Test
    fun bingYanitiCozulur() {
        val json = """
        {"images":[
          {"url":"/th?id=OHR.Kapadokya_TR_1920x1080.jpg&rf=x","copyright":"Kapadokya, Türkiye (© Foto/Ajans)"},
          {"url":"","copyright":"boş"},
          {"url":"https://example.com/a.jpg","copyright":"Dağ"}
        ]}
        """.trimIndent()
        val list = WallpaperFeed.parseBing(json)
        assertEquals(2, list.size)
        assertEquals("https://www.bing.com/th?id=OHR.Kapadokya_TR_1920x1080.jpg&rf=x", list[0].url)
        assertEquals("Kapadokya, Türkiye", list[0].caption)
        assertEquals("https://example.com/a.jpg", list[1].url)
        assertEquals("Dağ", list[1].caption)
    }

    @Test
    fun resimYoksaBosListe() {
        assertTrue(WallpaperFeed.parseBing("""{}""").isEmpty())
        assertTrue(WallpaperFeed.parseBing("""{"images":[]}""").isEmpty())
    }

    @Test
    fun altyaziTelifSatiriniAtar() {
        assertEquals("Abant, Bolu", WallpaperFeed.caption("Abant, Bolu (© X)"))
        assertEquals("Düz yazı", WallpaperFeed.caption("Düz yazı"))
    }
}

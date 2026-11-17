package com.tvstarter.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListTest {
    private fun app(pkg: String, label: String) = AppItem(pkg, "$pkg.Main", label)

    private val items = listOf(
        app("c", "Çizgi film"),
        app("z", "Zaman"),
        app("a", "Ayarlar"),
        app("i", "Işık"),
        app("b", "Browsy")
    )

    @Test
    fun favoriYoksaTekBaslikVeAlfabetikSira() {
        val rows = AppListBuilder.build(items, emptySet(), "Favoriler", "Diğer", "Tümü")
        assertEquals(AppRow.Header("Tümü"), rows[0])
        val labels = rows.drop(1).map { (it as AppRow.App).item.label }
        // Türkçe sıra: Ayarlar, Browsy, Çizgi film, Işık, Zaman
        assertEquals(listOf("Ayarlar", "Browsy", "Çizgi film", "Işık", "Zaman"), labels)
    }

    @Test
    fun favorilerOneAlinir() {
        val rows = AppListBuilder.build(items, setOf("z", "b"), "Favoriler", "Diğer", "Tümü")
        assertEquals(AppRow.Header("Favoriler"), rows[0])
        assertEquals("Browsy", (rows[1] as AppRow.App).item.label)
        assertTrue((rows[1] as AppRow.App).favorite)
        assertEquals("Zaman", (rows[2] as AppRow.App).item.label)
        assertEquals(AppRow.Header("Diğer"), rows[3])
        assertEquals(false, (rows[4] as AppRow.App).favorite)
        assertEquals(1 + 2 + 1 + 3, rows.size)
    }

    @Test
    fun herUygulamaFavoriyseDigerBasligiYok() {
        val rows = AppListBuilder.build(items, items.map { it.packageName }.toSet(), "Favoriler", "Diğer", "Tümü")
        assertEquals(1 + items.size, rows.size)
        assertTrue(rows.none { it == AppRow.Header("Diğer") })
    }

    @Test
    fun bosListeBosDoner() {
        assertTrue(AppListBuilder.build(emptyList(), setOf("a"), "F", "D", "T").isEmpty())
    }

    @Test
    fun anahtarPaketVeEtkinlikAdindanOlusur() {
        assertEquals("a/a.Main", app("a", "Ayarlar").key)
    }
}

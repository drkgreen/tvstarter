package com.tvstarter.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioHistoryTest {
    private val min = 60_000L
    private val hour = 60 * min

    @Test
    fun kisaAralikliOrnekEklenmez() {
        val h = PortfolioHistory(minGapMs = 10 * min)
        assertTrue(h.add(Sample(0, 100.0)))
        assertFalse(h.add(Sample(5 * min, 101.0)))
        assertTrue(h.add(Sample(10 * min, 102.0)))
        assertEquals(2, h.size)
    }

    @Test
    fun saatGeriGidinceEklenmez() {
        val h = PortfolioHistory(minGapMs = 10 * min)
        h.add(Sample(100 * min, 1.0))
        assertFalse(h.add(Sample(50 * min, 2.0)))
    }

    @Test
    fun enFazlaOrnekSayisiAsilmaz() {
        val h = PortfolioHistory(maxSamples = 3, minGapMs = 1)
        for (i in 0 until 10) h.add(Sample(i.toLong() * 10, i.toDouble()))
        assertEquals(3, h.size)
        // Sıra: eski -> yeni. En eski örnekler atılır.
        assertEquals(listOf(7.0, 8.0, 9.0), h.slice(HistoryRange.MONTH, 1000).map { it.value })
    }

    @Test
    fun aralikSadeceSonSuresiniKapsar() {
        val h = PortfolioHistory(minGapMs = 1)
        val now = 10 * 24 * hour
        h.add(Sample(now - 9 * 24 * hour, 50.0))
        h.add(Sample(now - 2 * hour, 100.0))
        h.add(Sample(now - hour, 110.0))
        assertEquals(2, h.slice(HistoryRange.DAY, now).size)
        assertEquals(2, h.slice(HistoryRange.WEEK, now).size)
        assertEquals(3, h.slice(HistoryRange.MONTH, now).size)
    }

    @Test
    fun yuzdeDegisimHesaplanir() {
        val h = PortfolioHistory(minGapMs = 1)
        val now = 100 * hour
        h.add(Sample(now - 3 * hour, 200.0))
        h.add(Sample(now - hour, 210.0))
        val ch = h.changePercent(HistoryRange.DAY, now)
        assertNotNull(ch)
        assertEquals(5.0, ch!!, 1e-9)
    }

    @Test
    fun veriYetersizseDegisimNullOlur() {
        val h = PortfolioHistory(minGapMs = 1)
        assertNull(h.changePercent(HistoryRange.DAY, 1000))
        h.add(Sample(500, 1.0))
        assertNull(h.changePercent(HistoryRange.DAY, 1000))
    }

    @Test
    fun grafikNoktalariSeyreltilir() {
        val h = PortfolioHistory(minGapMs = 1)
        for (i in 0 until 100) h.add(Sample(i.toLong(), i.toDouble()))
        val pts = h.spark(HistoryRange.MONTH, 100, maxPoints = 10)
        assertEquals(10, pts.size)
        assertEquals(0.0, pts.first(), 1e-9)
        assertEquals(99.0, pts.last(), 1e-9)
    }

    @Test
    fun kaydedipGeriYuklenir() {
        val h = PortfolioHistory(minGapMs = 1)
        h.add(Sample(1000, 12.5))
        h.add(Sample(2000, 13.75))
        val back = PortfolioHistory.parse(h.serialize(), minGapMs = 1)
        assertEquals(2, back.size)
        assertEquals(h.serialize(), back.serialize())
    }

    @Test
    fun bozukKayitAtlanir() {
        val h = PortfolioHistory.parse("1000,5.0;bozuk;x,y;2000,6.0;3000", minGapMs = 1)
        assertEquals(2, h.size)
        assertEquals(0, PortfolioHistory.parse(null).size)
        assertEquals(0, PortfolioHistory.parse("").size)
    }
}

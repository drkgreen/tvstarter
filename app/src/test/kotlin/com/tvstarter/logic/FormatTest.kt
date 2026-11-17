package com.tvstarter.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class FormatTest {
    @Test
    fun paraBicimiTurkceAyracKullanir() {
        assertEquals("₺ 1.248.600", TrFormat.currency(1248600.4))
        assertEquals("₺3.250,50", TrFormat.price(3250.5))
        assertEquals("₺38,50", TrFormat.price(38.5))
    }

    @Test
    fun miktarGereksizSifirYazmaz() {
        assertEquals("125", TrFormat.amount(125.0))
        assertEquals("4.200,5", TrFormat.amount(4200.5))
    }

    @Test
    fun yuzdeIsaretsizYazilir() {
        assertEquals("%0,8", TrFormat.percent(0.8))
        assertEquals("%2,4", TrFormat.percent(-2.4))
    }

    @Test
    fun haberZamaniYerelSaateCevrilir() {
        val tr = TimeZone.getTimeZone("Europe/Istanbul")
        assertEquals("17 Nov 13:23", NewsTime.format("2026-11-17T10:23:45.123456+00:00", tr, Locale.ENGLISH))
        assertEquals("17 Nov 11:00", NewsTime.format("2026-11-17T08:00:00Z", tr, Locale.ENGLISH))
        assertEquals("17 Nov 10:00", NewsTime.format("2026-11-17 10:00:00+03:00", tr, Locale.ENGLISH))
    }

    @Test
    fun gecersizZamanBosDoner() {
        assertEquals("", NewsTime.format(null))
        assertEquals("", NewsTime.format(""))
        assertEquals("", NewsTime.format("dün"))
    }
}

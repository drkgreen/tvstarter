package com.tvstarter.logic

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Türkçe sayı biçimi: binlik ayracı nokta, ondalık ayracı virgül. */
object TrFormat {
    private val TR = Locale("tr", "TR")

    private fun format(pattern: String, v: Double): String =
        DecimalFormat(pattern, DecimalFormatSymbols(TR)).format(v)

    /** 1248600.4 -> "₺ 1.248.600" */
    fun currency(v: Double): String = "₺ " + format("#,##0", v)

    /** 3250.5 -> "₺3.250,50" */
    fun price(v: Double): String = "₺" + format("#,##0.00", v)

    /** 125.0 -> "125", 4200.5 -> "4.200,5" */
    fun amount(v: Double): String = format("#,##0.##", v)

    /** 0.8 -> "%0,8" (işaretsiz; yön oku ayrı gösterilir) */
    fun percent(v: Double): String = "%" + format("0.0", kotlin.math.abs(v))
}

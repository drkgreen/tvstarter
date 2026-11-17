package com.tvstarter.logic

import kotlin.math.abs

data class Sample(val timeMs: Long, val value: Double)

enum class HistoryRange(val label: String, val durationMs: Long) {
    DAY("1G", 24L * 60 * 60 * 1000),
    WEEK("1H", 7L * 24 * 60 * 60 * 1000),
    MONTH("1A", 30L * 24 * 60 * 60 * 1000)
}

/**
 * Portföy değerinin cihazda biriken geçmişi. Sunucu geçmiş vermez:
 * her başarılı yenilemede bir örnek eklenir (en az [minGapMs] arayla).
 */
class PortfolioHistory(
    private val maxSamples: Int = 4500,
    private val minGapMs: Long = 10L * 60 * 1000
) {
    private val samples = ArrayList<Sample>()

    val size: Int get() = samples.size

    /** Örnek eklendiyse true döner. Saat geriye gittiyse veya aralık kısaysa eklemez. */
    fun add(sample: Sample): Boolean {
        val last = samples.lastOrNull()
        if (last != null && sample.timeMs - last.timeMs < minGapMs) return false
        samples.add(sample)
        while (samples.size > maxSamples) samples.removeAt(0)
        return true
    }

    fun slice(range: HistoryRange, nowMs: Long): List<Sample> {
        val from = nowMs - range.durationMs
        return samples.filter { it.timeMs >= from }
    }

    /** Aralıktaki ilk ve son değer arasındaki yüzde değişim. Yeterli veri yoksa null. */
    fun changePercent(range: HistoryRange, nowMs: Long): Double? {
        val s = slice(range, nowMs)
        if (s.size < 2) return null
        val first = s.first().value
        if (abs(first) < 1e-9) return null
        return (s.last().value - first) / first * 100.0
    }

    /** Grafik için en çok [maxPoints] noktaya seyreltilmiş değerler. */
    fun spark(range: HistoryRange, nowMs: Long, maxPoints: Int = 48): List<Double> {
        val s = slice(range, nowMs)
        if (s.size <= maxPoints || maxPoints < 2) return s.map { it.value }
        return (0 until maxPoints).map { i -> s[i * (s.size - 1) / (maxPoints - 1)].value }
    }

    /** "zaman,değer;zaman,değer" biçimi. */
    fun serialize(): String = samples.joinToString(";") { "${it.timeMs},${it.value}" }

    companion object {
        fun parse(text: String?, maxSamples: Int = 4500, minGapMs: Long = 10L * 60 * 1000): PortfolioHistory {
            val h = PortfolioHistory(maxSamples, minGapMs)
            if (text.isNullOrBlank()) return h
            for (part in text.split(';')) {
                val bits = part.split(',')
                if (bits.size != 2) continue
                val t = bits[0].toLongOrNull() ?: continue
                val v = bits[1].toDoubleOrNull() ?: continue
                h.samples.add(Sample(t, v))
            }
            while (h.samples.size > maxSamples) h.samples.removeAt(0)
            return h
        }
    }
}

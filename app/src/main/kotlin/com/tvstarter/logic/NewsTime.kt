package com.tvstarter.logic

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Supabase ISO zamanını ("2026-11-17T10:23:45.123456+00:00") "17 Kas 13:23" biçimine çevirir. */
object NewsTime {
    private val ISO = Regex("""^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})(?:\.\d+)?(Z|[+-]\d{2}(?::?\d{2})?)?$""")

    fun format(
        iso: String?,
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale("tr", "TR")
    ): String {
        if (iso.isNullOrBlank()) return ""
        val m = ISO.matchEntire(iso.trim()) ?: return ""
        val offset = normalizeOffset(m.groupValues[3])
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US)
        val date = try {
            parser.parse("${m.groupValues[1]} ${m.groupValues[2]}$offset")
        } catch (e: ParseException) {
            null
        } ?: return ""
        val out = SimpleDateFormat("d MMM HH:mm", locale)
        out.timeZone = zone
        return out.format(date)
    }

    private fun normalizeOffset(raw: String): String {
        if (raw.isEmpty() || raw == "Z") return "+0000"
        val sign = raw[0]
        val digits = raw.substring(1).replace(":", "").padEnd(4, '0')
        return "$sign$digits"
    }
}

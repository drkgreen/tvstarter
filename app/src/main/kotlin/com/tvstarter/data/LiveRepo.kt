package com.tvstarter.data

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager

class LiveItem(
    val title: String,
    val number: String,
    val program: String,
    /** 0..100. Program süresi yoksa 0. */
    val progress: Int,
    val intent: Intent
)

class LiveResult(val channels: List<LiveItem>, val inputs: List<LiveItem>)

object LiveRepo {
    private const val MAX_CHANNELS = 16

    /**
     * Canlı TV kanallarını TvProvider'dan okur. Başka uygulamaların kanallarını okumak için
     * sistem izni gerekir; verilmediyse kanal listesi boş gelir. Bu durumda yalnızca
     * HDMI gibi doğrudan giriş kaynakları listelenir. Arka planda çağır.
     */
    fun load(context: Context): LiveResult = LiveResult(readChannels(context), readInputs(context))

    private fun readChannels(context: Context): List<LiveItem> {
        val out = ArrayList<LiveItem>()
        try {
            val projection = arrayOf(
                TvContract.Channels._ID,
                TvContract.Channels.COLUMN_DISPLAY_NUMBER,
                TvContract.Channels.COLUMN_DISPLAY_NAME
            )
            context.contentResolver.query(TvContract.Channels.CONTENT_URI, projection, null, null, null)?.use { c ->
                while (c.moveToNext() && out.size < MAX_CHANNELS) {
                    val id = c.getLong(0)
                    val number = c.getString(1) ?: ""
                    val name = c.getString(2) ?: continue
                    val now = System.currentTimeMillis()
                    val program = nowPlaying(context, id, now)
                    out.add(
                        LiveItem(
                            title = name,
                            number = number,
                            program = program.first,
                            progress = program.second,
                            intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUri(id))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // SecurityException dahil: izin yok veya TvProvider yok. Boş liste döner.
        }
        return out
    }

    private fun nowPlaying(context: Context, channelId: Long, now: Long): Pair<String, Int> {
        try {
            val projection = arrayOf(
                TvContract.Programs.COLUMN_TITLE,
                TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS,
                TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS
            )
            val uri = TvContract.buildProgramsUriForChannel(channelId, now, now)
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val title = c.getString(0) ?: ""
                    val start = c.getLong(1)
                    val end = c.getLong(2)
                    val progress = if (end > start) (((now - start) * 100) / (end - start)).toInt().coerceIn(0, 100) else 0
                    return Pair(title, progress)
                }
            }
        } catch (e: Exception) {
            // Program bilgisi okunamadı: yalnızca kanal adı gösterilir.
        }
        return Pair("", 0)
    }

    private fun readInputs(context: Context): List<LiveItem> {
        val out = ArrayList<LiveItem>()
        try {
            val tm = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return out
            for (info in tm.tvInputList) {
                if (!info.isPassthroughInput) continue
                val label = info.loadLabel(context)?.toString() ?: continue
                out.add(
                    LiveItem(
                        title = label,
                        number = "",
                        program = "",
                        progress = 0,
                        intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.id))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                )
            }
        } catch (e: Exception) {
            // Giriş listesi okunamadı.
        }
        return out
    }
}

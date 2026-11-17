package com.tvstarter.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Arka plan iş parçacığı havuzu ve ana iş parçacığına geçiş. */
object Bg {
    private val pool = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    fun run(block: () -> Unit) {
        pool.execute(block)
    }

    fun post(block: () -> Unit) {
        main.post(block)
    }
}

object Net {
    private const val TIMEOUT_MS = 10_000
    private const val MAX_BYTES = 12 * 1024 * 1024

    fun getString(url: String): String = String(getBytes(url), Charsets.UTF_8)

    fun getBytes(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = TIMEOUT_MS
        c.readTimeout = TIMEOUT_MS
        c.setRequestProperty("User-Agent", "TvStarter/1.0")
        try {
            val code = c.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            val out = ByteArrayOutputStream()
            c.inputStream.use { input ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > MAX_BYTES) throw IOException("yanıt çok büyük")
                }
            }
            return out.toByteArray()
        } finally {
            c.disconnect()
        }
    }

    /** Resmi ekran boyutuna yakın küçültüp çözer. Bellek için tek geçişte örnekler. */
    fun getBitmap(url: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bytes = getBytes(url)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= reqWidth && bounds.outHeight / (sample * 2) >= reqHeight) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}

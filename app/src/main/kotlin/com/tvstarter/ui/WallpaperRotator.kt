package com.tvstarter.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.tvstarter.data.Bg
import com.tvstarter.data.Net
import com.tvstarter.logic.Wallpaper
import com.tvstarter.logic.WallpaperFeed

/**
 * HOME arka planı: resimleri sırayla çapraz geçişle gösterir, yavaşça yakınlaştırır.
 * İnternet yoksa arka plan düz lacivert kalır. [start] ve [stop] ile açılıp kapanır.
 */
class WallpaperRotator(
    private val first: ImageView,
    private val second: ImageView,
    private val caption: TextView
) {
    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { advance() }
    private var items: List<Wallpaper> = emptyList()
    private var index = -1
    private var running = false
    private var showingFirst = false
    private var zoom: ObjectAnimator? = null

    fun start() {
        if (running) return
        running = true
        if (items.isEmpty()) loadFeed() else advance()
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
        zoom?.cancel()
    }

    private fun loadFeed() {
        Bg.run {
            val list = try {
                WallpaperFeed.parseBing(Net.getString(WallpaperFeed.FEED_URL))
            } catch (e: Exception) {
                emptyList()
            }
            Bg.post {
                if (list.isNotEmpty()) items = list
                if (!running) return@post
                if (items.isEmpty()) handler.postDelayed({ if (running) loadFeed() }, RETRY_MS) else advance()
            }
        }
    }

    private fun advance() {
        if (!running || items.isEmpty()) return
        val next = (index + 1) % items.size
        val wallpaper = items[next]
        val metrics = first.resources.displayMetrics
        Bg.run {
            val bitmap = try {
                Net.getBitmap(wallpaper.url, metrics.widthPixels, metrics.heightPixels)
            } catch (e: Exception) {
                null
            }
            Bg.post {
                if (!running) return@post
                index = next
                if (bitmap == null) {
                    handler.postDelayed(tick, RETRY_MS)
                } else {
                    show(bitmap, wallpaper.caption)
                    handler.postDelayed(tick, INTERVAL_MS)
                }
            }
        }
    }

    private fun show(bitmap: Bitmap, text: String) {
        val incoming = if (showingFirst) second else first
        val outgoing = if (showingFirst) first else second
        showingFirst = !showingFirst

        incoming.setImageBitmap(bitmap)
        incoming.scaleX = 1f
        incoming.scaleY = 1f
        incoming.animate().alpha(1f).setDuration(FADE_MS).start()
        outgoing.animate().alpha(0f).setDuration(FADE_MS).withEndAction { outgoing.setImageDrawable(null) }.start()

        zoom?.cancel()
        zoom = ObjectAnimator.ofPropertyValuesHolder(
            incoming,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f)
        ).apply {
            duration = INTERVAL_MS + FADE_MS
            interpolator = LinearInterpolator()
            start()
        }

        caption.text = text
        caption.visibility = if (text.isBlank()) View.INVISIBLE else View.VISIBLE
    }

    private companion object {
        const val INTERVAL_MS = 30_000L
        const val FADE_MS = 1_600L
        const val RETRY_MS = 15_000L
    }
}

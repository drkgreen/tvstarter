package com.tvstarter.ui

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.tvstarter.R
import com.tvstarter.data.Bg
import com.tvstarter.data.Net
import com.tvstarter.data.Prefs
import com.tvstarter.databinding.ItemNewsBinding
import com.tvstarter.databinding.PanelPortfolioBinding
import com.tvstarter.logic.Direction
import com.tvstarter.logic.HistoryRange
import com.tvstarter.logic.NewsDirection
import com.tvstarter.logic.NewsItem
import com.tvstarter.logic.NewsLevel
import com.tvstarter.logic.NewsTime
import com.tvstarter.logic.Ounce
import com.tvstarter.logic.PortfolioHistory
import com.tvstarter.logic.PortfolioParser
import com.tvstarter.logic.PortfolioSnapshot
import com.tvstarter.logic.Sample
import com.tvstarter.logic.TrFormat

/**
 * PORTFÖY sekmesi: toplam değer, altın, gümüş, ons fiyatı ve piyasa haberleri.
 * Veri `altin-gumus-tracker` `/api/widget-data` adresinden gelir; adres [Prefs] içinde tutulur.
 * Tamam: grafik aralığını değiştirir. Uzun basış: veri adresini değiştirir.
 */
class PortfolioPanel(context: Context) : FrameLayout(context), TabPanel {
    private val b = PanelPortfolioBinding.inflate(LayoutInflater.from(context), this, true)
    private val prefs = Prefs(context)
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = Runnable { refresh() }
    private val newsViews = ArrayList<ItemNewsBinding>()
    private var history = PortfolioHistory.parse(prefs.portfolioHistory)
    private var range = HistoryRange.DAY
    private var snapshot: PortfolioSnapshot? = null
    private var error: String? = null
    private var visible = false

    override val view: View get() = this

    init {
        val ranges = HistoryRange.values()
        val chips = listOf(b.chipDay, b.chipWeek, b.chipMonth)
        for (i in ranges.indices) chips[i].text = ranges[i].label

        for (i in 0 until NEWS_SLOTS) {
            val nb = ItemNewsBinding.inflate(LayoutInflater.from(context), b.newsColumn, false)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, if (i == 0) 1.25f else 1f)
            if (i > 0) lp.topMargin = context.dp(12)
            nb.root.layoutParams = lp
            nb.root.id = View.generateViewId()
            nb.root.visibility = View.INVISIBLE
            nb.root.focusScale(1.03f)
            b.newsColumn.addView(nb.root)
            newsViews.add(nb)
        }
        // Satırlar sol ve sağda hizalı: yatay odak aynı satırdaki karta gider.
        val left = listOf(b.cardTotal, b.cardGold, b.cardSilver)
        for (i in 0 until NEWS_SLOTS) {
            left[i].nextFocusRightId = newsViews[i].root.id
            newsViews[i].root.nextFocusLeftId = left[i].id
        }
        left.forEach { it.focusScale(1.03f) }
        b.emptyCard.focusScale(1.03f)

        b.cardTotal.setOnClickListener { cycleRange() }
        b.cardTotal.setOnLongClickListener {
            promptUrl()
            true
        }
        b.emptyCard.setOnClickListener { promptUrl() }
        render()
    }

    override fun onShow() {
        visible = true
        render()
        refresh()
    }

    override fun onHide() {
        visible = false
        handler.removeCallbacks(refreshRunnable)
    }

    override fun focusContent(): Boolean {
        val target = if (currentUrl().isBlank()) b.emptyCard else b.cardTotal
        return target.requestFocus()
    }

    /** Veri adresini TV'de sorar. Ayarlar menüsünden de çağrılır. */
    fun promptUrl() {
        TextPrompt.show(
            context,
            context.getString(R.string.portfolio_url_title),
            context.getString(R.string.portfolio_url_hint),
            prefs.portfolioUrl,
            context.getString(R.string.save),
            context.getString(R.string.cancel)
        ) { value ->
            prefs.portfolioUrl = value
            snapshot = null
            error = null
            render()
            if (visible) refresh()
        }
    }

    private fun currentUrl(): String = prefs.effectivePortfolioUrl

    private fun refresh() {
        handler.removeCallbacks(refreshRunnable)
        val url = currentUrl()
        if (url.isBlank()) {
            render()
            return
        }
        Bg.run {
            val result = runCatching { PortfolioParser.parse(Net.getString(url)) }
            Bg.post {
                val parsed = result.getOrNull()
                if (parsed != null) {
                    snapshot = parsed
                    error = null
                    if (history.add(Sample(System.currentTimeMillis(), parsed.total))) {
                        prefs.portfolioHistory = history.serialize()
                    }
                } else {
                    error = result.exceptionOrNull()?.message ?: "bilinmeyen hata"
                }
                render()
                if (visible) handler.postDelayed(refreshRunnable, REFRESH_MS)
            }
        }
    }

    private fun cycleRange() {
        val all = HistoryRange.values()
        range = all[(range.ordinal + 1) % all.size]
        renderChange()
    }

    private fun render() {
        val empty = currentUrl().isBlank()
        b.emptyCard.visibility = if (empty) View.VISIBLE else View.GONE
        b.portfolioContent.visibility = if (empty) View.GONE else View.VISIBLE
        if (empty) return

        val s = snapshot
        if (s != null) {
            b.totalValue.text = TrFormat.currency(s.total)
            b.goldDetail.text = TrFormat.amount(s.goldAmount) + " gr · " + TrFormat.price(s.goldPrice) + "/gr"
            b.goldValue.text = TrFormat.currency(s.goldValue)
            b.goldOns.text = onsText(s.goldOunce)
            b.silverDetail.text = TrFormat.amount(s.silverAmount) + " gr · " + TrFormat.price(s.silverPrice) + "/gr"
            b.silverValue.text = TrFormat.currency(s.silverValue)
            b.silverOns.text = onsText(s.silverOunce)
            renderNews(s.news)
        }
        renderChange()
        val err = error
        b.updatedText.text = when {
            err != null -> context.getString(R.string.portfolio_failed, err)
            s != null -> context.getString(R.string.portfolio_updated, s.updatedAt)
            else -> context.getString(R.string.portfolio_loading)
        }
    }

    private fun renderChange() {
        val now = System.currentTimeMillis()
        val change = history.changePercent(range, now)
        val muted = ContextCompat.getColor(context, R.color.muted)
        if (change == null) {
            b.changeText.text = context.getString(R.string.portfolio_collecting)
            b.changeText.setTextColor(muted)
        } else {
            val up = change >= 0
            val label = when (range) {
                HistoryRange.DAY -> R.string.range_day
                HistoryRange.WEEK -> R.string.range_week
                HistoryRange.MONTH -> R.string.range_month
            }
            b.changeText.text = (if (up) "▲ " else "▼ ") + TrFormat.percent(change) + " " + context.getString(label)
            b.changeText.setTextColor(ContextCompat.getColor(context, if (up) R.color.up else R.color.live))
        }
        b.spark.setValues(history.spark(range, now))
        val chips = listOf(b.chipDay, b.chipWeek, b.chipMonth)
        for (i in chips.indices) chips[i].isSelected = i == range.ordinal
    }

    private fun onsText(o: Ounce): CharSequence {
        val price = o.price ?: return ""
        val out = SpannableStringBuilder(context.getString(R.string.portfolio_ons, price))
        val arrow = when (o.direction) {
            Direction.UP -> " ▲"
            Direction.DOWN -> " ▼"
            Direction.NEUTRAL -> ""
        }
        val extra = arrow + (o.changePercent?.let { " $it" } ?: "")
        if (extra.isNotEmpty()) {
            val start = out.length
            out.append(extra)
            val color = when (o.direction) {
                Direction.UP -> ContextCompat.getColor(context, R.color.up)
                Direction.DOWN -> ContextCompat.getColor(context, R.color.live)
                Direction.NEUTRAL -> Color.WHITE
            }
            out.setSpan(ForegroundColorSpan(color), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return out
    }

    private fun renderNews(items: List<NewsItem>) {
        for (i in 0 until NEWS_SLOTS) {
            val nb = newsViews[i]
            val item = items.getOrNull(i)
            if (item == null) {
                // Haber yoksa ilk kartta ipucu, diğerleri gizli.
                nb.root.visibility = if (i == 0) View.VISIBLE else View.INVISIBLE
                nb.newsPill.visibility = View.GONE
                nb.newsLevel.visibility = View.GONE
                nb.newsTime.visibility = View.GONE
                nb.newsTitle.text = if (i == 0) context.getString(R.string.news_none) else ""
                continue
            }
            nb.root.visibility = View.VISIBLE
            nb.newsPill.visibility = View.VISIBLE
            nb.newsLevel.visibility = View.VISIBLE
            nb.newsTime.visibility = View.VISIBLE
            val (label, bg) = when (item.direction) {
                NewsDirection.UP -> R.string.news_up to R.drawable.bg_pill_up
                NewsDirection.DOWN -> R.string.news_down to R.drawable.bg_pill_down
                NewsDirection.MIXED -> R.string.news_mixed to R.drawable.bg_pill_mixed
                NewsDirection.UNCLEAR -> R.string.news_unclear to R.drawable.bg_pill_unclear
            }
            nb.newsPill.setText(label)
            nb.newsPill.setBackgroundResource(bg)
            nb.newsLevel.setText(
                when (item.level) {
                    NewsLevel.HIGH -> R.string.news_level_high
                    NewsLevel.MEDIUM -> R.string.news_level_medium
                    NewsLevel.LOW -> R.string.news_level_low
                }
            )
            nb.newsTitle.text = item.title
            nb.newsTime.text = NewsTime.format(item.timeIso)
        }
    }

    private companion object {
        const val NEWS_SLOTS = 3
        const val REFRESH_MS = 5L * 60 * 1000
    }
}

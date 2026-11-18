package com.tvstarter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.tvstarter.R
import com.tvstarter.data.LiveTv
import com.tvstarter.databinding.PanelLiveBinding

/** LIVE sekmesi: TV'nin kendi canlı TV uygulamasını açan tek kart. */
class LivePanel(context: Context) : FrameLayout(context), TabPanel {
    private val b = PanelLiveBinding.inflate(LayoutInflater.from(context), this, true)

    override val view: View get() = this

    init {
        b.liveCard.focusScale(1.05f)
        b.liveCard.setOnClickListener { openLiveTv() }
    }

    override fun onShow() {}

    override fun onHide() {}

    override fun focusContent(): Boolean = b.liveCard.requestFocus()

    private fun openLiveTv() {
        val intent = LiveTv.intent(context)
        if (intent == null) {
            Toast.makeText(context, R.string.live_not_found, Toast.LENGTH_LONG).show()
            return
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }
}

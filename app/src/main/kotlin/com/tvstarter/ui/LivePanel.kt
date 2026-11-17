package com.tvstarter.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tvstarter.R
import com.tvstarter.data.Bg
import com.tvstarter.data.LiveItem
import com.tvstarter.data.LiveRepo
import com.tvstarter.databinding.ItemChannelBinding
import com.tvstarter.databinding.ItemHeaderBinding
import com.tvstarter.databinding.PanelLiveBinding

/** LIVE sekmesi: canlı TV kanalları ve şu an yayındaki program. Kanal yoksa HDMI gibi girişler. */
class LivePanel(context: Context) : FrameLayout(context), TabPanel {
    private val b = PanelLiveBinding.inflate(LayoutInflater.from(context), this, true)
    private val adapter = LiveAdapter { openItem(it) }
    private var loadedOnce = false

    override val view: View get() = this

    init {
        val manager = GridLayoutManager(context, COLUMNS)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = if (adapter.isHeader(position)) COLUMNS else 1
        }
        b.liveList.layoutManager = manager
        b.liveList.adapter = adapter
        b.liveList.itemAnimator = null
    }

    override fun onShow() {
        // Her gösterimde yenile: program bilgisi ve ilerleme çubuğu eskir.
        if (!loadedOnce) showMessage(context.getString(R.string.live_loading))
        Bg.run {
            val result = LiveRepo.load(context)
            Bg.post {
                loadedOnce = true
                val rows = ArrayList<LiveRow>()
                if (result.channels.isNotEmpty()) {
                    rows.add(LiveRow.Header(context.getString(R.string.live_now)))
                    result.channels.mapTo(rows) { LiveRow.Channel(it) }
                }
                if (result.inputs.isNotEmpty()) {
                    rows.add(LiveRow.Header(context.getString(R.string.live_inputs)))
                    result.inputs.mapTo(rows) { LiveRow.Channel(it) }
                }
                adapter.setRows(rows)
                if (rows.isEmpty()) showMessage(context.getString(R.string.live_empty)) else b.liveMessage.visibility = View.GONE
            }
        }
    }

    override fun onHide() {}

    override fun focusContent(): Boolean {
        val pos = adapter.firstChannelPosition()
        if (pos < 0) return false
        b.liveList.scrollToPosition(pos)
        b.liveList.post { b.liveList.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus() }
        return true
    }

    private fun showMessage(text: String) {
        b.liveMessage.text = text
        b.liveMessage.visibility = View.VISIBLE
    }

    private fun openItem(item: LiveItem) {
        try {
            context.startActivity(item.intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val COLUMNS = 4
    }
}

private sealed class LiveRow {
    class Header(val title: String) : LiveRow()
    class Channel(val item: LiveItem) : LiveRow()
}

private class LiveAdapter(private val onOpen: (LiveItem) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var rows: List<LiveRow> = emptyList()

    fun setRows(newRows: List<LiveRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    fun isHeader(position: Int): Boolean = rows.getOrNull(position) is LiveRow.Header

    fun firstChannelPosition(): Int = rows.indexOfFirst { it is LiveRow.Channel }

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = if (isHeader(position)) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            HeaderHolder(ItemHeaderBinding.inflate(inflater, parent, false))
        } else {
            val holder = ChannelHolder(ItemChannelBinding.inflate(inflater, parent, false))
            holder.itemView.focusScale(1.05f)
            holder
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is LiveRow.Header -> (holder as HeaderHolder).b.headerTitle.text = row.title
            is LiveRow.Channel -> {
                val b = (holder as ChannelHolder).b
                val item = row.item
                val ctx = b.root.context
                b.channelLetter.text = item.title.take(1).uppercase()
                b.channelName.text = item.title
                b.channelProgram.text = item.program
                b.channelNumber.text = item.number
                b.channelNumber.visibility = if (item.number.isBlank()) View.GONE else View.VISIBLE
                // Canlı rozeti ve ilerleme yalnızca program bilgisi olan kanallarda.
                val hasProgram = item.program.isNotBlank()
                b.channelBadge.visibility = if (hasProgram) View.VISIBLE else View.GONE
                b.channelProgress.visibility = if (hasProgram) View.VISIBLE else View.GONE
                b.channelProgress.progress = item.progress
                val art = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(colorFor(item.title), 0xFF0D1B36.toInt())
                )
                art.cornerRadius = ctx.dp(8).toFloat()
                b.channelArt.background = art
                holder.itemView.setOnClickListener { onOpen(item) }
            }
        }
    }

    private fun colorFor(name: String): Int {
        val palette = intArrayOf(
            0xFFB4352F.toInt(), 0xFF2D8A47.toInt(), 0xFF7A3FC4.toInt(), 0xFF2F7FA3.toInt(),
            0xFFC2691F.toInt(), 0xFFC25A7E.toInt(), 0xFF1F8A70.toInt(), 0xFF8A6A1F.toInt()
        )
        return palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    }

    private class HeaderHolder(val b: ItemHeaderBinding) : RecyclerView.ViewHolder(b.root)
    private class ChannelHolder(val b: ItemChannelBinding) : RecyclerView.ViewHolder(b.root)
}

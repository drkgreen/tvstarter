package com.tvstarter.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tvstarter.R
import com.tvstarter.data.AppImage
import com.tvstarter.data.AppRepo
import com.tvstarter.data.Bg
import com.tvstarter.data.LoadedApps
import com.tvstarter.data.Prefs
import com.tvstarter.databinding.ItemAppBinding
import com.tvstarter.databinding.ItemHeaderBinding
import com.tvstarter.databinding.PanelAppsBinding
import com.tvstarter.logic.AppItem
import com.tvstarter.logic.AppListBuilder
import com.tvstarter.logic.AppRow

/** APPS sekmesi: favoriler ve diğer uygulamalar, 6 sütun. Uzun basınca menü açılır. */
class AppsPanel(context: Context) : FrameLayout(context), TabPanel {
    private val b = PanelAppsBinding.inflate(LayoutInflater.from(context), this, true)
    private val prefs = Prefs(context)
    private var loaded: LoadedApps? = null
    private val adapter = AppsAdapter({ launchApp(it) }, { item, favorite -> menu(item, favorite) })

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            reload()
        }
    }

    override val view: View get() = this

    init {
        val manager = GridLayoutManager(context, COLUMNS)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (adapter.isHeader(position)) COLUMNS else 1
        }
        b.appsList.layoutManager = manager
        b.appsList.adapter = adapter
        b.appsList.itemAnimator = null
        b.appsList.setHasFixedSize(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDetachedFromWindow() {
        try {
            context.unregisterReceiver(packageReceiver)
        } catch (e: IllegalArgumentException) {
            // Zaten kayıtlı değil.
        }
        super.onDetachedFromWindow()
    }

    override fun onShow() {
        if (loaded == null) reload()
    }

    override fun onHide() {}

    override fun focusContent(): Boolean {
        val pos = adapter.firstAppPosition()
        if (pos < 0) return false
        b.appsList.scrollToPosition(pos)
        b.appsList.post { b.appsList.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus() }
        return true
    }

    private fun reload() {
        Bg.run {
            val result = AppRepo.load(context)
            Bg.post {
                loaded = result
                rebuild()
            }
        }
    }

    private fun rebuild() {
        val l = loaded ?: return
        val rows = AppListBuilder.build(
            l.items,
            prefs.favorites,
            context.getString(R.string.apps_favorites),
            context.getString(R.string.apps_others),
            context.getString(R.string.apps_all)
        )
        adapter.setData(rows, l.images)
        b.appsEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun launchApp(item: AppItem) {
        try {
            context.startActivity(AppRepo.launchIntent(item))
        } catch (e: Exception) {
            Toast.makeText(context, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun menu(item: AppItem, favorite: Boolean) {
        val toggle = if (favorite) R.string.menu_remove_favorite else R.string.menu_add_favorite
        ActionMenu.show(
            context,
            item.label,
            listOf(
                ActionMenu.Action(context.getString(toggle)) { toggleFavorite(item.packageName, favorite) },
                ActionMenu.Action(context.getString(R.string.menu_app_info)) {
                    startSafely(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${item.packageName}"))
                    )
                },
                ActionMenu.Action(context.getString(R.string.menu_uninstall)) {
                    startSafely(Intent(Intent.ACTION_DELETE, Uri.parse("package:${item.packageName}")))
                }
            )
        )
    }

    private fun toggleFavorite(pkg: String, wasFavorite: Boolean) {
        val set = prefs.favorites.toMutableSet()
        if (wasFavorite) set.remove(pkg) else set.add(pkg)
        prefs.favorites = set
        rebuild()
    }

    private fun startSafely(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val COLUMNS = 6
    }
}

private class AppsAdapter(
    private val onOpen: (AppItem) -> Unit,
    private val onMenu: (AppItem, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<AppRow> = emptyList()
    private var images: Map<String, AppImage> = emptyMap()

    fun setData(newRows: List<AppRow>, newImages: Map<String, AppImage>) {
        rows = newRows
        images = newImages
        notifyDataSetChanged()
    }

    fun isHeader(position: Int): Boolean = rows.getOrNull(position) is AppRow.Header

    fun firstAppPosition(): Int = rows.indexOfFirst { it is AppRow.App }

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = if (isHeader(position)) TYPE_HEADER else TYPE_APP

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemHeaderBinding.inflate(inflater, parent, false))
        } else {
            val holder = AppHolder(ItemAppBinding.inflate(inflater, parent, false))
            holder.itemView.focusScale(1.08f)
            holder
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is AppRow.Header -> (holder as HeaderHolder).b.headerTitle.text = row.title
            is AppRow.App -> {
                val b = (holder as AppHolder).b
                val image = images[row.item.key]
                b.appImage.scaleType = if (image?.isBanner == true) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                b.appImage.setImageDrawable(image?.drawable)
                b.appLabel.text = row.item.label
                b.favStar.visibility = if (row.favorite) View.VISIBLE else View.GONE
                holder.itemView.setOnClickListener { onOpen(row.item) }
                holder.itemView.setOnLongClickListener {
                    onMenu(row.item, row.favorite)
                    true
                }
            }
        }
    }

    private class HeaderHolder(val b: ItemHeaderBinding) : RecyclerView.ViewHolder(b.root)
    private class AppHolder(val b: ItemAppBinding) : RecyclerView.ViewHolder(b.root)

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_APP = 1
    }
}

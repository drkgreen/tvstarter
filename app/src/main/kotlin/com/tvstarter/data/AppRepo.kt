package com.tvstarter.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.tvstarter.logic.AppItem

class AppImage(val drawable: Drawable, val isBanner: Boolean)

class LoadedApps(val items: List<AppItem>, val images: Map<String, AppImage>)

object AppRepo {
    /** Yüklü uygulamaları okur. Arka planda çağır. Önce TV, sonra normal başlatıcı etkinlikleri. */
    fun load(context: Context): LoadedApps {
        val pm = context.packageManager
        val leanback = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0
        )
        val phone = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
        val seenPackages = HashSet<String>()
        val items = ArrayList<AppItem>()
        val images = HashMap<String, AppImage>()

        fun add(ri: android.content.pm.ResolveInfo, leanbackEntry: Boolean) {
            val info = ri.activityInfo ?: return
            if (info.packageName == context.packageName) return
            val item = AppItem(info.packageName, info.name, ri.loadLabel(pm).toString())
            items.add(item)
            val banner = if (leanbackEntry) info.loadBanner(pm) else null
            val drawable = banner ?: ri.loadIcon(pm)
            images[item.key] = AppImage(drawable, banner != null)
        }

        for (ri in leanback) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName) continue
            seenPackages.add(pkg)
            add(ri, true)
        }
        for (ri in phone) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg in seenPackages) continue
            seenPackages.add(pkg)
            add(ri, false)
        }
        return LoadedApps(items, images)
    }

    fun launchIntent(item: AppItem): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(item.packageName, item.activityName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
}

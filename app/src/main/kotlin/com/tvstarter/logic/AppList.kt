package com.tvstarter.logic

import java.text.Collator
import java.util.Locale

data class AppItem(val packageName: String, val activityName: String, val label: String) {
    val key: String get() = "$packageName/$activityName"
}

sealed class AppRow {
    data class Header(val title: String) : AppRow()
    data class App(val item: AppItem, val favorite: Boolean) : AppRow()
}

object AppListBuilder {
    /**
     * Önce favoriler, sonra diğerleri. Her grup Türkçe alfabe sırasıyla.
     * Favori yoksa tek başlık [allTitle] kullanılır.
     */
    fun build(
        items: List<AppItem>,
        favorites: Set<String>,
        favoritesTitle: String,
        othersTitle: String,
        allTitle: String
    ): List<AppRow> {
        val collator = Collator.getInstance(Locale("tr", "TR"))
        val sorted = items.sortedWith(Comparator { a, b -> collator.compare(a.label, b.label) })
        val favs = sorted.filter { it.packageName in favorites }
        val others = sorted.filter { it.packageName !in favorites }
        val rows = ArrayList<AppRow>()
        if (favs.isNotEmpty()) {
            rows.add(AppRow.Header(favoritesTitle))
            favs.mapTo(rows) { AppRow.App(it, true) }
        }
        if (others.isNotEmpty()) {
            rows.add(AppRow.Header(if (favs.isEmpty()) allTitle else othersTitle))
            others.mapTo(rows) { AppRow.App(it, false) }
        }
        return rows
    }
}

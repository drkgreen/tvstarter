package com.tvstarter.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.tvstarter.R
import com.tvstarter.databinding.ActivityMainBinding
import java.util.Locale

/**
 * Tek ekranlı ana ekran: HOME, APPS, LIVE ve PORTFÖY sekmeleri.
 * Odak sistemi standart Android odağıdır. Sekme odak alınca seçilir.
 */
class MainActivity : Activity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var tabs: List<TextView>
    private lateinit var panels: List<TabPanel>
    private lateinit var portfolio: PortfolioPanel
    private lateinit var wallpaper: WallpaperRotator
    private var selected = -1
    private var resumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Arayüz yalnızca Türkçe. Saat ve tarih de Türkçe yazılsın.
        Locale.setDefault(Locale("tr", "TR"))
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        portfolio = PortfolioPanel(this)
        panels = listOf(HomePanel(this), AppsPanel(this), LivePanel(this), portfolio)
        for (p in panels) {
            p.view.visibility = View.GONE
            b.panelHost.addView(p.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        tabs = listOf(b.tabHome, b.tabApps, b.tabLive, b.tabPortfolio)
        tabs.forEachIndexed { i, tab ->
            tab.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) selectTab(i) }
            tab.setOnClickListener {
                selectTab(i)
                panels[i].focusContent()
            }
        }
        b.btnSettings.focusScale(1.1f)
        b.btnSettings.setOnClickListener { showSettingsMenu() }

        wallpaper = WallpaperRotator(b.wallpaperA, b.wallpaperB, b.wallpaperCaption)
        selectTab(0)
        window.decorView.post { b.tabHome.requestFocus() }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        panels[selected].onShow()
        if (selected == HOME) wallpaper.start()
    }

    override fun onPause() {
        resumed = false
        panels[selected].onHide()
        wallpaper.stop()
        super.onPause()
    }

    /** HOME tuşuna basılınca (singleTask) ana sekmeye dön. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            selectTab(HOME)
            tabs[HOME].requestFocus()
        }
    }

    private fun selectTab(index: Int) {
        if (index == selected) return
        if (resumed && selected >= 0) panels[selected].onHide()
        selected = index
        tabs.forEachIndexed { i, tab -> tab.isSelected = i == index }
        panels.forEachIndexed { i, p -> p.view.visibility = if (i == index) View.VISIBLE else View.GONE }

        val home = index == HOME
        b.wallpaperLayer.animate().alpha(if (home) 1f else 0f).setDuration(200).start()
        b.topClock.visibility = if (home) View.INVISIBLE else View.VISIBLE
        if (resumed) {
            panels[index].onShow()
            if (home) wallpaper.start() else wallpaper.stop()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val focus = currentFocus
            if (focus != null) {
                // İçerikte yukarı tuşu, üstte odaklanacak öğe kalmadıysa seçili sekmeye çıkar.
                // Varsayılan odak araması konuma göre başka bir sekmeyi seçerdi.
                if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP && !tabs.contains(focus) && focus !== b.btnSettings) {
                    val next = focus.focusSearch(View.FOCUS_UP)
                    if (next == null || tabs.contains(next) || next === b.btnSettings) {
                        tabs[selected].requestFocus()
                        return true
                    }
                }
                if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && focus === b.btnSettings) {
                    tabs[selected].requestFocus()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /** Geri: içerikten sekmeye, sekmeden HOME'a. HOME'da bir şey yapmaz (başlatıcı kapanmaz). */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        val focus = currentFocus
        if (focus != null && !tabs.contains(focus) && focus !== b.btnSettings) {
            tabs[selected].requestFocus()
        } else if (selected != HOME) {
            tabs[HOME].requestFocus()
        }
    }

    private fun showSettingsMenu() {
        ActionMenu.show(
            this,
            getString(R.string.settings),
            listOf(
                ActionMenu.Action(getString(R.string.settings_wifi)) { startSafely(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                ActionMenu.Action(getString(R.string.settings_system)) { startSafely(Intent(Settings.ACTION_SETTINGS)) },
                ActionMenu.Action(getString(R.string.settings_home)) { startSafely(Intent(Settings.ACTION_HOME_SETTINGS)) },
                ActionMenu.Action(getString(R.string.settings_portfolio_url)) { portfolio.promptUrl() }
            )
        )
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val HOME = 0
    }
}

package com.tvstarter.data

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract

/**
 * TV'nin kendi canlı TV uygulamasını bulur. Kanal listesini biz okumayız:
 * başka uygulamaların kanallarını okumak sistem izni ister ve yüklenen uygulamaya verilmez.
 */
object LiveTv {
    /**
     * Bilinen canlı TV uygulamaları, öncelik sırasıyla. Her paket `AndroidManifest.xml`
     * içindeki `<queries>` listesinde de olmalı (Android 11+ paket görünürlüğü).
     */
    private val PACKAGES = listOf(
        "com.google.android.tv",
        "com.android.tv",
        "org.droidtv.channels",
        "com.sony.dtv.tvx",
        "com.mediatek.wwtv.tvcenter"
    )

    /** Açılacak niyet. Cihazda canlı TV uygulaması bulunamazsa null. */
    fun intent(context: Context): Intent? {
        val pm = context.packageManager
        for (pkg in PACKAGES) {
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (launch != null) return launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Yedek: kanal içeriğini açabilen herhangi bir uygulama.
        val view = Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (view.resolveActivity(pm) != null) view else null
    }
}

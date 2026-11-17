package com.tvstarter.ui

import android.content.Context
import android.view.View

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

/** Odaktayken görünümü büyütür ve öne alır. 120 ms. */
fun View.focusScale(scale: Float = 1.06f) {
    setOnFocusChangeListener { v, hasFocus ->
        val s = if (hasFocus) scale else 1f
        v.animate()
            .scaleX(s)
            .scaleY(s)
            .translationZ(if (hasFocus) 8f else 0f)
            .setDuration(120)
            .start()
    }
}

/** Sekme içeriği. Sekme seçilince [onShow], başka sekmeye geçilince veya uygulama durunca [onHide] çağrılır. */
interface TabPanel {
    val view: View
    fun onShow()
    fun onHide()

    /** İçeriğe odak verir. Odaklanacak öğe yoksa false döner. */
    fun focusContent(): Boolean
}

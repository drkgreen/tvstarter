package com.tvstarter.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tvstarter.R

/** Küçük çizgi grafik: çizgi ve altında dolgu. En az 2 değer gerekir. */
class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val accent = ContextCompat.getColor(context, R.color.accent)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = accent
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePath = Path()
    private val fillPath = Path()
    private var values: List<Double> = emptyList()

    fun setValues(v: List<Double>) {
        values = v
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (values.size < 2) return
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = line.strokeWidth
        val min = values.min()
        val max = values.max()
        val span = if (max - min < 1e-9) 1.0 else max - min

        linePath.reset()
        fillPath.reset()
        for (i in values.indices) {
            val x = w * i / (values.size - 1)
            val y = (h - pad - ((values[i] - min) / span * (h - 2 * pad))).toFloat()
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        fill.shader = LinearGradient(0f, 0f, 0f, h, (accent and 0x00FFFFFF) or 0x55000000, accent and 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawPath(fillPath, fill)
        canvas.drawPath(linePath, line)
    }
}

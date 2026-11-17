package com.tvstarter.ui

import android.app.Dialog
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tvstarter.R

private fun dialogRoot(context: Context): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_dialog)
        setPadding(context.dp(24), context.dp(20), context.dp(24), context.dp(24))
    }

private fun titleView(context: Context, text: String): TextView =
    TextView(context).apply {
        this.text = text
        textSize = 20f
        setTextColor(ContextCompat.getColor(context, R.color.text))
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

private fun buttonView(context: Context, text: String, onClick: () -> Unit): TextView =
    TextView(context).apply {
        this.text = text
        textSize = 17f
        gravity = Gravity.CENTER_VERTICAL
        setTextColor(ContextCompat.getColor(context, R.color.text))
        setBackgroundResource(R.drawable.bg_menu_item)
        setPadding(context.dp(16), context.dp(12), context.dp(16), context.dp(12))
        isFocusable = true
        isClickable = true
        setOnClickListener { onClick() }
    }

private fun presentDialog(dialog: Dialog, root: LinearLayout, widthDp: Int, context: Context) {
    dialog.setContentView(root)
    dialog.window?.setLayout(context.dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT)
    dialog.show()
}

/** Başlık ve odaklanabilir eylem listesi. AlertDialog yerine tutarlı görünüm için özel pencere. */
object ActionMenu {
    class Action(val label: String, val onSelect: () -> Unit)

    fun show(context: Context, title: String, actions: List<Action>) {
        val dialog = Dialog(context, R.style.TvDialog)
        val root = dialogRoot(context)
        root.addView(titleView(context, title))
        var first: TextView? = null
        for (a in actions) {
            val item = buttonView(context, a.label) {
                dialog.dismiss()
                a.onSelect()
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = context.dp(10)
            root.addView(item, lp)
            if (first == null) first = item
        }
        presentDialog(dialog, root, 420, context)
        first?.requestFocus()
    }
}

/** Tek satır metin girişi. TV ekran klavyesi açılır. */
object TextPrompt {
    fun show(
        context: Context,
        title: String,
        hint: String,
        initial: String,
        okLabel: String,
        cancelLabel: String,
        onOk: (String) -> Unit
    ) {
        val dialog = Dialog(context, R.style.TvDialog)
        val root = dialogRoot(context)
        root.addView(titleView(context, title))

        val input = EditText(context).apply {
            setText(initial)
            this.hint = hint
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_DONE
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text))
            setHintTextColor(ContextCompat.getColor(context, R.color.muted))
            setBackgroundResource(R.drawable.bg_input)
            setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.dismiss()
                onOk(input.text.toString())
                true
            } else {
                false
            }
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = context.dp(14)
        root.addView(input, lp)

        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val save = buttonView(context, okLabel) {
            dialog.dismiss()
            onOk(input.text.toString())
        }
        val cancel = buttonView(context, cancelLabel) { dialog.dismiss() }
        val half = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        half.marginEnd = context.dp(8)
        row.addView(save, half)
        row.addView(cancel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val rowLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rowLp.topMargin = context.dp(14)
        root.addView(row, rowLp)

        presentDialog(dialog, root, 560, context)
        input.requestFocus()
    }
}

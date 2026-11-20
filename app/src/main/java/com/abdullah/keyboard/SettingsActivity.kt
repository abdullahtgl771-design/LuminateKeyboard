package com.abdullah.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx: Context = this
        val d = resources.displayMetrics.density
        val pad = (20 * d).toInt()

        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(pad, pad * 2, pad, pad)

        fun title(text: String, size: Float): TextView {
            val tv = TextView(ctx)
            tv.text = text
            tv.textSize = size
            tv.setPadding(0, (16 * d).toInt(), 0, (8 * d).toInt())
            return tv
        }

        fun btn(text: String, onClick: () -> Unit): Button {
            val b = Button(ctx)
            b.text = text
            b.setOnClickListener { onClick() }
            return b
        }

        root.addView(title(getString(R.string.app_name), 26f))

        root.addView(title("Setup", 18f))
        root.addView(btn("1. Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(btn("2. Select keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        val test = EditText(ctx)
        test.hint = "3. Test here"
        root.addView(test)

        root.addView(title("Theme", 18f))
        val group = RadioGroup(ctx)
        val cur = Prefs.theme(ctx)
        val opts = listOf("system" to "System default", "light" to "Light", "dark" to "Dark")
        for ((key, text) in opts) {
            val rb = RadioButton(ctx)
            rb.id = View.generateViewId()
            rb.text = text
            rb.tag = key
            rb.isChecked = key == cur
            group.addView(rb)
        }
        group.setOnCheckedChangeListener { g, id ->
            val rb = g.findViewById<RadioButton>(id)
            if (rb != null) Prefs.setTheme(ctx, rb.tag as String)
        }
        root.addView(group)

        val glass = Switch(ctx)
        glass.text = "Liquid Glass theme"
        glass.isChecked = Prefs.glass(ctx)
        glass.setOnCheckedChangeListener { _, checked -> Prefs.setGlass(ctx, checked) }
        glass.setPadding(0, (12 * d).toInt(), 0, (12 * d).toInt())
        root.addView(glass)

        val sv = ScrollView(ctx)
        sv.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(sv)
    }
}

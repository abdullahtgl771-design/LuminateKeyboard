package com.abdullah.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

enum class Panel { TOOLBOX, EMOJI, CLIPBOARD, TEXTEDIT, RESIZE, THEME }

interface PanelHost {
    fun commit(text: String)
    fun sendKey(code: Int)
    fun contextAction(id: Int)
    fun closePanel()
    fun openPanel(p: Panel)
    fun clipItems(): List<String>
    fun clearClips()
    fun toggleOneHand()
    fun setScale(f: Float)
    fun share()
}

/** Rounded pill button used by all panels. */
class PillButton(
    c: Context,
    private val t: Theme,
    private val icon: IconId?,
    private val label: String,
    private val onClick: () -> Unit
) : View(c) {

    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f * d }
    private val r = RectF()
    private var down = false

    var active: Boolean = false
        set(v) {
            field = v
            invalidate()
        }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val h = MeasureSpec.getSize(heightSpec)
        setMeasuredDimension(
            MeasureSpec.getSize(widthSpec),
            if (h > 0) h else (48f * d).toInt()
        )
    }

    override fun onDraw(c: Canvas) {
        r.set(0f, 0f, width.toFloat(), height.toFloat())
        val rad = height / 2f
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = when {
            down -> t.pressed
            active -> t.accentContainer
            else -> t.key
        }
        c.drawRoundRect(r, rad, rad, p)
        if (t.glass) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f * d
            p.color = t.keyStroke
            c.drawRoundRect(r, rad, rad, p)
            p.style = Paint.Style.FILL
        }
        val col = if (active) t.onAccentContainer else t.text
        tp.color = col
        val baseline = height / 2f - (tp.descent() + tp.ascent()) / 2f
        if (icon != null) {
            val x = 20f * d
            Icons.draw(c, icon, x + 12f * d, height / 2f, 24f * d, col, p)
            tp.textAlign = Paint.Align.LEFT
            c.drawText(label, x + 24f * d + 14f * d, baseline, tp)
        } else {
            tp.textAlign = Paint.Align.CENTER
            c.drawText(label, width / 2f, baseline, tp)
        }
    }

    private fun inside(e: MotionEvent) = e.x in 0f..width.toFloat() && e.y in 0f..height.toFloat()

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = true
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> if (down && !inside(e)) {
                down = false
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val was = down
                down = false
                invalidate()
                if (was && inside(e)) onClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                down = false
                invalidate()
            }
        }
        return true
    }
}

object Panels {

    private val EMOJIS = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
        "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥳", "😏", "😒",
        "😞", "😔", "😟", "😕", "🙁", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬",
        "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗", "🤔", "🤭", "🤫", "😶", "😐", "😑",
        "😬", "🙄", "😯", "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐", "🥴", "🤢", "🤮",
        "🤧", "😷", "🤒", "🤕", "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇",
        "👋", "🤚", "✋", "🖖", "👏", "🙌", "🤝", "🙏", "💪", "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤",
        "🤍", "💔", "💕", "💖", "💗", "💘", "💝", "🔥", "✨", "⭐", "🌟", "💫", "🎉", "🎊", "💯", "✅",
        "❌", "⚡", "☀️", "🌙", "🌈", "☁️", "❄️", "🌸", "🌹", "🍀", "🎮", "🎧", "🎵", "📱", "💻", "🚀"
    )

    fun build(p: Panel, ctx: Context, t: Theme, h: PanelHost): View = when (p) {
        Panel.TOOLBOX -> toolbox(ctx, t, h)
        Panel.EMOJI -> emoji(ctx, t, h)
        Panel.CLIPBOARD -> clipboard(ctx, t, h)
        Panel.TEXTEDIT -> textEdit(ctx, t, h)
        Panel.RESIZE -> resize(ctx, t, h)
        Panel.THEME -> themePanel(ctx, t, h)
    }

    // ---------- helpers ----------
    private fun dp(ctx: Context, v: Float): Int = (v * ctx.resources.displayMetrics.density).toInt()

    private fun row(ctx: Context, vararg views: View): LinearLayout {
        val m = dp(ctx, 4f)
        val ll = LinearLayout(ctx)
        ll.orientation = LinearLayout.HORIZONTAL
        for (v in views) {
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            lp.setMargins(m, m, m, m)
            ll.addView(v, lp)
        }
        return ll
    }

    private fun column(ctx: Context, vararg rows: View): LinearLayout {
        val ll = LinearLayout(ctx)
        ll.orientation = LinearLayout.VERTICAL
        ll.setPadding(dp(ctx, 10f), dp(ctx, 4f), dp(ctx, 10f), dp(ctx, 4f))
        for (v in rows) {
            ll.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        return ll
    }

    private fun label(ctx: Context, t: Theme, text: String, sizeSp: Float = 16f): TextView {
        val tv = TextView(ctx)
        tv.text = text
        tv.setTextColor(t.text)
        tv.textSize = sizeSp
        tv.gravity = Gravity.CENTER
        return tv
    }

    // ---------- toolbox ----------
    private fun toolbox(ctx: Context, t: Theme, h: PanelHost): View {
        val oneHand = PillButton(ctx, t, IconId.ONEHAND, "One-handed") { h.toggleOneHand() }
        oneHand.active = Prefs.oneHand(ctx) != "off"
        return column(
            ctx,
            row(
                ctx,
                PillButton(ctx, t, IconId.EMOJI, "Emoji") { h.openPanel(Panel.EMOJI) },
                PillButton(ctx, t, IconId.CLIPBOARD, "Clipboard") { h.openPanel(Panel.CLIPBOARD) }
            ),
            row(
                ctx,
                oneHand,
                PillButton(ctx, t, IconId.TEXTEDIT, "Text editing") { h.openPanel(Panel.TEXTEDIT) }
            ),
            row(
                ctx,
                PillButton(ctx, t, IconId.RESIZE, "Resize") { h.openPanel(Panel.RESIZE) },
                PillButton(ctx, t, IconId.SHARE, "Share") { h.share() }
            )
        )
    }

    // ---------- emoji ----------
    private fun emoji(ctx: Context, t: Theme, h: PanelHost): View {
        val sv = ScrollView(ctx)
        val list = LinearLayout(ctx)
        list.orientation = LinearLayout.VERTICAL
        list.setPadding(dp(ctx, 6f), dp(ctx, 4f), dp(ctx, 6f), dp(ctx, 8f))
        val perRow = 8
        EMOJIS.chunked(perRow).forEach { chunk ->
            val rowView = LinearLayout(ctx)
            rowView.orientation = LinearLayout.HORIZONTAL
            for (i in 0 until perRow) {
                val tv = TextView(ctx)
                tv.gravity = Gravity.CENTER
                tv.textSize = 24f
                if (i < chunk.size) {
                    val e = chunk[i]
                    tv.text = e
                    tv.setOnClickListener {
                        tv.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        h.commit(e)
                    }
                }
                rowView.addView(tv, LinearLayout.LayoutParams(0, dp(ctx, 46f), 1f))
            }
            list.addView(rowView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        sv.addView(list)
        return sv
    }

    // ---------- clipboard ----------
    private fun clipboard(ctx: Context, t: Theme, h: PanelHost): View {
        val items = h.clipItems()
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(ctx, 12f), dp(ctx, 4f), dp(ctx, 12f), dp(ctx, 4f))

        val header = LinearLayout(ctx)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val title = label(ctx, t, "Clipboard")
        title.gravity = Gravity.CENTER_VERTICAL
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(
            PillButton(ctx, t, null, "Clear") {
                h.clearClips()
                h.openPanel(Panel.CLIPBOARD)
            },
            LinearLayout.LayoutParams(dp(ctx, 90f), dp(ctx, 38f))
        )
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 42f)))

        if (items.isEmpty()) {
            val tv = label(ctx, t, "Copy something and it shows up here", 14f)
            tv.setTextColor(t.hint)
            root.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        } else {
            val sv = ScrollView(ctx)
            val list = LinearLayout(ctx)
            list.orientation = LinearLayout.VERTICAL
            for (s in items) {
                val tv = TextView(ctx)
                tv.text = s
                tv.setTextColor(t.text)
                tv.textSize = 15f
                tv.maxLines = 2
                tv.ellipsize = android.text.TextUtils.TruncateAt.END
                tv.setPadding(dp(ctx, 14f), dp(ctx, 10f), dp(ctx, 14f), dp(ctx, 10f))
                val bg = GradientDrawable()
                bg.setColor(t.key)
                bg.cornerRadius = dp(ctx, 14f).toFloat()
                if (t.glass) bg.setStroke(dp(ctx, 1f), t.keyStroke)
                tv.background = bg
                tv.setOnClickListener {
                    tv.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    h.commit(s)
                }
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(ctx, 6f)
                list.addView(tv, lp)
            }
            sv.addView(list)
            root.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        return root
    }

    // ---------- text editing ----------
    private fun textEdit(ctx: Context, t: Theme, h: PanelHost): View {
        fun arrow(txt: String, code: Int) = PillButton(ctx, t, null, txt) { h.sendKey(code) }
        fun act(txt: String, id: Int) = PillButton(ctx, t, null, txt) { h.contextAction(id) }

        val left = column(
            ctx,
            row(ctx, View(ctx), arrow("\u25B2", KeyEvent.KEYCODE_DPAD_UP), View(ctx)),
            row(
                ctx,
                arrow("\u25C0", KeyEvent.KEYCODE_DPAD_LEFT), View(ctx),
                arrow("\u25B6", KeyEvent.KEYCODE_DPAD_RIGHT)
            ),
            row(ctx, View(ctx), arrow("\u25BC", KeyEvent.KEYCODE_DPAD_DOWN), View(ctx))
        )
        val right = column(
            ctx,
            row(ctx, act("Select all", android.R.id.selectAll), act("Copy", android.R.id.copy)),
            row(ctx, act("Cut", android.R.id.cut), act("Paste", android.R.id.paste))
        )
        val outer = LinearLayout(ctx)
        outer.orientation = LinearLayout.HORIZONTAL
        outer.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        outer.addView(right, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        return outer
    }

    // ---------- resize ----------
    private fun resize(ctx: Context, t: Theme, h: PanelHost): View {
        var s = Prefs.scale(ctx)
        fun txt(v: Float) = "Keyboard height: ${Math.round(v * 100)}%"
        val title = label(ctx, t, txt(s))
        fun update(n: Float) {
            s = n.coerceIn(0.8f, 1.3f)
            h.setScale(s)
            title.text = txt(s)
        }
        return column(
            ctx,
            title,
            row(
                ctx,
                PillButton(ctx, t, null, "Smaller") { update(s - 0.1f) },
                PillButton(ctx, t, null, "Bigger") { update(s + 0.1f) }
            ),
            row(
                ctx,
                PillButton(ctx, t, null, "Reset") { update(1f) },
                PillButton(ctx, t, null, "Done") { h.closePanel() }
            )
        )
    }

    // ---------- theme ----------
    private fun themePanel(ctx: Context, t: Theme, h: PanelHost): View {
        val cur = Prefs.theme(ctx)
        val glass = Prefs.glass(ctx)
        fun opt(text: String, key: String): PillButton {
            val b = PillButton(ctx, t, null, text) { Prefs.setTheme(ctx, key) }
            b.active = cur == key
            return b
        }
        val glassBtn = PillButton(ctx, t, IconId.PALETTE, "Liquid Glass: " + if (glass) "On" else "Off") {
            Prefs.setGlass(ctx, !glass)
        }
        glassBtn.active = glass
        return column(
            ctx,
            label(ctx, t, "Theme"),
            row(ctx, opt("Light", "light"), opt("Dark", "dark"), opt("System", "system")),
            row(ctx, glassBtn)
        )
    }
}

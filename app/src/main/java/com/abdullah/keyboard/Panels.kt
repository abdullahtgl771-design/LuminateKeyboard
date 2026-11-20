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

class PillButton(c: Context, private val t: Theme, private val icon: IconId?, private val label: String, private val onClick: () -> Unit) : View(c) {
    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f * d }
    private val r = RectF()
    private var down = false
    var active = false
        set(v) { field = v; invalidate() }
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val h = MeasureSpec.getSize(heightSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), if (h > 0) h else (48f * d).toInt())
    }
    override fun onDraw(c: Canvas) {
        r.set(0f, 0f, width.toFloat(), height.toFloat())
        p.shader = null; p.style = Paint.Style.FILL
        p.color = when { down -> t.pressed; active -> t.accentContainer; else -> t.key }
        c.drawRoundRect(r, height / 2f, height / 2f, p)
        if (t.glass) { p.style = Paint.Style.STROKE; p.strokeWidth = d; p.color = t.keyStroke; c.drawRoundRect(r, height / 2f, height / 2f, p); p.style = Paint.Style.FILL }
        val col = if (active) t.onAccentContainer else t.text
        tp.color = col
        val baseline = height / 2f - (tp.descent() + tp.ascent()) / 2f
        if (icon != null) { Icons.draw(c, icon, 32f * d, height / 2f, 24f * d, col, p); tp.textAlign = Paint.Align.LEFT; c.drawText(label, 52f * d, baseline, tp) }
        else { tp.textAlign = Paint.Align.CENTER; c.drawText(label, width / 2f, baseline, tp) }
    }
    private fun inside(e: MotionEvent) = e.x in 0f..width.toFloat() && e.y in 0f..height.toFloat()
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { down = true; performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate() }
            MotionEvent.ACTION_MOVE -> if (down && !inside(e)) { down = false; invalidate() }
            MotionEvent.ACTION_UP -> { val was = down; down = false; invalidate(); if (was && inside(e)) onClick() }
            MotionEvent.ACTION_CANCEL -> { down = false; invalidate() }
        }
        return true
    }
}

object Panels {
    private val EMOJIS = listOf("😀","😃","😄","😁","😆","😅","😂","🤣","😊","😇","🙂","🙃","😉","😍","🥰","😘","😋","😛","😜","🤪","😎","🥳","😔","😢","😭","😤","😡","🤯","😳","😱","🤔","😴","👍","👎","👌","✌️","🤞","🤟","👋","👏","🙌","🙏","💪","❤️","🧡","💛","💚","💙","💜","🖤","🤍","💔","💕","🔥","✨","⭐","🌟","🎉","💯","✅","❌","⚡","☀️","🌙","🌈","🌸","🌹","🍀","🎮","🎧","🎵","📱","💻","🚀")
    fun build(p: Panel, ctx: Context, t: Theme, h: PanelHost): View = when (p) {
        Panel.TOOLBOX -> toolbox(ctx,t,h)
        Panel.EMOJI -> emoji(ctx,t,h)
        Panel.CLIPBOARD -> clipboard(ctx,t,h)
        Panel.TEXTEDIT -> textEdit(ctx,t,h)
        Panel.RESIZE -> resize(ctx,t,h)
        Panel.THEME -> themePanel(ctx,t,h)
    }
    private fun dp(ctx: Context, v: Float) = (v * ctx.resources.displayMetrics.density).toInt()
    private fun row(ctx: Context, vararg views: View): LinearLayout {
        val ll = LinearLayout(ctx); ll.orientation = LinearLayout.HORIZONTAL
        views.forEach { v -> val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f); lp.setMargins(dp(ctx,4f),dp(ctx,4f),dp(ctx,4f),dp(ctx,4f)); ll.addView(v,lp) }
        return ll
    }
    private fun column(ctx: Context, vararg rows: View): LinearLayout {
        val ll = LinearLayout(ctx); ll.orientation = LinearLayout.VERTICAL; ll.setPadding(dp(ctx,10f),dp(ctx,4f),dp(ctx,10f),dp(ctx,4f))
        rows.forEach { ll.addView(it,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f)) }
        return ll
    }
    private fun label(ctx: Context,t: Theme,s: String,size: Float=16f): TextView {
        return TextView(ctx).apply { text=s; setTextColor(t.text); textSize=size; gravity=Gravity.CENTER }
    }
    private fun toolbox(ctx: Context,t: Theme,h: PanelHost): View {
        val one = PillButton(ctx,t,IconId.ONEHAND,"One-handed") { h.toggleOneHand() }.apply { active=Prefs.oneHand(ctx)!="off" }
        return column(ctx,
            row(ctx,PillButton(ctx,t,IconId.EMOJI,"Emoji"){h.openPanel(Panel.EMOJI)},PillButton(ctx,t,IconId.CLIPBOARD,"Clipboard"){h.openPanel(Panel.CLIPBOARD)}),
            row(ctx,one,PillButton(ctx,t,IconId.TEXTEDIT,"Text editing"){h.openPanel(Panel.TEXTEDIT)}),
            row(ctx,PillButton(ctx,t,IconId.RESIZE,"Resize"){h.openPanel(Panel.RESIZE)},PillButton(ctx,t,IconId.SHARE,"Share"){h.share()}))
    }
    private fun emoji(ctx: Context,t: Theme,h: PanelHost): View {
        val sv=ScrollView(ctx); val list=LinearLayout(ctx).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(ctx,6f),dp(ctx,4f),dp(ctx,6f),dp(ctx,8f)) }
        EMOJIS.chunked(8).forEach { chunk ->
            val rr=LinearLayout(ctx).apply { orientation=LinearLayout.HORIZONTAL }
            for(i in 0 until 8) {
                val tv=TextView(ctx).apply { gravity=Gravity.CENTER; textSize=24f }
                if(i<chunk.size) { val e=chunk[i]; tv.text=e; tv.setOnClickListener { tv.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); h.commit(e) } }
                rr.addView(tv,LinearLayout.LayoutParams(0,dp(ctx,46f),1f))
            }
            list.addView(rr,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        sv.addView(list); return sv
    }
    private fun clipboard(ctx: Context,t: Theme,h: PanelHost): View {
        val root=LinearLayout(ctx).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(ctx,12f),dp(ctx,4f),dp(ctx,12f),dp(ctx,4f)) }
        val header=LinearLayout(ctx).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        val title=label(ctx,t,"Clipboard").apply { gravity=Gravity.CENTER_VERTICAL }
        header.addView(title,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
        header.addView(PillButton(ctx,t,null,"Clear"){h.clearClips();h.openPanel(Panel.CLIPBOARD)},LinearLayout.LayoutParams(dp(ctx,90f),dp(ctx,38f)))
        root.addView(header,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(ctx,42f)))
        val items=h.clipItems()
        if(items.isEmpty()) root.addView(label(ctx,t,"Copy something and it shows up here",14f).apply { setTextColor(t.hint) },LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        else {
            val sv=ScrollView(ctx); val list=LinearLayout(ctx).apply { orientation=LinearLayout.VERTICAL }
            items.forEach { s ->
                val tv=TextView(ctx).apply {
                    text=s; setTextColor(t.text); textSize=15f; maxLines=2; ellipsize=android.text.TextUtils.TruncateAt.END
                    setPadding(dp(ctx,14f),dp(ctx,10f),dp(ctx,14f),dp(ctx,10f))
                    background=GradientDrawable().apply { setColor(t.key); cornerRadius=dp(ctx,14f).toFloat(); if(t.glass) setStroke(dp(ctx,1f),t.keyStroke) }
                    setOnClickListener { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); h.commit(s) }
                }
                val lp=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.bottomMargin=dp(ctx,6f); list.addView(tv,lp)
            }
            sv.addView(list); root.addView(sv,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        }
        return root
    }
    private fun textEdit(ctx: Context,t: Theme,h: PanelHost): View {
        fun arrow(s:String,code:Int)=PillButton(ctx,t,null,s){h.sendKey(code)}
        fun act(s:String,id:Int)=PillButton(ctx,t,null,s){h.contextAction(id)}
        val left=column(ctx,row(ctx,View(ctx),arrow("▲",KeyEvent.KEYCODE_DPAD_UP),View(ctx)),row(ctx,arrow("◀",KeyEvent.KEYCODE_DPAD_LEFT),View(ctx),arrow("▶",KeyEvent.KEYCODE_DPAD_RIGHT)),row(ctx,View(ctx),arrow("▼",KeyEvent.KEYCODE_DPAD_DOWN),View(ctx)))
        val right=column(ctx,row(ctx,act("Select all",android.R.id.selectAll),act("Copy",android.R.id.copy)),row(ctx,act("Cut",android.R.id.cut),act("Paste",android.R.id.paste)))
        return LinearLayout(ctx).apply { orientation=LinearLayout.HORIZONTAL; addView(left,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f)); addView(right,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f)) }
    }
    private fun resize(ctx: Context,t: Theme,h: PanelHost): View {
        var s=Prefs.scale(ctx); fun txt(v:Float)="Keyboard height: ${Math.round(v*100)}%"
        val title=label(ctx,t,txt(s))
        fun update(n:Float){s=n.coerceIn(0.8f,1.3f);h.setScale(s);title.text=txt(s)}
        return column(ctx,title,row(ctx,PillButton(ctx,t,null,"Smaller"){update(s-0.1f)},PillButton(ctx,t,null,"Bigger"){update(s+0.1f)}),row(ctx,PillButton(ctx,t,null,"Reset"){update(1f)},PillButton(ctx,t,null,"Done"){h.closePanel()}))
    }
    private fun themePanel(ctx: Context,t: Theme,h: PanelHost): View {
        val cur=Prefs.theme(ctx); val glass=Prefs.glass(ctx)
        fun opt(text:String,key:String)=PillButton(ctx,t,null,text){Prefs.setTheme(ctx,key)}.apply { active=cur==key }
        val gb=PillButton(ctx,t,IconId.PALETTE,"Liquid Glass: "+if(glass)"On" else "Off"){Prefs.setGlass(ctx,!glass)}.apply { active=glass }
        return column(ctx,label(ctx,t,"Theme"),row(ctx,opt("Light","light"),opt("Dark","dark"),opt("System","system")),row(ctx,gb))
    }
}

package com.abdullah.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class ToolbarView(context: Context, private val onTool: (Tool) -> Unit) : View(context) {
    enum class Tool { TOOLBOX, EMOJI, CLIPBOARD, TRANSLATE, SETTINGS, THEME, MIC }
    private val d = resources.displayMetrics.density
    private val items = listOf(Tool.TOOLBOX to IconId.GRID, Tool.EMOJI to IconId.EMOJI, Tool.CLIPBOARD to IconId.CLIPBOARD, Tool.TRANSLATE to IconId.TRANSLATE, Tool.SETTINGS to IconId.SETTINGS, Tool.THEME to IconId.PALETTE, Tool.MIC to IconId.MIC)
    var theme: Theme = Themes.resolve(context)
        set(v) { field = v; invalidate() }
    var closeMode: Boolean = false
        set(v) { field = v; invalidate() }
    private var down = -1
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(widthSpec: Int, heightSpec: Int) { setMeasuredDimension(MeasureSpec.getSize(widthSpec), (52f * d).toInt()) }
    override fun onDraw(c: Canvas) {
        val cell = width / items.size.toFloat()
        val cy = height / 2f
        items.forEachIndexed { i, pair ->
            val cx = cell * i + cell / 2f
            val first = i == 0
            val r = 21f * d
            paint.shader = null; paint.style = Paint.Style.FILL
            when {
                i == down -> { paint.color = theme.pressed; c.drawCircle(cx, cy, r, paint) }
                first && closeMode -> { paint.color = theme.accentContainer; c.drawCircle(cx, cy, r, paint) }
                pair.first == Tool.MIC -> { paint.color = theme.special; c.drawCircle(cx, cy, r, paint) }
            }
            val icon = if (first && closeMode) IconId.CLOSE else pair.second
            val col = if (first && closeMode) theme.onAccentContainer else theme.text
            Icons.draw(c, icon, cx, cy, 24f * d, col, paint)
        }
    }
    private fun indexAt(x: Float): Int = (x / (width / items.size.toFloat())).toInt().coerceIn(0, items.size - 1)
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { down = indexAt(e.x); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate() }
            MotionEvent.ACTION_UP -> {
                val i = indexAt(e.x); val was = down; down = -1; invalidate()
                if (was == i) onTool(items[i].first)
            }
            MotionEvent.ACTION_CANCEL -> { down = -1; invalidate() }
        }
        return true
    }
}

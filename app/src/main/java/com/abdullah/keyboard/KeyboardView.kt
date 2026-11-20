package com.abdullah.keyboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.max
import kotlin.math.min

class KeyboardView(context: Context, private val listener: Listener) : View(context) {
    interface Listener {
        fun onText(text: String)
        fun onDelete()
        fun onEnter()
        fun onGlobe()
    }
    enum class Shift { OFF, ON, CAPS }
    private class Slot(val key: Key, val rect: RectF, val hl: Shader?)
    private val d = resources.displayMetrics.density
    private var hScale = 1f
    private val rowHeight: Float get() = 54f * d * hScale
    private val padH = 3f * d
    private val padV = 6f * d
    private val gap = 4f * d
    private val radius = 10f * d
    private var theme: Theme = Themes.resolve(context)
    private var layer = Layer.LETTERS
    private var shift = Shift.OFF
    private var enterLabel = "\u21B5"
    private var slots: List<Slot> = emptyList()
    private var pressed: Slot? = null
    private var lastShiftTap = 0L
    private var longFired = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private var popupSlot: Slot? = null
    private var popupLabel = ""
    private var popupAnim = 0f
    private var animator: ValueAnimator? = null
    private val popupPath = Path()
    private val bubbleRect = RectF()
    private val neckRect = RectF()
    private val uiHandler = Handler(Looper.getMainLooper())
    private val repeat = object : Runnable {
        override fun run() { listener.onDelete(); uiHandler.postDelayed(this, 45) }
    }
    private val longPress = Runnable {
        val p = pressed
        val h = p?.key?.hint
        if (p != null && h != null) {
            longFired = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            popupLabel = h
            invalidate()
            listener.onText(h)
        }
    }
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), (4 * rowHeight + 2 * padV).toInt())
    }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); buildSlots() }
    private fun buildSlots() {
        val list = ArrayList<Slot>()
        val usableW = width - 2 * padH
        val rh = rowHeight
        Layouts.rows(layer).forEachIndexed { r, row ->
            val total = row.sumOf { it.weight.toDouble() }.toFloat()
            val unit = usableW / total
            val top = padV + r * rh
            var x = padH
            for (k in row) {
                val w = unit * k.weight
                if (k.type != KeyType.SPACER) {
                    val rect = RectF(x + gap / 2, top + gap / 2, x + w - gap / 2, top + rh - gap / 2)
                    val hl: Shader? = if (theme.glass) LinearGradient(0f, rect.top, 0f, rect.bottom,
                        if (theme.dark) 0x40FFFFFF else 0x99FFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP) else null
                    list.add(Slot(k, rect, hl))
                }
                x += w
            }
        }
        slots = list
    }
    fun applyTheme(t: Theme) { theme = t; buildSlots(); invalidate() }
    fun setHeightScale(s: Float) { if (s == hScale) return; hScale = s; requestLayout(); buildSlots(); invalidate() }
    fun setEnterLabel(s: String) { enterLabel = s; invalidate() }
    fun setLayer(l: Layer) { layer = l; buildSlots(); invalidate() }
    fun reset() = setLayer(Layer.LETTERS)
    fun setAutoShift(on: Boolean) {
        if (shift == Shift.CAPS) return
        val n = if (on) Shift.ON else Shift.OFF
        if (n != shift) { shift = n; invalidate() }
    }
    private fun labelFor(k: Key): String? = when (k.type) {
        KeyType.CHAR -> if (layer == Layer.LETTERS && shift != Shift.OFF) k.label.uppercase() else k.label
        KeyType.SHIFT -> "\u21E7"
        KeyType.DEL -> "\u232B"
        KeyType.ENTER -> enterLabel
        KeyType.MODE_SYM -> "?123"
        KeyType.MODE_ABC -> "ABC"
        else -> null
    }
    private fun drawCentered(c: Canvas, text: String, rect: RectF) {
        val cy = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        c.drawText(text, rect.centerX(), cy, textPaint)
    }
    override fun onDraw(canvas: Canvas) {
        for (s in slots) {
            val k = s.key
            val fill = when {
                s === pressed -> theme.pressed
                k.type == KeyType.ENTER -> theme.accent
                k.type == KeyType.SHIFT && shift == Shift.CAPS -> theme.accent
                k.type.special -> theme.special
                else -> theme.key
            }
            val r = when (k.type) { KeyType.ENTER, KeyType.MODE_SYM, KeyType.MODE_ABC -> s.rect.height() / 2f; else -> radius }
            paint.shader = null; paint.style = Paint.Style.FILL; paint.color = fill
            canvas.drawRoundRect(s.rect, r, r, paint)
            if (theme.glass) {
                paint.shader = s.hl; paint.color = Color.WHITE; canvas.drawRoundRect(s.rect, r, r, paint)
                paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f * d
                paint.color = theme.keyStroke; canvas.drawRoundRect(s.rect, r, r, paint); paint.style = Paint.Style.FILL
            }
            when (k.type) {
                KeyType.GLOBE -> Icons.draw(canvas, IconId.GLOBE, s.rect.centerX(), s.rect.centerY(), 22f * d, theme.text, paint)
                KeyType.SPACE -> { textPaint.color = theme.hint; textPaint.textSize = 15f * d; drawCentered(canvas, "English", s.rect) }
                else -> {
                    val label = labelFor(k)
                    if (label != null) {
                        textPaint.color = when {
                            k.type == KeyType.ENTER -> theme.onAccent
                            k.type == KeyType.SHIFT && shift == Shift.CAPS -> theme.onAccent
                            k.type == KeyType.SHIFT && shift == Shift.ON -> theme.accent
                            else -> theme.text
                        }
                        textPaint.textSize = when (k.type) { KeyType.MODE_SYM, KeyType.MODE_ABC -> 15f * d; KeyType.ENTER -> 26f * d; KeyType.SHIFT -> 24f * d; else -> 22f * d }
                        drawCentered(canvas, label, s.rect)
                    }
                }
            }
            val hint = k.hint
            if (hint != null && layer == Layer.LETTERS) {
                hintPaint.color = theme.hint; hintPaint.textSize = 11f * d
                canvas.drawText(hint, s.rect.right - 7f * d, s.rect.top + 15f * d, hintPaint)
            }
        }
        drawPopup(canvas)
    }
    private fun showPopup(s: Slot) { animator?.cancel(); popupSlot = s; popupLabel = labelFor(s.key) ?: ""; animateTo(1f, 120, OvershootInterpolator(1.8f)) }
    private fun hidePopup() { animateTo(0f, 140, DecelerateInterpolator()) }
    private fun animateTo(target: Float, dur: Long, interp: TimeInterpolator) {
        animator?.cancel()
        val a = ValueAnimator.ofFloat(popupAnim, target); a.duration = dur; a.interpolator = interp
        a.addUpdateListener { popupAnim = it.animatedValue as Float; invalidate() }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) { if (target == 0f && popupAnim <= 0.02f) popupSlot = null }
        })
        animator = a; a.start()
    }
    private fun drawPopup(c: Canvas) {
        val s = popupSlot ?: return
        val p = popupAnim
        if (p <= 0.01f) return
        val kr = s.rect
        val bw = max(kr.width() * 1.45f, 50f * d)
        val bh = 56f * d
        val maxLeft = max(2f * d, width - 2f * d - bw)
        val left = (kr.centerX() - bw / 2f).coerceIn(2f * d, maxLeft)
        bubbleRect.set(left, kr.top - bh - 2f * d, left + bw, kr.top - 2f * d)
        neckRect.set(kr.left, bubbleRect.bottom - 18f * d, kr.right, kr.bottom)
        popupPath.reset(); popupPath.addRoundRect(bubbleRect, 14f * d, 14f * d, Path.Direction.CW)
        popupPath.addRoundRect(neckRect, 9f * d, 9f * d, Path.Direction.CW)
        val sc = 0.7f + 0.3f * p
        val a = (min(p, 1f) * 255).toInt()
        c.save(); c.scale(sc, sc, kr.centerX(), kr.bottom)
        paint.shader = null; paint.style = Paint.Style.FILL; paint.color = theme.popup
        paint.alpha = Color.alpha(theme.popup) * a / 255; c.drawPath(popupPath, paint)
        if (Color.alpha(theme.popupStroke) > 0) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f * d
            paint.color = theme.popupStroke; paint.alpha = Color.alpha(theme.popupStroke) * a / 255
            c.drawPath(popupPath, paint); paint.style = Paint.Style.FILL
        }
        textPaint.color = theme.popupText; textPaint.alpha = a; textPaint.textSize = 32f * d
        drawCentered(c, popupLabel, bubbleRect); c.restore()
    }
    private fun hit(x: Float, y: Float): Slot? {
        val h = gap / 2
        return slots.firstOrNull { x >= it.rect.left - h && x <= it.rect.right + h && y >= it.rect.top - h && y <= it.rect.bottom + h }
    }
    private fun haptic() { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    private fun onNewPress(s: Slot) {
        when (s.key.type) {
            KeyType.CHAR -> { showPopup(s); if (s.key.hint != null) uiHandler.postDelayed(longPress, 380) }
            else -> hidePopup()
        }
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longFired = false; pressed = hit(e.x, e.y); invalidate()
                val s = pressed
                if (s != null) {
                    haptic()
                    if (s.key.type == KeyType.DEL) { listener.onDelete(); uiHandler.postDelayed(repeat, 400) }
                    else onNewPress(s)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val h = hit(e.x, e.y)
                if (h !== pressed) {
                    uiHandler.removeCallbacks(repeat); uiHandler.removeCallbacks(longPress); longFired = false
                    pressed = h; invalidate()
                    if (h != null) { haptic(); onNewPress(h) } else hidePopup()
                }
            }
            MotionEvent.ACTION_UP -> {
                uiHandler.removeCallbacks(repeat); uiHandler.removeCallbacks(longPress)
                val p = pressed; pressed = null; invalidate(); hidePopup()
                if (p != null && p.key.type != KeyType.DEL && !longFired) fire(p.key)
                longFired = false
            }
            MotionEvent.ACTION_CANCEL -> {
                uiHandler.removeCallbacks(repeat); uiHandler.removeCallbacks(longPress)
                pressed = null; longFired = false; invalidate(); hidePopup()
            }
        }
        return true
    }
    private fun fire(k: Key) {
        when (k.type) {
            KeyType.CHAR -> {
                val s = if (layer == Layer.LETTERS && shift != Shift.OFF) k.label.uppercase() else k.label
                if (layer == Layer.LETTERS && shift == Shift.ON) { shift = Shift.OFF; invalidate() }
                listener.onText(s)
            }
            KeyType.SPACE -> listener.onText(" ")
            KeyType.ENTER -> listener.onEnter()
            KeyType.GLOBE -> listener.onGlobe()
            KeyType.SHIFT -> {
                val now = SystemClock.uptimeMillis()
                shift = when { shift == Shift.CAPS -> Shift.OFF; shift == Shift.ON && now - lastShiftTap < 350 -> Shift.CAPS; shift == Shift.ON -> Shift.OFF; else -> Shift.ON }
                lastShiftTap = now; invalidate()
            }
            KeyType.MODE_SYM -> setLayer(Layer.SYMBOLS)
            KeyType.MODE_ABC -> setLayer(Layer.LETTERS)
            else -> {}
        }
    }
    override fun onDetachedFromWindow() {
        uiHandler.removeCallbacks(repeat); uiHandler.removeCallbacks(longPress); animator?.cancel()
        super.onDetachedFromWindow()
    }
}
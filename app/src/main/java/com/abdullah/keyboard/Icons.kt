package com.abdullah.keyboard

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

enum class IconId {
    GRID, EMOJI, CLIPBOARD, TRANSLATE, SETTINGS, PALETTE, MIC, CLOSE,
    ONEHAND, TEXTEDIT, RESIZE, SHARE, GLOBE
}

/** Vector-ish icons drawn on a 24x24 grid. */
object Icons {
    private val rect = RectF()
    private val path = Path()
    private val dash = DashPathEffect(floatArrayOf(2.6f, 2.6f), 0f)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    fun draw(c: Canvas, id: IconId, cx: Float, cy: Float, size: Float, color: Int, p: Paint) {
        val u = size / 24f
        c.save()
        c.translate(cx - size / 2f, cy - size / 2f)
        c.scale(u, u)
        p.color = color
        p.shader = null
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 2f
        when (id) {
            IconId.GRID -> {
                p.style = Paint.Style.FILL
                for (i in 0..1) for (j in 0..1) {
                    val x = 3f + i * 10f
                    val y = 3f + j * 10f
                    rect.set(x, y, x + 8f, y + 8f)
                    c.drawRoundRect(rect, 2.2f, 2.2f, p)
                }
            }
            IconId.EMOJI -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(12f, 12f, 9f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(9f, 10f, 1.3f, p)
                c.drawCircle(15f, 10f, 1.3f, p)
                p.style = Paint.Style.STROKE
                rect.set(7.5f, 8f, 16.5f, 17f)
                c.drawArc(rect, 25f, 130f, false, p)
            }
            IconId.CLIPBOARD -> {
                p.style = Paint.Style.STROKE
                rect.set(5f, 4f, 19f, 21f)
                c.drawRoundRect(rect, 2.5f, 2.5f, p)
                p.style = Paint.Style.FILL
                rect.set(9f, 2f, 15f, 6f)
                c.drawRoundRect(rect, 1.5f, 1.5f, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.8f
                c.drawLine(8.5f, 11f, 15.5f, 11f, p)
                c.drawLine(8.5f, 14.5f, 15.5f, 14.5f, p)
                c.drawLine(8.5f, 18f, 12.5f, 18f, p)
            }
            IconId.TRANSLATE -> {
                tp.color = color
                tp.textSize = 15f
                tp.isFakeBoldText = true
                c.drawText("A\u6587", 12f, 17f, tp)
            }
            IconId.SETTINGS -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(12f, 12f, 6.2f, p)
                c.drawCircle(12f, 12f, 2.4f, p)
                p.style = Paint.Style.FILL
                for (i in 0 until 8) {
                    c.save()
                    c.rotate(i * 45f, 12f, 12f)
                    rect.set(10.6f, 2f, 13.4f, 5.2f)
                    c.drawRoundRect(rect, 0.8f, 0.8f, p)
                    c.restore()
                }
            }
            IconId.PALETTE -> {
                p.style = Paint.Style.STROKE
                rect.set(3f, 3.5f, 21f, 20.5f)
                c.drawOval(rect, p)
                p.style = Paint.Style.FILL
                c.drawCircle(8f, 10.5f, 1.4f, p)
                c.drawCircle(12f, 7.5f, 1.4f, p)
                c.drawCircle(16.5f, 10.5f, 1.4f, p)
                c.drawCircle(8.5f, 15.5f, 1.4f, p)
            }
            IconId.MIC -> {
                p.style = Paint.Style.FILL
                rect.set(9f, 2f, 15f, 14f)
                c.drawRoundRect(rect, 3f, 3f, p)
                p.style = Paint.Style.STROKE
                rect.set(5.5f, 5.5f, 18.5f, 18.5f)
                c.drawArc(rect, 0f, 180f, false, p)
                c.drawLine(12f, 18.5f, 12f, 22f, p)
                c.drawLine(8.5f, 22f, 15.5f, 22f, p)
            }
            IconId.CLOSE -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2.4f
                c.drawLine(6f, 6f, 18f, 18f, p)
                c.drawLine(18f, 6f, 6f, 18f, p)
            }
            IconId.ONEHAND -> {
                p.style = Paint.Style.STROKE
                rect.set(7f, 2.5f, 17f, 21.5f)
                c.drawRoundRect(rect, 2.5f, 2.5f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(12f, 17.5f, 1.4f, p)
                p.style = Paint.Style.STROKE
                c.drawLine(10.5f, 6f, 13.5f, 6f, p)
            }
            IconId.TEXTEDIT -> {
                p.style = Paint.Style.STROKE
                c.drawLine(12f, 5f, 12f, 19f, p)
                c.drawLine(9.5f, 5f, 14.5f, 5f, p)
                c.drawLine(9.5f, 19f, 14.5f, 19f, p)
                p.style = Paint.Style.FILL
                path.reset()
                path.moveTo(6.5f, 8.5f); path.lineTo(3f, 12f); path.lineTo(6.5f, 15.5f); path.close()
                c.drawPath(path, p)
                path.reset()
                path.moveTo(17.5f, 8.5f); path.lineTo(21f, 12f); path.lineTo(17.5f, 15.5f); path.close()
                c.drawPath(path, p)
            }
            IconId.RESIZE -> {
                p.style = Paint.Style.STROKE
                p.pathEffect = dash
                c.drawRect(4f, 4f, 20f, 20f, p)
                p.pathEffect = null
            }
            IconId.SHARE -> {
                p.style = Paint.Style.STROKE
                c.drawLine(6f, 12f, 18f, 6f, p)
                c.drawLine(6f, 12f, 18f, 18f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(6f, 12f, 2.8f, p)
                c.drawCircle(18f, 6f, 2.8f, p)
                c.drawCircle(18f, 18f, 2.8f, p)
            }
            IconId.GLOBE -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.8f
                c.drawCircle(12f, 12f, 9f, p)
                rect.set(8f, 3f, 16f, 21f)
                c.drawOval(rect, p)
                c.drawLine(3f, 12f, 21f, 12f, p)
            }
        }
        c.restore()
        p.pathEffect = null
    }
}

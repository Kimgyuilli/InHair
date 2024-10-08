package com.example.a2024capstonesample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

// GuideFrameView.kt
class GuideFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val framePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#33000000") // 반투명 검정
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // 중앙에 500x500 프레임 그리기
        val frameSize = 500f
        val left = (width - frameSize) / 2
        val top = (height - frameSize) / 2
        val right = left + frameSize
        val bottom = top + frameSize

        // 바깥 영역을 어둡게 그리기
        canvas.drawRect(0f, 0f, width, top, overlayPaint) // 상단
        canvas.drawRect(0f, top, left, bottom, overlayPaint) // 좌측
        canvas.drawRect(right, top, width, bottom, overlayPaint) // 우측
        canvas.drawRect(0f, bottom, width, height, overlayPaint) // 하단

        // 프레임 테두리 그리기
        canvas.drawRect(left, top, right, bottom, framePaint)
    }
}
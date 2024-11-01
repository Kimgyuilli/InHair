package com.example.InHair

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

    private val textPaint = Paint().apply {
        color = Color.WHITE // 텍스트 색상
        textSize = 50f // 텍스트 크기
        textAlign = Paint.Align.CENTER // 중앙 정렬
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // 디스플레이 크기에 따라 타원 크기 설정
        val ovalWidth = width * 0.9f  // 화면 너비의 90%를 타원의 너비로 설정
        val ovalHeight = height * 0.9f // 화면 높이의 90%를 타원의 높이로 설정
        val left = (width - ovalWidth) / 2
        val top = (height - ovalHeight) / 2
        val right = left + ovalWidth
        val bottom = top + ovalHeight

        // 바깥 영역을 어둡게 그리기
        canvas.drawRect(0f, 0f, width, top, overlayPaint) // 상단
        canvas.drawRect(0f, top, left, bottom, overlayPaint) // 좌측
        canvas.drawRect(right, top, width, bottom, overlayPaint) // 우측
        canvas.drawRect(0f, bottom, width, height, overlayPaint) // 하단

        // 타원형 프레임 테두리 그리기
        canvas.drawOval(left, top, right, bottom, framePaint)
        canvas.drawText("머리를 타원에 맞춰주세요", width / 2, top + 300, textPaint)
    }
}
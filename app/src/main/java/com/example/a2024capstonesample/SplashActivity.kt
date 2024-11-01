package com.example.a2024capstonesample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 스플래시 화면 레이아웃을 설정
        setContentView(R.layout.activity_splash)

        // 1초 후에 MainActivity로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // 현재 액티비티 종료
        }, 1000) // 1000ms = 1초
    }
}

package com.example.a2024capstonesample.Room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,         // 사진을 찍은 날짜 (형식: YY-MM-DD-HH-mm-ss)
    val imagePath: String?, // 사진 경로
    val score: Float,         // 점수
    val formattedScore: String? = null, // 포맷팅된 점수 추가
)
data class AverageScore(
    val date: String,
    val averageScore: Float
)
package com.example.InHair.data

data class PhotoData(
    val date: String, // 사진 촬영 날짜
    val photoPath: String, // 사진 경로
    var goodMeasurement: Double, // 양호 수치
    var cautionMeasurement: Double // 주의 수치
)
package com.example.a2024capstonesample.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MyDao {
    @Insert
    suspend fun insert(myEntity: MyEntity)

    @Query("SELECT * FROM MyEntity")
    suspend fun getAllEntities(): List<MyEntity>

    @Query("DELETE FROM MyEntity")
    suspend fun deleteAll()

    // 날짜별 평균 점수 가져오기
    @Query("""
        SELECT date, AVG(score) as averageScore
        FROM MyEntity
        GROUP BY date
        ORDER BY date
        LIMIT 10""")
    suspend fun getAverageScoresByDate(): List<AverageScore>
}
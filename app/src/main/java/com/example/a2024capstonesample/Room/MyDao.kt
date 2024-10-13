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
}
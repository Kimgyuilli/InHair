package com.example.InHair.Room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MyEntity::class], version = 4)
abstract class MyAppDatabase : RoomDatabase() {

    abstract fun myDao(): MyDao

    companion object {
        @Volatile
        private var INSTANCE: MyAppDatabase? = null

        fun getDatabase(context: Context): MyAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyAppDatabase::class.java,
                    "my-database"
                )
                    .fallbackToDestructiveMigration() // 마이그레이션 전략 추가
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
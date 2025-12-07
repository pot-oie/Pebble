package com.pot.pebble.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pot.pebble.data.dao.AppConfigDao
import com.pot.pebble.data.entity.AppConfig

@Database(entities = [AppConfig::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appConfigDao(): AppConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pebble_database"
                )
                    // 开发阶段允许主线程查询（防止还没切协程报错），但在生产环境最好去掉
                    // .allowMainThreadQueries()
                    .fallbackToDestructiveMigration() // 数据库版本变动时，直接清空重建 (开发阶段省事)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
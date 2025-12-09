package com.pot.pebble.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pot.pebble.data.dao.AnalyticsDao
import com.pot.pebble.data.dao.AppConfigDao
import com.pot.pebble.data.entity.AppConfig
import com.pot.pebble.data.entity.InterferenceLog

@Database(
    entities = [AppConfig::class, InterferenceLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appConfigDao(): AppConfigDao
    abstract fun analyticsDao(): AnalyticsDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
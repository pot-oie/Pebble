package com.pot.pebble.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pot.pebble.data.entity.AppConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {

    // 给 Service 用的：只查黑名单包名，不需要 Flow，直接返回快照
    @Query("SELECT packageName FROM app_configs WHERE isBlacklisted = 1")
    suspend fun getBlacklistedPackageList(): List<String>

    // 给 UI 用的：查询所有应用，按“是否黑名单”优先排序，名字其次
    @Query("SELECT * FROM app_configs ORDER BY isBlacklisted DESC, appName ASC")
    fun getAllConfigsFlow(): Flow<List<AppConfig>>

    // 扫描器用：如果存在则忽略 (保留用户的设置)，如果不存在则插入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfigs(configs: List<AppConfig>)

    // 用户操作：切换开关
    @Query("UPDATE app_configs SET isBlacklisted = :isBlacklisted WHERE packageName = :packageName")
    suspend fun updateStatus(packageName: String, isBlacklisted: Boolean)
}
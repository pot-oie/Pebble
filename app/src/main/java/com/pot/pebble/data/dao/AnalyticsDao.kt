package com.pot.pebble.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pot.pebble.data.entity.InterferenceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {
    // 插入日志
    @Insert
    suspend fun insertLog(log: InterferenceLog)

    // 统计今日落石次数 (用于主页报表)
    @Query("SELECT COUNT(*) FROM interference_logs WHERE type = 0 AND timestamp >= :startTime")
    fun getInterferenceCountSince(startTime: Long): Flow<Int>

    // 统计今日专注时长 (用于主页报表)
    @Query("SELECT SUM(duration) FROM interference_logs WHERE type = 1 AND timestamp >= :startTime")
    fun getFocusDurationSince(startTime: Long): Flow<Long?>

    // 获取指定时间之后的所有日志 (用于绘制趋势图)
    @Query("SELECT * FROM interference_logs WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getLogsSinceFlow(startTime: Long): Flow<List<InterferenceLog>>
}
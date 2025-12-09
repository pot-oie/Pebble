package com.pot.pebble.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interference_logs")
data class InterferenceLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 发生时间
    val timestamp: Long,

    // 类型：0=被动干扰(落石), 1=主动专注
    val type: Int,

    // 关联的应用包名 (专注模式下可为空)
    val packageName: String?,

    // 专注时长 (仅 type=1 有效)
    val duration: Long = 0L
)
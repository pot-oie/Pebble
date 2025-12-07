package com.pot.pebble.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_configs")
data class AppConfig(
    @PrimaryKey val packageName: String,
    val appName: String,

    // 核心功能：是否在黑名单
    val isBlacklisted: Boolean = false,

    // --- 预留给第二阶段 (Skin System) ---
    val skinType: String = "DEFAULT_ROCK", // 比如: TEXT, POOP, CRACK

    // --- 预留给物理参数调整 ---
    val gravityScale: Float = 1.0f,
    val restitution: Float = 0.2f
)
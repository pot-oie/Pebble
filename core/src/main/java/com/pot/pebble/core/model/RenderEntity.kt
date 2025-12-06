package com.pot.pebble.core.model

/**
 * 渲染实体
 * 物理引擎告诉 UI：在什么位置画什么东西
 */
data class RenderEntity(
    val id: Long,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val type: EntityType
)

enum class EntityType {
    CIRCLE, // 圆形（石头）
    TEXT    // 文字（弹幕）
}
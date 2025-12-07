package com.pot.pebble.core.model

/**
 * 渲染实体
 * 物理引擎告诉 UI：在什么位置画什么东西
 */
data class RenderEntity(
    var id: Long = 0,
    var x: Float = 0f,
    var y: Float = 0f,
    var rotation: Float = 0f,
    var type: EntityType = EntityType.CIRCLE,
    var radius: Float = 0f
)

enum class EntityType {
    CIRCLE, // 圆形（石头）
    TEXT    // 文字（弹幕）
}
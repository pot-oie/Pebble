package com.pot.pebble.core.model

/**
 * 渲染实体
 */
data class RenderEntity(
    var id: Long = 0,
    var x: Float = 0f,
    var y: Float = 0f,
    var rotation: Float = 0f,

    // 类型：圆形石头、方形方块、文字弹幕
    var type: EntityType = EntityType.CIRCLE,

    // --- 形状参数 ---
    var radius: Float = 0f,       // 圆形半径
    var width: Float = 0f,        // 矩形宽
    var height: Float = 0f,       // 矩形高

    // --- 内容参数 ---
    var text: String? = null,      // 弹幕内容
    var textureId: String? = null, // 自定义贴图ID
    var color: Int? = null,        // 颜色

    var tetrisShape: TetrisShape? = null,

    var customImageUri: String? = null
)

enum class EntityType {
    CIRCLE,    // 普通石头
    BOX,       // 俄罗斯方块 / 碎屏块
    TEXT,      // 文字弹幕
    CRACK,     // 碎屏
    CUSTOM     // 自定义
}
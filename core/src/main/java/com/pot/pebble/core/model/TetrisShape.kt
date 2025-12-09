package com.pot.pebble.core.model

// 莫兰迪色系
enum class TetrisShape(
    val color: Int, // 0xAARRGGBB
    val offsets: List<Pair<Float, Float>>
) {
    // I: 雾霾蓝
    I(0xFF7DA7D9.toInt(), listOf(-1.5f to 0f, -0.5f to 0f, 0.5f to 0f, 1.5f to 0f)),

    // J: 灰蓝色
    J(0xFF8FA3B8.toInt(), listOf(-1f to -0.5f, -1f to 0.5f, 0f to 0.5f, 1f to 0.5f)),

    // L: 脏橘色
    L(0xFFE0B08E.toInt(), listOf(-1f to 0.5f, 0f to 0.5f, 1f to 0.5f, 1f to -0.5f)),

    // O: 奶酪黄
    O(0xFFE6D690.toInt(), listOf(-0.5f to -0.5f, 0.5f to -0.5f, -0.5f to 0.5f, 0.5f to 0.5f)),

    // S: 豆沙绿
    S(0xFFA2BFA2.toInt(), listOf(-1f to 0.5f, 0f to 0.5f, 0f to -0.5f, 1f to -0.5f)),

    // T: 香芋紫
    T(0xFFBCA2C7.toInt(), listOf(-1f to 0.5f, 0f to 0.5f, 1f to 0.5f, 0f to -0.5f)),

    // Z: 干燥玫瑰红
    Z(0xFFD68C8C.toInt(), listOf(-1f to -0.5f, 0f to -0.5f, 0f to 0.5f, 1f to 0.5f));

    companion object {
        fun random() = entries.random()
    }
}
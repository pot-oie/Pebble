package com.pot.pebble.core.strategy

import com.pot.pebble.core.contract.InterferenceStrategy
import com.pot.pebble.core.model.RenderEntity
import com.pot.pebble.core.physics.PhysicsManager
import kotlin.random.Random

class JBox2DStrategy : InterferenceStrategy {

    private val physics = PhysicsManager()
    private var isInitialized = false
    private var screenW = 0f

    fun setScreenSize(w: Float, h: Float) {
        this.screenW = w
        physics.setupBounds(w, h)
        isInitialized = true
    }

    // 供外部调用，动态生成一个石头
    fun addRandomRock() {
        if (!isInitialized) return

        physics.createRock(
            xPx = Random.nextFloat() * screenW,
            yPx = -50f, // 从屏幕外刚刚好的位置掉下来
            radiusPx = 30f + Random.nextFloat() * 30f // 大小随机
        )
    }

    override fun onStart() {
        // 如果还没初始化屏幕尺寸，先不生成
        if (!isInitialized) return
    }

    override fun update(dt: Long, gx: Float, gy: Float): List<RenderEntity> {
        if (!isInitialized) return emptyList()

        // 1. 物理计算一步
        physics.step(dt, gx, gy)

        // 2. 返回渲染数据
        return physics.getRenderData()
    }

    override fun onStop() {
        // JBox2D 没有显式的 destroy world，Java GC 会回收
        // 但如果以后有复杂的 Listener，这里需要清理
    }
}
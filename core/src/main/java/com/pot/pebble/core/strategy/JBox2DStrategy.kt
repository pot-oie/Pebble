package com.pot.pebble.core.strategy

import com.pot.pebble.core.contract.InterferenceStrategy
import com.pot.pebble.core.model.RenderEntity
import com.pot.pebble.core.physics.PhysicsManager
import kotlin.random.Random

class JBox2DStrategy : InterferenceStrategy {

    private val physics = PhysicsManager()
    private var isInitialized = false
    private var screenW = 0f

    fun setScreenSize(w: Float, h: Float, paddingTop: Float, paddingBottom: Float) {
        this.screenW = w
        physics.setupBounds(w, h, paddingTop, paddingBottom)
        isInitialized = true
    }

    // 供外部调用，动态生成一个石头
    fun addRandomRock() {
        if (!isInitialized) return
        physics.createRock(
            xPx = Random.nextFloat() * screenW,
            yPx = -100f,
            radiusPx = 80f + Random.nextFloat() * 40f
        )
    }

    fun clearAllBodies() {
        if (!isInitialized) return
        physics.clearDynamicBodies()
    }

    fun isFull(): Boolean {
        if (!isInitialized) return false
        return physics.isTopFull()
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
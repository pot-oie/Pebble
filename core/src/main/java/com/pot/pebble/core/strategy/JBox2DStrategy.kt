package com.pot.pebble.core.strategy

import com.pot.pebble.core.contract.InterferenceStrategy
import com.pot.pebble.core.model.EntityType
import com.pot.pebble.core.model.RenderEntity
import com.pot.pebble.core.model.TetrisShape
import com.pot.pebble.core.physics.PhysicsManager
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

class JBox2DStrategy : InterferenceStrategy {

    private val physics = PhysicsManager()
    private var isInitialized = false
    private var screenW = 0f
    private var screenH = 0f // 新增：保存屏幕高度

    private val danmakuBgColor = 0xAA000000.toInt()

    private var customDanmakuList: List<String> = listOf("Focus!")

    // 缓存当前的图片 URI 和图片宽高比 (宽/高)
    private var currentCustomUri: String? = null
    private var currentAspectRatio: Float = 1.0f

    fun setScreenSize(w: Float, h: Float, paddingTop: Float, paddingBottom: Float) {
        this.screenW = w
        this.screenH = h
        physics.setupBounds(w, h, paddingTop, paddingBottom)
        isInitialized = true
    }

    // 供外部更新弹幕列表
    fun updateDanmakuList(list: List<String>) {
        if (list.isNotEmpty()) {
            this.customDanmakuList = list
        }
    }

    // 供 GameEngine 更新 URI 和比例
    fun updateCustomImage(uri: String?, ratio: Float) {
        this.currentCustomUri = uri
        this.currentAspectRatio = ratio
    }

    /**
     * 通用生成方法 (物理物体)
     */
    fun spawnObstacle(type: EntityType) {
        if (!isInitialized) return

        val maxAttempts = 5

        for (i in 0 until maxAttempts) {
            // 1. 随机位置
            // startX: 屏幕宽度内随机
            val startX = 50f + Random.nextFloat() * (screenW - 100f)

            // startY: 给一个垂直方向的随机偏移 (-300 到 -150)
            // 这样即使横向重叠，纵向也能错开，不会粘死
            val startY = -150f - Random.nextFloat() * 150f

            // 2. 根据类型预估大小，并检查安全
            var estimatedSize = 0f
            var isSafe = false

            when (type) {
                EntityType.CIRCLE -> {
                    estimatedSize = 80f // 预估半径
                    // 检查半径 * 2.2 倍的范围，确保完全不碰
                    if (physics.isRegionSafe(startX, startY, estimatedSize * 2.2f)) {
                        physics.createCircle(
                            xPx = startX,
                            yPx = startY,
                            radiusPx = 60f + Random.nextFloat() * 50f
                        )
                        isSafe = true
                    }
                }
                EntityType.BOX -> {
                    // 俄罗斯方块比较大，给大一点的安全距离
                    estimatedSize = 100f
                    if (physics.isRegionSafe(startX, startY, estimatedSize * 2.0f)) {
                        val shape = TetrisShape.random()
                        val blockSize = 40f + Random.nextFloat() * 20f
                        physics.createTetrisBody(
                            xPx = startX,
                            yPx = startY,
                            shape = shape,
                            blockSizePx = blockSize
                        )
                        isSafe = true
                    }
                }
                EntityType.TEXT -> {
                    estimatedSize = 120f
                    if (physics.isRegionSafe(startX, startY, estimatedSize * 1.5f)) {
                        val text = customDanmakuList.random()
                        val estimatedWidth = text.length * 40f + 60f
                        physics.createBox(
                            xPx = startX,
                            yPx = startY,
                            wPx = estimatedWidth,
                            hPx = 90f,
                            type = EntityType.TEXT,
                            text = text,
                            color = danmakuBgColor
                        )
                        isSafe = true
                    }
                }
                EntityType.CUSTOM -> {
                    if (currentCustomUri != null) {
                        // 1. 增大基础尺寸：从 100 提升到 180~240
                        val baseSize = 180f + Random.nextFloat() * 60f

                        // 2. 根据比例计算宽和高 (面积恒定算法)
                        val sqrtRatio = sqrt(currentAspectRatio)
                        var w = baseSize * sqrtRatio
                        var h = baseSize / sqrtRatio

                        // 3. 尺寸安全限制：防止过细或过大
                        // 限制单边最小 80px (太小看不清)
                        // 限制单边最大 400px (太大挡屏幕)
                        if (w < 80f) { w = 80f; h = 80f / currentAspectRatio }
                        if (h < 80f) { h = 80f; w = 80f * currentAspectRatio }
                        if (w > 400f) { w = 400f; h = 400f / currentAspectRatio }
                        if (h > 400f) { h = 400f; w = 400f * currentAspectRatio }

                        // 取最大边作为安全检查半径
                        val safeRadius = max(w, h)

                        if (physics.isRegionSafe(startX, startY, safeRadius * 1.2f)) {
                            physics.createCustomBody(
                                xPx = startX,
                                yPx = startY,
                                wPx = w,
                                hPx = h,
                                uri = currentCustomUri!!
                            )
                            isSafe = true
                        }
                    } else {
                        spawnObstacle(EntityType.CIRCLE)
                        return
                    }
                }
                else -> { isSafe = true } // Crack 不需要检查
            }

            // 3. 如果成功生成了，就结束循环；否则再试一次新的坐标
            if (isSafe) break
        }
    }

    /**
     * 专门生成静态裂纹实体 (不进入物理世界)
     */
    fun createStaticCrack(): RenderEntity? {
        if (!isInitialized) return null

        return RenderEntity(
            id = System.nanoTime(),
            x = Random.nextFloat() * screenW,
            y = Random.nextFloat() * screenH,
            rotation = Random.nextFloat() * 360f,
            type = EntityType.CRACK,
            radius = 150f + Random.nextFloat() * 200f
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

    override fun onStart() {}

    override fun update(dt: Long, gx: Float, gy: Float): List<RenderEntity> {
        if (!isInitialized) return emptyList()
        physics.step(dt, gx, gy)
        return physics.getRenderData()
    }

    override fun onStop() {}
}
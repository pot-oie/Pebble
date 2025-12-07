package com.pot.pebble.core.physics

import com.pot.pebble.core.model.EntityType
import com.pot.pebble.core.model.RenderEntity
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import java.util.ArrayList
import java.util.LinkedList

class PhysicsManager {

    private val PPM = 30f
    private val world = World(Vec2(0f, 10f))
    private var screenWidthMeters = 0f
    private var screenHeightMeters = 0f

    // 计算用的缓存池（复用对象，减少计算时的 GC）
    private val cachedEntities = ArrayList<RenderEntity>()

    // 🔒 锁对象：用来保证计算和读取不会同时发生
    private val lock = Any()

    fun setupBounds(widthPx: Float, heightPx: Float, paddingTopPx: Float, paddingBottomPx: Float) {
        synchronized(lock) {
            screenWidthMeters = widthPx / PPM
            screenHeightMeters = heightPx / PPM

            // 【修复】底部位置修正
            // 确保地板上表面紧贴 paddingBottomPx (即导航栏上方)
            val bottomMeters = paddingBottomPx / PPM
            val floorHeight = 2f
            // 地板中心位置 = 屏幕底边 - 导航栏高度 + 地板一半厚度
            val floorY = screenHeightMeters - bottomMeters + (floorHeight / 2)

            // 创建静态边界
            // 地板
            createStaticBox(screenWidthMeters / 2, floorY, screenWidthMeters, floorHeight)
            // 左右墙壁 (加高防止溢出)
            createStaticBox(-1f, screenHeightMeters / 2, 2f, screenHeightMeters * 3)
            createStaticBox(screenWidthMeters + 1f, screenHeightMeters / 2, 2f, screenHeightMeters * 3)
        }
    }

    // 【修复】检测顶部是否堵住
    fun isTopFull(): Boolean {
        synchronized(lock) {
            var body = world.bodyList
            while (body != null) {
                if (body.type == BodyType.DYNAMIC) {
                    val yPx = body.position.y * PPM
                    // 【关键参数】0 到 150px 是屏幕最上方的检测区域
                    // 如果有石头在这个区域内且基本静止，认为已满
                    if (yPx > 0 && yPx < 150f) {
                        if (body.linearVelocity.length() < 1.0f) {
                            return true
                        }
                    }
                }
                body = body.next
            }
            return false
        }
    }

    // 辅助方法：获取当前石头数量 (调试用)
    fun getDynamicBodyCount(): Int {
        synchronized(lock) {
            var count = 0
            var body = world.bodyList
            while (body != null) {
                if (body.type == BodyType.DYNAMIC) {
                    count++
                }
                body = body.next
            }
            return count
        }
    }

    fun createRock(xPx: Float, yPx: Float, radiusPx: Float): Long {
        synchronized(lock) {
            val bodyDef = BodyDef().apply {
                type = BodyType.DYNAMIC
                position.set(xPx / PPM, yPx / PPM)
            }
            val body = world.createBody(bodyDef)
            val shape = CircleShape().apply { radius = radiusPx / PPM }
            val fixtureDef = FixtureDef().apply {
                this.shape = shape
                density = 1.0f
                friction = 0.3f
                restitution = 0.2f
            }
            body.createFixture(fixtureDef)
            return body.hashCode().toLong()
        }
    }

    fun step(dt: Long, gravityX: Float, gravityY: Float) {
        synchronized(lock) {
            world.gravity = Vec2(-gravityX, gravityY)

            // 固定时间步长，保证物理模拟稳定
            val fixedTimeStep = 1.0f / 60.0f
            world.step(fixedTimeStep, 8, 3)

            // 【核心修复】清理掉出屏幕的石头
            // 防止穿模导致的“幽灵石头”占用内存和影响计数
            cleanupOutOfBoundsBodies()
        }
    }

    private fun cleanupOutOfBoundsBodies() {
        // 定义删除阈值：屏幕底部再往下 200px
        // 只要石头掉到这里，就肯定看不见且回不来了
        val deleteThresholdY = screenHeightMeters + (200f / PPM)

        var body = world.bodyList
        // 用一个临时列表存要删除的 body，避免在遍历时修改集合导致异常
        val bodiesToRemove = LinkedList<org.jbox2d.dynamics.Body>()

        while (body != null) {
            if (body.type == BodyType.DYNAMIC) {
                if (body.position.y > deleteThresholdY) {
                    bodiesToRemove.add(body)
                }
            }
            body = body.next
        }

        // 统一销毁
        for (b in bodiesToRemove) {
            world.destroyBody(b)
        }
    }

    // 获取渲染数据 (Deep Copy 快照)
    fun getRenderData(): List<RenderEntity> {
        synchronized(lock) {
            var body = world.bodyList
            var index = 0

            while (body != null) {
                if (body.type == BodyType.DYNAMIC) {
                    // 1. 确保缓存池够大
                    if (index >= cachedEntities.size) {
                        cachedEntities.add(RenderEntity())
                    }

                    // 2. 更新缓存池里的数据
                    val entity = cachedEntities[index]
                    entity.id = body.hashCode().toLong()
                    entity.x = body.position.x * PPM
                    entity.y = body.position.y * PPM
                    entity.rotation = Math.toDegrees(body.angle.toDouble()).toFloat()
                    entity.type = EntityType.CIRCLE

                    // 获取准确的半径
                    val fixture = body.fixtureList
                    if (fixture != null && fixture.shape is CircleShape) {
                        val shape = fixture.shape as CircleShape
                        entity.radius = shape.radius * PPM
                    } else {
                        entity.radius = 30f // 默认保护
                    }

                    index++
                }
                body = body.next
            }

            // 3. 生成快照 (Deep Copy)
            // 只有这样，UI 线程拿到的数据才永远不会被后台线程修改
            val snapshot = ArrayList<RenderEntity>(index)
            for (i in 0 until index) {
                snapshot.add(cachedEntities[i].copy())
            }

            return snapshot
        }
    }

    private fun createStaticBox(x: Float, y: Float, width: Float, height: Float) {
        val bodyDef = BodyDef().apply {
            position.set(x, y)
            type = BodyType.STATIC
        }
        val body = world.createBody(bodyDef)
        val shape = PolygonShape()
        shape.setAsBox(width / 2, height / 2)
        body.createFixture(shape, 0f)
    }

    // 清除所有动态物体（石头），保留墙壁和地板
    fun clearDynamicBodies() {
        synchronized(lock) {
            var body = world.bodyList
            // 使用临时列表存储待删除的 body，防止遍历时修改集合报错
            val bodiesToRemove = ArrayList<org.jbox2d.dynamics.Body>()

            while (body != null) {
                // 只删除动态物体 (石头)，别把墙拆了
                if (body.type == BodyType.DYNAMIC) {
                    bodiesToRemove.add(body)
                }
                body = body.next
            }

            for (b in bodiesToRemove) {
                world.destroyBody(b)
            }

            // 清空缓存池
            cachedEntities.clear()
        }
    }
}
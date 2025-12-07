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

            // 【修改点1】地板位置修正
            // 原来可能抬得不够高。现在我们把地板中心放在：
            // (屏幕高度 - 底部边距 - 地板半厚度 - 额外的安全缓冲50px)
            // 这样石头底部会严格处于 paddingBottomPx + 50px 的位置之上
            val safeBufferMeters = 50f / PPM
            val bottomMeters = paddingBottomPx / PPM + safeBufferMeters

            val floorHeight = 2f // 地板厚度2米
            val floorY = screenHeightMeters - bottomMeters + (floorHeight / 2)

            // 清理旧物体（简单起见这里假设只调用一次，或者你需要加清理逻辑）
            // createStaticBox...

            // 地板
            createStaticBox(screenWidthMeters / 2, floorY, screenWidthMeters, floorHeight)
            // 左右墙壁 (加高一点防止溢出)
            createStaticBox(-1f, screenHeightMeters / 2, 2f, screenHeightMeters * 3)
            createStaticBox(screenWidthMeters + 1f, screenHeightMeters / 2, 2f, screenHeightMeters * 3)
        }
    }

    // 【新增】检测顶部是否堵住了
    fun isTopFull(): Boolean {
        synchronized(lock) {
            var body = world.bodyList
            while (body != null) {
                if (body.type == BodyType.DYNAMIC) {
                    val yPx = body.position.y * PPM
                    // 如果石头处于屏幕顶部 600px 范围内
                    if (yPx > 0 && yPx < 600) {
                        // 并且它是基本静止的（不是刚生成正在往下冲的）
                        // velocity 长度小于 1.0
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

            // 🔥 关键修复：固定时间步长
            // 无论 dt 是多少，我们只告诉物理引擎过去了 1/60 秒
            // 这样能保证物理模拟极其稳定，不会乱抖
            val fixedTimeStep = 1.0f / 60.0f
            world.step(fixedTimeStep, 8, 3)
        }
    }

    // 🔥 关键修复：返回深拷贝的快照
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

                    // 👇 新增：从 Fixture 获取半径 (米 -> 像素)
                    // 注意：Box2D 的 m_radius 是私有字段，但 shape.radius 是公开的
                    val fixture = body.fixtureList
                    if (fixture != null && fixture.shape is CircleShape) {
                        val shape = fixture.shape as CircleShape
                        entity.radius = shape.radius * PPM
                    } else {
                        // 默认值，防崩溃
                        entity.radius = 30f
                    }

                    index++
                }
                body = body.next
            }

            // 3. 生成快照 (Deep Copy)
            // 我们必须创建一个新的 List，并复制里面的 RenderEntity
            // 只有这样，UI 线程拿到的数据才永远不会被后台线程修改
            val snapshot = ArrayList<RenderEntity>(index)
            for (i in 0 until index) {
                // 使用 data class 的 copy() 方法复制一份完全一样但独立的对象
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
}
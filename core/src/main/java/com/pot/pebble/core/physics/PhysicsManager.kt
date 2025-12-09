package com.pot.pebble.core.physics

import com.pot.pebble.core.model.EntityType
import com.pot.pebble.core.model.RenderEntity
import com.pot.pebble.core.model.TetrisShape
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import java.util.ArrayList
import java.util.LinkedList

class PhysicsManager {

    private val PPM = 30f
    private val world = World(Vec2(0f, 10f))

    private var screenWidthMeters = 0f
    private var screenHeightMeters = 0f

    private val cachedEntities = ArrayList<RenderEntity>()
    private val lock = Any()

    // BodyMeta
    private data class BodyMeta(
        val type: EntityType,
        val widthPx: Float = 0f,
        val heightPx: Float = 0f,
        val radiusPx: Float = 0f,
        val text: String? = null,
        val color: Int? = null,
        val tetrisShape: TetrisShape? = null,
        val customImageUri: String? = null
    )

    // 创建自定义图片刚体 (本质是 Box)
    fun createCustomBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, uri: String): Long {
        synchronized(lock) {
            val body = createDynamicBody(xPx, yPx)

            val shape = PolygonShape()
            // setAsBox 参数是半宽、半高
            shape.setAsBox((wPx / 2) / PPM, (hPx / 2) / PPM)

            val fixtureDef = FixtureDef().apply {
                this.shape = shape
                density = 1.0f
                friction = 0.4f
                restitution = 0.2f
            }
            body.createFixture(fixtureDef)

            body.userData = BodyMeta(
                type = EntityType.CUSTOM,
                widthPx = wPx,  // 存宽
                heightPx = hPx, // 存高
                customImageUri = uri
            )
            return body.hashCode().toLong()
        }
    }

    // 检测某个区域是否安全（没有其他物体）
    fun isRegionSafe(xPx: Float, yPx: Float, safeRadiusPx: Float): Boolean {
        synchronized(lock) {
            var body = world.bodyList
            val checkPos = Vec2(xPx / PPM, yPx / PPM)
            // 稍微放宽一点检查范围，转成米
            val checkDistMeters = safeRadiusPx / PPM

            while (body != null) {
                if (body.type == BodyType.DYNAMIC) {
                    // 计算现有物体和目标点的距离
                    val dist = body.position.sub(checkPos).length()

                    // 如果距离小于安全半径，说明这里已经有东西了，不安全
                    if (dist < checkDistMeters) {
                        return false
                    }
                }
                body = body.next
            }
            return true
        }
    }

    fun setupBounds(widthPx: Float, heightPx: Float, paddingTopPx: Float, paddingBottomPx: Float) {
        synchronized(lock) {
            screenWidthMeters = widthPx / PPM
            screenHeightMeters = heightPx / PPM
            clearStaticBodies()

            val bottomMeters = paddingBottomPx / PPM
            val floorHeight = 2f
            val floorY = screenHeightMeters - bottomMeters + (floorHeight / 2)

            createStaticBox(screenWidthMeters / 2, floorY, screenWidthMeters, floorHeight)
            createStaticBox(-1f, screenHeightMeters / 2, 2f, screenHeightMeters * 3)
            createStaticBox(screenWidthMeters + 1f, screenHeightMeters / 2, 2f, screenHeightMeters * 3)
        }
    }

    fun createCircle(xPx: Float, yPx: Float, radiusPx: Float): Long {
        synchronized(lock) {
            val body = createDynamicBody(xPx, yPx)
            val shape = CircleShape().apply { radius = radiusPx / PPM }
            val fixtureDef = FixtureDef().apply {
                this.shape = shape
                density = 1.0f
                friction = 0.3f
                restitution = 0.2f
            }
            body.createFixture(fixtureDef)
            // 修改点: 传入空 color
            body.userData = BodyMeta(type = EntityType.CIRCLE, radiusPx = radiusPx)
            return body.hashCode().toLong()
        }
    }

    // createBox 方法签名和实现
    fun createBox(
        xPx: Float,
        yPx: Float,
        wPx: Float,
        hPx: Float,
        type: EntityType,
        text: String? = null,
        color: Int? = null // 新增参数
    ): Long {
        synchronized(lock) {
            val body = createDynamicBody(xPx, yPx)

            val shape = PolygonShape()
            shape.setAsBox((wPx / 2) / PPM, (hPx / 2) / PPM)

            val fixtureDef = FixtureDef().apply {
                this.shape = shape
                density = 1.0f
                friction = 0.3f
                restitution = 0.1f
            }
            body.createFixture(fixtureDef)

            body.userData = BodyMeta(
                type = type,
                widthPx = wPx,
                heightPx = hPx,
                text = text,
                color = color
            )

            return body.hashCode().toLong()
        }
    }

    private fun createDynamicBody(xPx: Float, yPx: Float): Body {
        val bodyDef = BodyDef().apply {
            this.type = BodyType.DYNAMIC
            this.position.set(xPx / PPM, yPx / PPM)
        }
        return world.createBody(bodyDef)
    }

    fun getRenderData(): List<RenderEntity> {
        synchronized(lock) {
            var body = world.bodyList
            var index = 0

            while (body != null) {
                val meta = body.userData as? BodyMeta

                if (body.type == BodyType.DYNAMIC && meta != null) {
                    if (index >= cachedEntities.size) {
                        cachedEntities.add(RenderEntity())
                    }

                    val entity = cachedEntities[index]
                    entity.id = body.hashCode().toLong()
                    entity.x = body.position.x * PPM
                    entity.y = body.position.y * PPM
                    entity.rotation = Math.toDegrees(body.angle.toDouble()).toFloat()

                    entity.type = meta.type
                    entity.radius = meta.radiusPx
                    entity.width = meta.widthPx
                    entity.height = meta.heightPx
                    entity.text = meta.text
                    entity.color = meta.color
                    entity.tetrisShape = meta.tetrisShape
                    entity.customImageUri = meta.customImageUri

                    index++
                }
                body = body.next
            }

            val snapshot = ArrayList<RenderEntity>(index)
            for (i in 0 until index) {
                snapshot.add(cachedEntities[i].copy())
            }
            return snapshot
        }
    }

    fun step(dt: Long, gravityX: Float, gravityY: Float) {
        synchronized(lock) {
            world.gravity = Vec2(-gravityX, gravityY)
            world.step(1.0f / 60.0f, 8, 3)
            cleanupOutOfBoundsBodies()
        }
    }

    private fun cleanupOutOfBoundsBodies() {
        val deleteThresholdY = screenHeightMeters + (200f / PPM)
        var body = world.bodyList
        val bodiesToRemove = LinkedList<Body>()

        while (body != null) {
            if (body.type == BodyType.DYNAMIC && body.position.y > deleteThresholdY) {
                bodiesToRemove.add(body)
            }
            body = body.next
        }
        for (b in bodiesToRemove) {
            world.destroyBody(b)
        }
    }

    fun isTopFull(): Boolean {
        synchronized(lock) {
            var body = world.bodyList
            while (body != null) {
                if (body.type == BodyType.DYNAMIC) {
                    val yPx = body.position.y * PPM
                    if (yPx > 0 && yPx < 150f && body.linearVelocity.length() < 0.5f) {
                        return true
                    }
                }
                body = body.next
            }
            return false
        }
    }

    /**
     * 创建真正的俄罗斯方块组合刚体
     * @param blockSizePx 单个小方格的边长 (像素)
     */
    fun createTetrisBody(xPx: Float, yPx: Float, shape: TetrisShape, blockSizePx: Float): Long {
        synchronized(lock) {
            val body = createDynamicBody(xPx, yPx)

            // 遍历该形状定义的 4 个小方块坐标，给同一个 Body 创建 4 个 Fixture
            shape.offsets.forEach { (offsetX, offsetY) ->
                val poly = PolygonShape()
                // setAsBox 的参数是半宽、半高，中心点位置，角度
                // 注意：Box2D 的坐标系和 setAsBox 的 center 参数都是以米为单位
                val halfSize = (blockSizePx / 2) / PPM
                val centerX = (offsetX * blockSizePx) / PPM
                val centerY = (offsetY * blockSizePx) / PPM

                poly.setAsBox(halfSize, halfSize, Vec2(centerX, centerY), 0f)

                val fixtureDef = FixtureDef().apply {
                    this.shape = poly
                    density = 1.0f
                    friction = 0.5f // 稍微增加摩擦，让堆叠更稳
                    restitution = 0.05f // 极低弹性，俄罗斯方块不应该乱弹
                }
                body.createFixture(fixtureDef)
            }

            // 绑定元数据
            body.userData = BodyMeta(
                type = EntityType.BOX,
                widthPx = blockSizePx, // 这里存单个格子的尺寸
                color = shape.color,
                tetrisShape = shape
            )

            return body.hashCode().toLong()
        }
    }

    fun clearDynamicBodies() {
        synchronized(lock) {
            var body = world.bodyList
            val bodiesToRemove = ArrayList<Body>()
            while (body != null) {
                if (body.type == BodyType.DYNAMIC) bodiesToRemove.add(body)
                body = body.next
            }
            for (b in bodiesToRemove) world.destroyBody(b)
            cachedEntities.clear()
        }
    }

    private fun clearStaticBodies() {
        var body = world.bodyList
        val bodiesToRemove = ArrayList<Body>()
        while (body != null) {
            if (body.type == BodyType.STATIC) bodiesToRemove.add(body)
            body = body.next
        }
        for (b in bodiesToRemove) world.destroyBody(b)
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
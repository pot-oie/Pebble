package com.pot.pebble.core.strategy

import com.pot.pebble.core.contract.InterferenceStrategy
import com.pot.pebble.core.model.EntityType
import com.pot.pebble.core.model.RenderEntity
import kotlin.random.Random

class SimpleGravityStrategy : InterferenceStrategy {

    // 所有的石头数据 (x, y, vx, vy)
    private val rocks = mutableListOf<RockData>()
    private var screenWidth = 1080f
    private var screenHeight = 1920f

    // 简单的内部数据类
    data class RockData(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        val id: Long
    )

    // 更新屏幕尺寸（从Service传进来）
    fun setScreenSize(w: Float, h: Float) {
        this.screenWidth = w
        this.screenHeight = h
    }

    override fun onStart() {
        // MVP: 开局直接生成 5 个石头在顶部随机位置
        repeat(5) {
            rocks.add(
                RockData(
                    x = Random.nextFloat() * screenWidth,
                    y = 0f, // 从顶端掉落
                    vx = 0f, vy = 0f,
                    id = it.toLong()
                )
            )
        }
    }

    override fun update(dt: Long, gx: Float, gy: Float): List<RenderEntity> {
        val dtSec = dt / 1000f // 毫秒转秒
        val gravityScale = 200f // 放大重力效果，不然掉太慢

        // 简单的物理模拟
        rocks.forEach { rock ->
            // 1. 根据传感器(gx, gy)更新速度
            // 注意：手机传感器的坐标系和屏幕绘制坐标系需要适配
            // 这里简单处理：gx影响x轴，gy影响y轴
            rock.vx -= gx * gravityScale * dtSec
            rock.vy += gy * gravityScale * dtSec

            // 2. 更新位置
            rock.x += rock.vx * dtSec
            rock.y += rock.vy * dtSec

            // 3. 简单的边界碰撞（碰到地板反弹）
            if (rock.y > screenHeight) {
                rock.y = screenHeight
                rock.vy = -rock.vy * 0.5f // 能量损耗，反弹一半力度
            }
            if (rock.x < 0 || rock.x > screenWidth) {
                rock.vx = -rock.vx * 0.5f
                rock.x = if (rock.x < 0) 0f else screenWidth
            }
        }

        // 4. 转换为渲染数据
        return rocks.map {
            RenderEntity(it.id, it.x, it.y, 0f, EntityType.CIRCLE)
        }
    }

    override fun onStop() {
        rocks.clear()
    }
}
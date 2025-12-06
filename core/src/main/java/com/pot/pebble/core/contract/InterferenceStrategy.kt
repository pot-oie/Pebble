package com.pot.pebble.core.contract

import com.pot.pebble.core.model.RenderEntity

/**
 * 干扰策略接口
 * 所有的玩法实现
 */
interface InterferenceStrategy {
    fun onStart()
    // dt = 距离上一帧过去的时间(毫秒), gx/gy = 重力传感器数据
    fun update(dt: Long, gx: Float, gy: Float): List<RenderEntity>
    fun onStop()
}
package com.pot.pebble.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.pot.pebble.core.model.RenderEntity

class PebbleOverlayView(context: Context) : View(context) {

    private val paint = Paint().apply {
        color = Color.BLACK // MVP 先用黑色实心球
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // 渲染列表
    private var entities: List<RenderEntity> = emptyList()

    fun updateState(newEntities: List<RenderEntity>) {
        this.entities = newEntities
        invalidate() // 通知系统重绘
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 遍历数据，画出来
        entities.forEach { entity ->
            // MVP: 暂时把所有的石头都画成半径 30px 的圆
            canvas.drawCircle(entity.x, entity.y, 30f, paint)
        }
    }
}
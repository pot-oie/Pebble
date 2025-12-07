package com.pot.pebble.ui.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.VectorDrawable
import android.view.View
import androidx.core.content.ContextCompat
import com.pot.pebble.R
import com.pot.pebble.core.model.RenderEntity

class PebbleOverlayView(context: Context) : View(context) {

    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true // 开启位图滤波，旋转时锯齿更少
        alpha = 255 // 透明度
    }

    private var entities: List<RenderEntity> = emptyList()

    // 缓存一个 Bitmap，不要在 onDraw 里重复创建
    private val rockBitmap: Bitmap by lazy {
        getBitmapFromVectorDrawable(context, R.drawable.ic_rock_pixel)
    }

    fun updateState(newEntities: List<RenderEntity>) {
        this.entities = newEntities
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        entities.forEach { entity ->
            // 保存画布当前状态
            canvas.save()

            // 1. 移动画布原点到石头的中心 (x, y)
            canvas.translate(entity.x, entity.y)

            // 2. 旋转画布 (entity.rotation 是角度)
            canvas.rotate(entity.rotation)

            // 3. 绘制图片
            // 因为我们已经把原点移到了中心，所以画图时要以 (0,0) 为中心向左上偏移半径
            val r = entity.radius
            // 定义图片要画多大 (目标矩形)
            val destRect = RectF(-r, -r, r, r)

            // 画出石头
            canvas.drawBitmap(rockBitmap, null, destRect, paint)

            // 恢复画布状态，准备画下一个
            canvas.restore()
        }
    }

    // 辅助方法：把 VectorDrawable 转成 Bitmap
    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId) as VectorDrawable
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
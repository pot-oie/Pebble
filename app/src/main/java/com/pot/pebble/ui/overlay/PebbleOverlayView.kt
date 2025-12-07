package com.pot.pebble.ui.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.pot.pebble.R
import com.pot.pebble.core.model.RenderEntity

class PebbleOverlayView(context: Context) : View(context) {

    // 画笔
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    // 调试画笔（当图片加载失败时用红色绘制）
    private val debugPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var entities: List<RenderEntity> = emptyList()

    // 🔥 修复：更健壮的 Bitmap 加载逻辑
    // 如果你没有 ic_rock_pixel 图片，请先随便放一张 png 进去，或者暂且容忍它画红球
    private val rockBitmap: Bitmap? by lazy {
        try {
            // 尝试加载资源
            val drawableId = R.drawable.ic_rock_pixel
            // 如果报错 "Resource not found"，请确保你 res/drawable 下有这个文件
            // 如果没有，可以临时改成 R.mipmap.ic_launcher 测试

            val drawable = ContextCompat.getDrawable(context, drawableId)

            when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                is VectorDrawable -> {
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
                else -> null // 不支持的格式或加载失败
            }
        } catch (e: Exception) {
            Log.e("PebbleOverlay", "Error loading rock bitmap: ${e.message}")
            null
        }
    }

    fun updateState(newEntities: List<RenderEntity>) {
        this.entities = newEntities
        // 强制重绘
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 🛡️ 如果列表为空，啥也不画
        if (entities.isEmpty()) return

        entities.forEach { entity ->
            canvas.save()
            // 移动到物体中心
            canvas.translate(entity.x, entity.y)
            canvas.rotate(entity.rotation)

            val r = entity.radius

            if (rockBitmap != null) {
                // ✅ 方案 A：图片加载成功，画图
                val destRect = RectF(-r, -r, r, r)
                canvas.drawBitmap(rockBitmap!!, null, destRect, paint)
            } else {
                // 🆘 方案 B：图片加载失败，画红色圆圈 (兜底)
                // 这样我们可以确认是“图的问题”还是“位置的问题”
                canvas.drawCircle(0f, 0f, r, debugPaint)
            }

            canvas.restore()
        }
    }
}
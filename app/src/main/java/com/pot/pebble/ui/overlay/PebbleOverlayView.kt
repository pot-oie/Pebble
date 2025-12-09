package com.pot.pebble.ui.overlay

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.text.TextPaint
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.pot.pebble.R
import com.pot.pebble.core.model.EntityType
import com.pot.pebble.core.model.RenderEntity
import java.util.Random

class PebbleOverlayView(context: Context) : View(context) {

    // --- 资源缓存 ---
    // 懒加载石头图片
    private val rockBitmap: Bitmap? by lazy { loadBitmap(R.drawable.ic_rock_pixel) }

    // 缓存用户自定义图片
    private var customBitmap: Bitmap? = null
    private var cachedUriString: String? = null

    // --- 画笔工具 ---
    private val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }

    // 专门用于画文字的画笔
    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // --- 渲染策略表 ---
    private val renderers = mapOf(
        EntityType.CIRCLE to RockRenderer(),
        EntityType.TEXT to TextRenderer(),
        EntityType.BOX to BoxRenderer(),
        EntityType.CRACK to CrackRenderer(),
        EntityType.CUSTOM to CustomRenderer()
    )

    private var entities: List<RenderEntity> = emptyList()

    fun updateState(newEntities: List<RenderEntity>) {
        this.entities = newEntities
        // 触发重绘
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (entities.isEmpty()) return

        // 遍历实体，分发给对应的渲染器
        entities.forEach { entity ->
            val renderer = renderers[entity.type]
            renderer?.draw(canvas, entity)
        }
    }

    // ==========================================
    //              渲染策略 (Renderers)
    // ==========================================

    interface IRenderer {
        fun draw(c: Canvas, e: RenderEntity)
    }

    /** 石头渲染器 (画图片) */
    inner class RockRenderer : IRenderer {
        override fun draw(c: Canvas, e: RenderEntity) {
            c.save()
            c.translate(e.x, e.y)
            c.rotate(e.rotation)

            val r = e.radius
            if (rockBitmap != null) {
                val destRect = RectF(-r, -r, r, r)
                c.drawBitmap(rockBitmap!!, null, destRect, paint)
            } else {
                // 兜底：画个深灰色的球
                paint.color = Color.DKGRAY
                paint.style = Paint.Style.FILL
                c.drawCircle(0f, 0f, r, paint)
            }
            c.restore()
        }
    }

    /** 弹幕渲染器 (画气泡 + 文字) */
    inner class TextRenderer : IRenderer {
        override fun draw(c: Canvas, e: RenderEntity) {
            c.save()
            c.translate(e.x, e.y)
            // 弹幕通常保持水平，或者你可以允许它随物理旋转 c.rotate(e.rotation)
            c.rotate(e.rotation)

            val w = e.width / 2
            val h = e.height / 2

            // 1. 画背景 (气泡)
            // 如果实体有颜色则用实体的，否则默认半透明黑
            paint.color = e.color ?: 0xAA000000.toInt()
            paint.style = Paint.Style.FILL
            val rect = RectF(-w, -h, w, h)
            c.drawRoundRect(rect, 16f, 16f, paint)

            // 2. 画文字
            val content = e.text ?: "FOCUS"
            val fontMetrics = textPaint.fontMetrics
            // 文字垂直居中计算
            val baseline = -fontMetrics.top / 2 - fontMetrics.bottom / 2
            c.drawText(content, 0f, baseline, textPaint)

            c.restore()
        }
    }

    /** 俄罗斯方块渲染器 (画彩色矩形) */
    inner class BoxRenderer : IRenderer {
        override fun draw(c: Canvas, e: RenderEntity) {
            c.save()
            c.translate(e.x, e.y)
            c.rotate(e.rotation)

            val shape = e.tetrisShape
            val blockSize = e.width // 这里的 width 存的是单个格子的边长
            val halfSize = blockSize / 2

            // 如果有形状定义，画组合体；如果没有(兼容旧数据)，画单个方块
            val offsets = shape?.offsets ?: listOf(0f to 0f)
            val baseColor = e.color ?: Color.GREEN

            // 准备画笔颜色
            paint.style = Paint.Style.FILL

            // 遍历 4 个小方块
            offsets.forEach { (offX, offY) ->
                // 计算小方块的中心坐标
                val cx = offX * blockSize
                val cy = offY * blockSize
                val rect = RectF(cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize)

                // 1. 画底色
                paint.color = baseColor
                paint.style = Paint.Style.FILL
                c.drawRect(rect, paint)

                // 2. 画高光 (左上边缘) - 让方块有立体感
                paint.color = Color.WHITE
                paint.alpha = 100
                paint.strokeWidth = 4f
                paint.style = Paint.Style.STROKE
                c.drawLine(rect.left, rect.bottom, rect.left, rect.top, paint)
                c.drawLine(rect.left, rect.top, rect.right, rect.top, paint)

                // 3. 画阴影 (右下边缘)
                paint.color = Color.BLACK
                paint.alpha = 80
                c.drawLine(rect.right, rect.top, rect.right, rect.bottom, paint)
                c.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paint)

                // 4. 画黑色分割线 (更像 8-bit 风格)
                paint.color = Color.BLACK
                paint.alpha = 255
                paint.strokeWidth = 2f
                c.drawRect(rect, paint)
            }

            paint.style = Paint.Style.FILL // 还原
            c.restore()
        }
    }

    /** 裂纹渲染器 (程序化绘制，无需图片) */
    inner class CrackRenderer : IRenderer {
        private val crackPath = Path()
        private val crackPaint = Paint().apply {
            color = 0xFFE0E0E0.toInt() // 亮灰白
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
            strokeJoin = Paint.Join.MITER
            strokeCap = Paint.Cap.BUTT
        }

        override fun draw(c: Canvas, e: RenderEntity) {
            c.save()
            c.translate(e.x, e.y)
            c.rotate(e.rotation)

            crackPath.reset()
            val baseSize = e.radius * 2

            // 使用实体的 ID 作为随机种子，保证同一个裂纹每次绘制形状都一样，不会闪烁
            val rng = Random(e.id)

            // 生成 5-8 条放射线
            val numBranches = 5 + rng.nextInt(4)
            for (i in 0 until numBranches) {
                crackPath.moveTo(0f, 0f)

                val angleStep = 360f / numBranches
                val baseAngle = i * angleStep + rng.nextFloat() * 30f
                val length = baseSize * (0.7f + rng.nextFloat() * 0.6f)

                val endX = (length * Math.cos(Math.toRadians(baseAngle.toDouble()))).toFloat()
                val endY = (length * Math.sin(Math.toRadians(baseAngle.toDouble()))).toFloat()

                // 画一条稍微弯曲的线
                val midX = endX / 2 + (rng.nextFloat() - 0.5f) * length * 0.3f
                val midY = endY / 2 + (rng.nextFloat() - 0.5f) * length * 0.3f
                crackPath.quadTo(midX, midY, endX, endY)
            }

            c.drawPath(crackPath, crackPaint)
            c.restore()
        }
    }

    // 加载自定义图片的辅助方法
    private fun loadCustomBitmap(uriString: String): Bitmap? {
        if (uriString == cachedUriString && customBitmap != null) {
            return customBitmap
        }

        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            // 计算目标尺寸，限制最大边长为 300px (兼顾清晰度和内存)
            val maxSide = 300f
            val scale = if (bitmap.width > bitmap.height) {
                maxSide / bitmap.width
            } else {
                maxSide / bitmap.height
            }

            val targetW = (bitmap.width * scale).toInt()
            val targetH = (bitmap.height * scale).toInt()

            // 按比例缩放，而不是强制正方形
            val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

            cachedUriString = uriString
            customBitmap = scaled
            scaled

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 自定义图片渲染器 */
    inner class CustomRenderer : IRenderer {
        override fun draw(c: Canvas, e: RenderEntity) {
            val uri = e.customImageUri ?: return
            val bitmap = loadCustomBitmap(uri) ?: return

            c.save()
            c.translate(e.x, e.y)
            c.rotate(e.rotation)

            // 使用 Entity 的物理宽高来绘制 (Strategy 已经算好了比例)
            val w = e.width
            val h = e.height
            val destRect = RectF(-w/2, -h/2, w/2, h/2)

            c.drawBitmap(bitmap, null, destRect, paint)

            c.restore()
        }
    }

    // --- 工具：加载 Bitmap ---
    private fun loadBitmap(resId: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, resId) ?: return null
            if (drawable is BitmapDrawable) return drawable.bitmap
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
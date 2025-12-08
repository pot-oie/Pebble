package com.pot.pebble.service.helper

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.pot.pebble.core.model.RenderEntity
import com.pot.pebble.ui.overlay.PebbleOverlayView

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val overlayView = PebbleOverlayView(context)
    private var isAdded = false
    private var isVisibleState = false // 记录当前逻辑上的显示状态

    // 预定义两种参数：全屏显示 / 1像素保活
    private lateinit var visibleParams: WindowManager.LayoutParams
    private lateinit var hiddenParams: WindowManager.LayoutParams

    fun setup() {
        if (isAdded) return

        // 初始化通用 Type
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        // 【显示态】参数：全屏、不可点击
        visibleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
        }

        // 【隐藏态】参数：1像素、放在角落、极低透明度
        hiddenParams = WindowManager.LayoutParams(
            1, // 宽 1px
            1, // 高 1px
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            alpha = 0.01f
            x = 0
            y = 0
        }

        // 默认先以【隐藏态】添加
        windowManager.addView(overlayView, hiddenParams)
        isAdded = true
        isVisibleState = false
    }

    fun updateRender(data: List<RenderEntity>) {
        if (!isAdded) return
        // 只有在逻辑可见时才更新画面
        if (isVisibleState) {
            overlayView.updateState(data)
        }
    }

    fun setVisible(visible: Boolean) {
        if (!isAdded) return
        if (isVisibleState == visible) return // 状态没变，忽略

        isVisibleState = visible

        if (visible) {
            // 切换到全屏模式
            overlayView.visibility = View.VISIBLE // 确保 View 内部也是可见的
            overlayView.alpha = 1.0f
            windowManager.updateViewLayout(overlayView, visibleParams)
        } else {
            // 切换到 1像素保活模式
            overlayView.alpha = 0.01f
            windowManager.updateViewLayout(overlayView, hiddenParams)
            // 清空画布，防止残留
            overlayView.updateState(emptyList())
        }
    }

    fun destroy() {
        if (isAdded) {
            windowManager.removeView(overlayView)
            isAdded = false
        }
    }
}
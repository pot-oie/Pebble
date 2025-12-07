package com.pot.pebble.service.helper

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.pot.pebble.core.model.RenderEntity
import com.pot.pebble.ui.overlay.PebbleOverlayView

// 悬浮窗管理组件
class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val overlayView = PebbleOverlayView(context)
    private var isAdded = false

    fun setup() {
        if (isAdded) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.LEFT
        windowManager.addView(overlayView, params)
        isAdded = true
        // 默认先隐藏，等检测到再显示
        setVisible(false)
    }

    fun updateRender(data: List<RenderEntity>) {
        overlayView.updateState(data)
    }

    fun setVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        if (overlayView.visibility != visibility) {
            overlayView.visibility = visibility
        }
    }

    fun destroy() {
        if (isAdded) {
            windowManager.removeView(overlayView)
            isAdded = false
        }
    }
}
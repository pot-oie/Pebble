package com.pot.pebble.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import com.pot.pebble.core.strategy.SimpleGravityStrategy
import com.pot.pebble.ui.overlay.PebbleOverlayView

class InterferenceService : Service(), SensorEventListener {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: PebbleOverlayView
    private lateinit var sensorManager: SensorManager

    // 核心策略
    private val strategy = SimpleGravityStrategy()

    // 游戏循环相关
    private val handler = Handler(Looper.getMainLooper())
    private var lastFrameTime = 0L
    private var currentGx = 0f
    private var currentGy = 0f // 默认重力

    // 循环任务
    private val loopRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val dt = if (lastFrameTime > 0) now - lastFrameTime else 16L
            lastFrameTime = now

            // 1. Core 计算下一帧
            val renderData = strategy.update(dt, currentGx, currentGy)

            // 2. UI 更新
            overlayView.updateState(renderData)

            // 3. 只有 Service 活着才继续循环
            handler.postDelayed(this, 16) // ~60 FPS
        }
    }

    override fun onCreate() {
        super.onCreate()

        // 1. 初始化传感器
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        // 2. 初始化悬浮窗 UI
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = PebbleOverlayView(this)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            // Android O (8.0) 以上必须用 TYPE_APPLICATION_OVERLAY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            // 关键 Flag：不获取焦点(FLAG_NOT_FOCUSABLE) + 允许触摸穿透(FLAG_NOT_TOUCHABLE)
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.LEFT

        windowManager.addView(overlayView, params)

        // 3. 初始化 Core
        val metrics = resources.displayMetrics
        strategy.setScreenSize(metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat())
        strategy.onStart()

        // 4. 启动循环
        lastFrameTime = System.currentTimeMillis()
        handler.post(loopRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(loopRunnable)
        windowManager.removeView(overlayView)
        sensorManager.unregisterListener(this)
        strategy.onStop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 传感器回调
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            // 简单的映射：手机竖着拿时，Y轴重力向下
            currentGx = it.values[0]
            currentGy = it.values[1]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
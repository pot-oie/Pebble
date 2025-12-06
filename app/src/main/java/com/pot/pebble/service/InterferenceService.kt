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
import com.pot.pebble.core.strategy.JBox2DStrategy // 确保这里引用的是 JBox2DStrategy
import com.pot.pebble.ui.overlay.PebbleOverlayView

class InterferenceService : Service(), SensorEventListener {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: PebbleOverlayView
    private lateinit var sensorManager: SensorManager

    // 策略切换为 JBox2DStrategy
    private val strategy = JBox2DStrategy()

    // 🔴 变化 1: 只需要一个主线程 Handler 用来发 UI 更新指令
    private val mainHandler = Handler(Looper.getMainLooper())

    // 🔴 变化 2: 增加一个线程控制标记
    private var isRunning = false

    // 🔴 变化 3: 独立的物理计算线程
    private val gameThread = Thread {
        while (isRunning) {
            val start = System.currentTimeMillis()

            // 1. 计算 (现在有锁了，很安全)
            val renderData = strategy.update(16, currentGx, currentGy) // dt 传多少无所谓了，内部固定了

            // 2. 发送给 UI (现在发的是快照，很安全)
            mainHandler.post {
                overlayView.updateState(renderData)
            }

            // 3. 稳定帧率 (Sleep)
            // 这一步是为了不让 CPU 100% 满负荷空转，给电池省点电
            val executionTime = System.currentTimeMillis() - start
            val targetDelay = 16L // 目标 60FPS
            if (executionTime < targetDelay) {
                try {
                    Thread.sleep(targetDelay - executionTime)
                } catch (e: Exception) {}
            }
        }
    }

    // 传感器数据 (简单做个 volatile 保证线程可见性)
    @Volatile private var currentGx = 0f
    @Volatile private var currentGy = 0f

    override fun onCreate() {
        super.onCreate()

        // ... (传感器和 WindowManager 初始化代码保持不变) ...
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = PebbleOverlayView(this)

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

        // 初始化策略
        val metrics = resources.displayMetrics
        strategy.setScreenSize(metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat())
        strategy.onStart()

        // 🔴 变化 4: 启动线程
        isRunning = true
        gameThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 🔴 变化 5: 停止线程
        isRunning = false
        // 等待线程安全结束（可选，Service destroy 很快）

        windowManager.removeView(overlayView)
        sensorManager.unregisterListener(this)
        strategy.onStop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            currentGx = it.values[0]
            currentGy = it.values[1]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
package com.pot.pebble.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import com.pot.pebble.core.strategy.JBox2DStrategy
import com.pot.pebble.monitor.AppUsageMonitor
import com.pot.pebble.service.helper.NotificationHelper
import com.pot.pebble.service.helper.OverlayManager
import com.pot.pebble.service.logic.GameEngine

class InterferenceService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var overlayManager: OverlayManager
    private lateinit var gameEngine: GameEngine

    // 保持 Strategy 的引用，传给 Engine
    private val strategy = JBox2DStrategy()

    override fun onCreate() {
        super.onCreate()

        // 1. 启动前台通知 (防止崩溃)
        notificationHelper = NotificationHelper(this)
        notificationHelper.startForeground()

        // 2. 初始化悬浮窗
        overlayManager = OverlayManager(this)
        overlayManager.setup()

        // 3. 初始化物理策略
        val metrics = resources.displayMetrics
        val statusBarHeight = getStatusBarHeight()
        val navBarHeight = getNavigationBarHeight()

        // 将屏幕总高度，以及上下边距传给策略
        strategy.setScreenSize(
            metrics.widthPixels.toFloat(),
            metrics.heightPixels.toFloat(),
            statusBarHeight.toFloat(),
            navBarHeight.toFloat() + 100f
        )
        strategy.onStart()

        // 4. 初始化传感器
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        // 5. 启动游戏引擎 (注入依赖)
        val usageMonitor = AppUsageMonitor(this)
        gameEngine = GameEngine(strategy, usageMonitor, overlayManager)
        gameEngine.start()
    }

    // 【新增辅助方法】获取状态栏高度
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    // 【新增辅助方法】获取导航栏高度
    private fun getNavigationBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        gameEngine.stop()
        overlayManager.destroy()
        sensorManager.unregisterListener(this)
        strategy.onStop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 传感器数据直接喂给 Engine
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            gameEngine.currentGx = it.values[0]
            gameEngine.currentGy = it.values[1]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
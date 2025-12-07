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
        strategy.setScreenSize(metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat())
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
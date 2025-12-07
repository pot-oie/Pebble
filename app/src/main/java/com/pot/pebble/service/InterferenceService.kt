package com.pot.pebble.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
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
import android.view.View
import android.view.WindowManager
import com.pot.pebble.core.strategy.JBox2DStrategy // 确保这里引用的是 JBox2DStrategy
import com.pot.pebble.monitor.AppUsageMonitor
import com.pot.pebble.ui.overlay.PebbleOverlayView

class InterferenceService : Service(), SensorEventListener {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: PebbleOverlayView
    private lateinit var sensorManager: SensorManager

    private val strategy = JBox2DStrategy()

    // 主线程 Handler 发 UI 更新指令
    private val mainHandler = Handler(Looper.getMainLooper())

    // 线程控制标记
    private var isRunning = false

    // 侦察兵
    private lateinit var usageMonitor: AppUsageMonitor

    // 黑名单列表 (测试)
    private val blackList = setOf(
        "com.google.android.youtube", // YouTube
        "com.ss.android.ugc.aweme",   // 抖音
        "com.android.chrome",         // Chrome (方便测试)
        "com.android.settings"        // 设置页 (极度方便测试！)
    )

    // 计时器变量
    private var punishTimer = 0L
    private val PUNISH_INTERVAL = 2000L // 每 2 秒惩罚一次

    // 独立的物理计算线程
    private val gameThread = Thread {
        while (isRunning) {
            val start = System.currentTimeMillis()

            // --- 🕵️‍♂️ 侦查阶段 ---
            // 每 1 秒查一次就行，不用每帧都查，省电
            if (System.currentTimeMillis() % 1000 < 20) {
                checkAppUsage()
            }

            // --- 🌍 物理阶段 ---
            val renderData = strategy.update(16, currentGx, currentGy)

            // --- 🎨 渲染阶段 ---
            mainHandler.post { overlayView.updateState(renderData) }

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

    private fun checkAppUsage() {
        val currentPkg = usageMonitor.getCurrentTopPackage()

        if (currentPkg != null && blackList.contains(currentPkg)) {
            // 😡 正在玩黑名单应用！
            punishTimer += 1000 // 累加时间

            if (punishTimer >= PUNISH_INTERVAL) {
                // ⏰ 时间到，执行惩罚！
                punishTimer = 0
                spawnPunishmentRock()
            }

            // 确保悬浮窗是可见的
            mainHandler.post {
                if (overlayView.visibility != View.VISIBLE) overlayView.visibility = View.VISIBLE
            }

        } else {
            // 😇 乖乖退出了，或者是桌面
            punishTimer = 0
            // 可以在这里清空石头 (需要去 PhysicsManager 加一个 clear 方法)
            // 或者直接隐藏 View
            mainHandler.post {
                // 这里为了效果明显，我们暂时做成“一退出就消失”
                if (overlayView.visibility == View.VISIBLE) overlayView.visibility = View.GONE
            }
            // TODO: 更好的做法是调用 strategy.clearRocks()
        }
    }

    private fun spawnPunishmentRock() {
        // 让 Strategy 暴露一个 addRock 方法，或者直接在这里通过 physics 生成
        // 现在的架构里，Service 没法直接调 physics.createRock。
        // 最好的办法是在 JBox2DStrategy 里加一个 public fun addRock()
        (strategy as? com.pot.pebble.core.strategy.JBox2DStrategy)?.addRandomRock()
    }

    // 传感器数据 (简单做个 volatile 保证线程可见性)
    @Volatile private var currentGx = 0f
    @Volatile private var currentGy = 0f

    /**
     * 🔥 【新增方法】创建通知渠道并启动前台服务
     */
    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "pebble_service_channel"
            val channelName = "Pebble 专注服务"

            // 创建通知渠道
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW // LOW 级别不会发出声音干扰用户
            ).apply {
                lightColor = Color.BLUE
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(channel)

            // 创建通知
            val notification: Notification = Notification.Builder(this, channelId)
                .setContentTitle("Pebble 正在运行")
                .setContentText("正在监测专注状态...")
                .setSmallIcon(com.pot.pebble.R.mipmap.ic_launcher) // 确保这里引用正确的图标资源
                .build()

            // 🔥 【修复点】根据 Android 版本选择不同的启动方式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
                // 显式声明类型：特殊用途
                startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                // 旧版本不需要类型
                startForeground(1, notification)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // 启动前台通知
        startForegroundNotification()

        // 传感器和 WindowManager 初始化
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

        // 初始化侦察兵
        usageMonitor = AppUsageMonitor(this)

        // 启动线程
        isRunning = true
        gameThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止线程
        isRunning = false

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
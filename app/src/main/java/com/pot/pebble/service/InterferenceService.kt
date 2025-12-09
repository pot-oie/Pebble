package com.pot.pebble.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.pot.pebble.core.strategy.JBox2DStrategy
import com.pot.pebble.data.AppDatabase
import com.pot.pebble.data.repository.AppScanner
import com.pot.pebble.monitor.UsageCollector
import com.pot.pebble.service.helper.NotificationHelper
import com.pot.pebble.service.helper.OverlayManager
import com.pot.pebble.service.logic.GameEngine
import kotlinx.coroutines.*

class InterferenceService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var overlayManager: OverlayManager
    private lateinit var gameEngine: GameEngine
    private lateinit var usageCollector: UsageCollector
    private lateinit var database: AppDatabase

    private val strategy = JBox2DStrategy()
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private lateinit var monitorThread: HandlerThread
    private lateinit var monitorHandler: Handler

    private val POLLING_INTERVAL = 1000L

    private var cachedBlacklist = emptySet<String>()

    private val pollRunnable = object : Runnable {
        override fun run() {
            performCheck()
            monitorHandler.postDelayed(this, POLLING_INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("PebbleDebug", "🚀 InterferenceService: onCreate")

        monitorThread = HandlerThread("PebbleMonitorThread", android.os.Process.THREAD_PRIORITY_FOREGROUND)
        monitorThread.start()
        monitorHandler = Handler(monitorThread.looper)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Pebble:KeepAlive")
        wakeLock?.acquire(10 * 60 * 60 * 1000L)

        notificationHelper = NotificationHelper(this)
        notificationHelper.startForeground()

        overlayManager = OverlayManager(this)
        overlayManager.setup()

        usageCollector = UsageCollector(this)

        val metrics = resources.displayMetrics
        val statusBarHeight = getStatusBarHeight()
        val navBarHeight = getNavigationBarHeight()
        strategy.setScreenSize(metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat(), statusBarHeight.toFloat(), navBarHeight.toFloat() + 20f)
        strategy.onStart()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        // GameEngine 会自己去读取最新的主题配置
        gameEngine = GameEngine(applicationContext, strategy, overlayManager)
        gameEngine.start()

        database = AppDatabase.getDatabase(this)
        serviceScope.launch {
            AppScanner(applicationContext, database.appConfigDao()).syncInstalledApps()
            val blackList = database.appConfigDao().getBlacklistedPackageList()
            cachedBlacklist = blackList.toSet()
            gameEngine.updateBlacklist(cachedBlacklist)
        }

        startPolling()
        ServiceState.isRunning.value = true
    }

    private fun startPolling() {
        monitorHandler.removeCallbacks(pollRunnable)
        ServiceState.startTime.value = System.currentTimeMillis()
        ServiceState.triggerCount.value = 0
        monitorHandler.post(pollRunnable)
    }

    private fun performCheck() {
        // 回归最简单的检查逻辑
        val detectedPkg = usageCollector.getTopPackageName()

        // 只要检测到包名变化，就更新状态
        if (detectedPkg != null && ServiceState.currentPackage.value != detectedPkg) {
            Log.d("PebbleDebug", "🔍 Switch Detected: $detectedPkg")
            ServiceState.currentPackage.tryEmit(detectedPkg)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorHandler.removeCallbacks(pollRunnable)
        monitorThread.quitSafely()
        gameEngine.stop()
        overlayManager.destroy()
        sensorManager.unregisterListener(this)
        strategy.onStop()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        ServiceState.isRunning.value = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLEAR_ROCKS) {
            gameEngine.clearRocks()
        }
        return START_STICKY
    }
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
        return result
    }
    private fun getNavigationBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
        return result
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { gameEngine.currentGx = it.values[0]; gameEngine.currentGy = it.values[1] }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    companion object { const val ACTION_CLEAR_ROCKS = "com.pot.pebble.action.CLEAR" }
}
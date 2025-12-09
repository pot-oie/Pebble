package com.pot.pebble.service.logic

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pot.pebble.core.model.EntityType
import com.pot.pebble.core.model.RenderEntity
import com.pot.pebble.core.strategy.JBox2DStrategy
import com.pot.pebble.data.AppDatabase
import com.pot.pebble.data.ThemeStore
import com.pot.pebble.data.entity.InterferenceLog
import com.pot.pebble.service.ServiceState
import com.pot.pebble.service.helper.OverlayManager
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

class GameEngine(
    private val context: Context,
    private val strategy: JBox2DStrategy,
    private val overlayManager: OverlayManager
) {

    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var observationJob: Job? = null

    private var physicsThread: Thread? = null
    @Volatile private var isPhysicsRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val PUNISH_INTERVAL = 600L
    private var punishTimer = 0L

    private var blackList: Set<String> = emptySet()

    @Volatile var currentGx = 0f
    @Volatile var currentGy = 0f
    private val MIN_GRAVITY = 5.0f

    var currentMode: EntityType = EntityType.CIRCLE

    private val staticEntities = CopyOnWriteArrayList<RenderEntity>()

    private val analyticsDao = AppDatabase.getDatabase(context).analyticsDao()

    init {
        ThemeStore.init(context)

        // 监听主题
        engineScope.launch {
            ThemeStore.currentTheme.collect { theme ->
                currentMode = theme
                // 每次切主题，最好清空一下之前的残留
                if (theme != EntityType.CRACK) {
                    strategy.clearAllBodies()
                    mainHandler.post { overlayManager.updateRender(emptyList()) }
                }
            }
        }

        // 监听弹幕
        engineScope.launch {
            ThemeStore.danmakuList.collect { list -> strategy.updateDanmakuList(list) }
        }

        // 监听图片并计算比例
        engineScope.launch {
            ThemeStore.customImageUri.collect { uriString ->
                if (uriString != null) {
                    // 计算宽高比
                    val ratio = calculateAspectRatio(uriString)
                    // 调用新方法 updateCustomImage (带比例参数)
                    strategy.updateCustomImage(uriString, ratio)
                    Log.d("PebbleEngine", "Custom Image Updated: ratio=$ratio")
                } else {
                    strategy.updateCustomImage(null, 1.0f)
                }
            }
        }
    }

    // 辅助方法，只读取图片尺寸，不加载内容（高效）
    private fun calculateAspectRatio(uriString: String): Float {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true // 关键：只读尺寸
            }
            val inputStream = context.contentResolver.openInputStream(Uri.parse(uriString))
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (options.outWidth > 0 && options.outHeight > 0) {
                options.outWidth.toFloat() / options.outHeight.toFloat()
            } else {
                1.0f
            }
        } catch (e: Exception) {
            e.printStackTrace()
            1.0f
        }
    }

    fun start() {
        if (observationJob?.isActive == true) return
        observationJob = engineScope.launch {
            ServiceState.currentPackage.collect { currentPkg ->
                if (currentPkg != null) processPackageChange(currentPkg)
            }
        }
    }

    fun stop() {
        observationJob?.cancel()
        stopPhysicsThread()
        mainHandler.post { overlayManager.setVisible(false) }
    }

    fun updateBlacklist(newSet: Set<String>) {
        this.blackList = newSet
        val current = ServiceState.currentPackage.value
        if (current != null) engineScope.launch { processPackageChange(current) }
    }

    fun clearRocks() {
        strategy.clearAllBodies()
        staticEntities.clear()
        mainHandler.post { overlayManager.updateRender(emptyList()) }
    }

    private fun processPackageChange(packageName: String) {
        if (blackList.contains(packageName)) {
            if (!isPhysicsRunning) {
                if (currentMode != EntityType.CRACK) strategy.clearAllBodies()
                ServiceState.triggerCount.value += 1
                startPhysicsThread(packageName)
            }
            mainHandler.post { overlayManager.setVisible(true) }
        } else {
            if (isPhysicsRunning) {
                stopPhysicsThread()
                staticEntities.clear()
                mainHandler.post { overlayManager.setVisible(false) }
            }
        }
    }

    private fun startPhysicsThread(packageName: String) {
        if (isPhysicsRunning) return
        isPhysicsRunning = true

        engineScope.launch {
            analyticsDao.insertLog(
                InterferenceLog(
                    timestamp = System.currentTimeMillis(),
                    type = 0,
                    packageName = packageName
                )
            )
        }

        physicsThread = Thread {
            var lastTime = System.currentTimeMillis()

            while (isPhysicsRunning) {
                val now = System.currentTimeMillis()
                val dt = now - lastTime
                lastTime = now
                val safeDt = if (dt > 50) 50 else dt

                try {
                    val finalRenderList: List<RenderEntity>

                    if (currentMode == EntityType.CRACK) {
                        punishTimer += dt
                        if (punishTimer >= 2000) {
                            punishTimer = 0
                            val crack = strategy.createStaticCrack()
                            if (crack != null) staticEntities.add(crack)
                        }
                        finalRenderList = staticEntities
                    } else {
                        val finalGy = if (currentGy < MIN_GRAVITY) MIN_GRAVITY else currentGy
                        val physicsEntities = strategy.update(safeDt, currentGx, finalGy)

                        punishTimer += dt
                        if (punishTimer >= PUNISH_INTERVAL) {
                            punishTimer = 0
                            if (!strategy.isFull()) {
                                strategy.spawnObstacle(currentMode)
                            }
                        }
                        finalRenderList = physicsEntities
                    }
                    mainHandler.post { overlayManager.updateRender(finalRenderList) }

                } catch (e: Exception) { e.printStackTrace() }

                val executionTime = System.currentTimeMillis() - now
                val targetDelay = 16L
                if (executionTime < targetDelay) {
                    try { Thread.sleep(targetDelay - executionTime) } catch (e: Exception) {}
                }
            }
        }.apply { start() }
    }

    private fun stopPhysicsThread() {
        isPhysicsRunning = false
        try { physicsThread?.join(200) } catch (e: Exception) {}
        physicsThread = null
    }
}
package com.pot.pebble.service.logic

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pot.pebble.core.strategy.JBox2DStrategy
import com.pot.pebble.service.ServiceState
import com.pot.pebble.service.helper.OverlayManager
import kotlinx.coroutines.*

class GameEngine(
    private val strategy: JBox2DStrategy,
    private val overlayManager: OverlayManager
) {

    // 协程作用域：用于监听状态流
    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var observationJob: Job? = null

    // 物理线程：只在惩罚时启动
    private var physicsThread: Thread? = null
    @Volatile private var isPhysicsRunning = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val PUNISH_INTERVAL = 2000L
    private var punishTimer = 0L

    // 黑名单
    private var blackList: Set<String> = emptySet()

    // 传感器数据
    @Volatile var currentGx = 0f
    @Volatile var currentGy = 0f
    private val MIN_GRAVITY = 5.0f

    /**
     * 启动引擎：开始监听无障碍服务的信号
     */
    fun start() {
        if (observationJob?.isActive == true) return

        Log.d("PebbleEngine", "Engine Started (Reactive Mode)")

        // 🔥 核心改变：不再轮询，而是观察流
        observationJob = engineScope.launch {
            ServiceState.currentPackage.collect { currentPkg ->
                if (currentPkg != null) {
                    processPackageChange(currentPkg)
                }
            }
        }
    }

    /**
     * 停止引擎
     */
    fun stop() {
        Log.d("PebbleEngine", "Engine Stopped")
        observationJob?.cancel()
        stopPhysicsThread()
        mainHandler.post { overlayManager.setVisible(false) }
    }

    fun updateBlacklist(newSet: Set<String>) {
        this.blackList = newSet
        // 黑名单更新时，手动触发一次检查当前状态
        val current = ServiceState.currentPackage.value
        if (current != null) {
            engineScope.launch { processPackageChange(current) }
        }
    }

    fun clearRocks() {
        strategy.clearAllBodies()
    }

    // --- 响应逻辑 ---

    private fun processPackageChange(packageName: String) {
        if (blackList.contains(packageName)) {
            // 🚨 命中黑名单：启动物理世界
            // Log.d("PebbleEngine", "Target Detected: $packageName")

            if (!isPhysicsRunning) {
                startPhysicsThread()
            }
            mainHandler.post { overlayManager.setVisible(true) }

        } else {
            // ✅ 安全应用：关闭物理世界
            // Log.d("PebbleEngine", "Safe App: $packageName")

            if (isPhysicsRunning) {
                stopPhysicsThread()
                mainHandler.post { overlayManager.setVisible(false) }
            }
        }
    }

    // --- 物理线程 (保持不变) ---

    private fun startPhysicsThread() {
        if (isPhysicsRunning) return
        isPhysicsRunning = true
        Log.w("PebbleEngine", "🔥 Physics Thread START")

        physicsThread = Thread {
            while (isPhysicsRunning) {
                val start = System.currentTimeMillis()
                try {
                    // 1. 物理步进
                    val finalGy = if (currentGy < MIN_GRAVITY) MIN_GRAVITY else currentGy
                    val renderData = strategy.update(16, currentGx, finalGy)

                    // 2. 渲染更新
                    mainHandler.post { overlayManager.updateRender(renderData) }

                    // 3. 自动生成石头逻辑 (放在这里比放在外部 Timer 更准)
                    punishTimer += 16
                    if (punishTimer >= PUNISH_INTERVAL) {
                        punishTimer = 0
                        if (!strategy.isFull()) {
                            strategy.addRandomRock()
                        }
                    }

                } catch (e: Exception) { e.printStackTrace() }

                // 稳帧
                val executionTime = System.currentTimeMillis() - start
                val targetDelay = 16L
                if (executionTime < targetDelay) {
                    try { Thread.sleep(targetDelay - executionTime) } catch (e: Exception) {}
                }
            }
            Log.w("PebbleEngine", "💤 Physics Thread STOP")
        }.apply { start() }
    }

    private fun stopPhysicsThread() {
        isPhysicsRunning = false
        try { physicsThread?.join(200) } catch (e: Exception) {}
        physicsThread = null
    }
}
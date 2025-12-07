package com.pot.pebble.service.logic

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pot.pebble.core.strategy.JBox2DStrategy
import com.pot.pebble.monitor.AppUsageMonitor
import com.pot.pebble.service.helper.OverlayManager

class GameEngine(
    private val strategy: JBox2DStrategy,
    private val usageMonitor: AppUsageMonitor,
    private val overlayManager: OverlayManager
) {

    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // 参数配置
    private val PUNISH_INTERVAL = 2000L
    private val GRACE_PERIOD = 3000L
    private val CHECK_INTERVAL = 500L   // 稳定的检测间隔

    // 状态变量
    private var punishTimer = 0L
    private var lastSeenBlacklistTime = 0L
    private var lastCheckTime = 0L

    // 黑名单
    private var blackList: Set<String> = emptySet()

    fun updateBlacklist(newSet: Set<String>) {
        this.blackList = newSet
        Log.d("PebbleDebug", "Blacklist updated: size=${newSet.size}")
    }

    @Volatile var currentGx = 0f
    @Volatile var currentGy = 0f
    private val MIN_GRAVITY = 5.0f

    private val gameThread = Thread {
        Log.w("PebbleDebug", "=== Game Thread Started ===")

        while (isRunning) {
            val start = System.currentTimeMillis()

            // 🔥【防崩溃护盾】全包裹 try-catch
            try {
                processGameLogic()

                val finalGy = if (currentGy < MIN_GRAVITY) MIN_GRAVITY else currentGy

                // 物理更新
                val renderData = strategy.update(16, currentGx, finalGy)

                // 渲染
                mainHandler.post { overlayManager.updateRender(renderData) }

            } catch (e: Exception) {
                // 🛑 如果发生崩溃，这里会接住，并告诉你原因！
                Log.e("PebbleDebug", "CRASH CAUGHT! Thread stays alive. Error: ${e.message}")
                e.printStackTrace()
            }

            // 稳帧逻辑
            val executionTime = System.currentTimeMillis() - start
            val targetDelay = 16L
            if (executionTime < targetDelay) {
                try { Thread.sleep(targetDelay - executionTime) } catch (e: Exception) {}
            }
        }
        Log.w("PebbleDebug", "=== Game Thread Stopped ===")
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        lastCheckTime = System.currentTimeMillis()
        gameThread.start()
    }

    fun stop() {
        isRunning = false
    }

    private fun processGameLogic() {
        val now = System.currentTimeMillis()

        // 使用时间差判定 (比 % 500 更稳定)
        if (now - lastCheckTime >= CHECK_INTERVAL) {
            lastCheckTime = now

            // 🕵️ 调试日志：尝试获取包名
            // Log.v("PebbleDebug", "Checking package...")

            val currentPkg = usageMonitor.getCurrentTopPackage()

            if (currentPkg == null) {
                // 如果获取不到，打印一下，看看是不是这里出了问题
                // Log.w("PebbleDebug", "Package detection returned NULL")
                return
            }

            // ✅ 成功获取到包名，打印出来
            Log.d("PebbleDebug", "Detected: $currentPkg")

            if (blackList.contains(currentPkg)) {
                lastSeenBlacklistTime = now
                mainHandler.post { overlayManager.setVisible(true) }

                punishTimer += CHECK_INTERVAL
                if (punishTimer >= PUNISH_INTERVAL) {
                    punishTimer = 0
                    if (!strategy.isFull()) {
                        strategy.addRandomRock()
                        Log.d("PebbleDebug", ">>> Rock DROP! (Screen not full)")
                    } else {
                        Log.d("PebbleDebug", ">>> Screen Full, waiting...")
                    }
                }
            } else {
                if (now - lastSeenBlacklistTime > GRACE_PERIOD) {
                    punishTimer = 0
                    mainHandler.post { overlayManager.setVisible(false) }
                }
            }
        }
    }
}
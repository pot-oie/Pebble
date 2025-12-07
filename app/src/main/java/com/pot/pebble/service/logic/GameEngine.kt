package com.pot.pebble.service.logic

import android.os.Handler
import android.os.Looper
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

    // 配置参数
    private val PUNISH_INTERVAL = 2000L // 2秒生成一个
    private val GRACE_PERIOD = 3000L    // 3秒宽限期 (防闪烁关键！)

    // 状态变量
    private var punishTimer = 0L
    private var lastSeenBlacklistTime = 0L // 上次见到黑名单应用的时间

    // 黑名单
    private val blackList = setOf(
        "com.google.android.youtube",
        "com.ss.android.ugc.aweme",
        "com.android.chrome",
        "com.android.settings",
        "com.google.android.apps.photos" // 相册也加上方便测试
    )

    // 物理参数
    @Volatile var currentGx = 0f
    @Volatile var currentGy = 0f

    private val gameThread = Thread {
        while (isRunning) {
            val start = System.currentTimeMillis()

            // 1. 逻辑检测 (每帧都跑，但在内部做时间控制)
            processGameLogic()

            // 2. 物理更新
            val renderData = strategy.update(16, currentGx, currentGy)

            // 3. 渲染
            mainHandler.post { overlayManager.updateRender(renderData) }

            // 4. 稳帧
            val executionTime = System.currentTimeMillis() - start
            val targetDelay = 16L
            if (executionTime < targetDelay) {
                try { Thread.sleep(targetDelay - executionTime) } catch (e: Exception) {}
            }
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        gameThread.start()
    }

    fun stop() {
        isRunning = false
    }

    private fun processGameLogic() {
        // 限制检测频率：每 500ms 检测一次包名足够了，太快也没用
        if (System.currentTimeMillis() % 500 < 20) {
            val currentPkg = usageMonitor.getCurrentTopPackage()
            val now = System.currentTimeMillis()

            if (currentPkg != null && blackList.contains(currentPkg)) {
                // -> 正在玩黑名单应用
                lastSeenBlacklistTime = now // 刷新最后目击时间

                // 确保悬浮窗显示
                mainHandler.post { overlayManager.setVisible(true) }

                // 累加惩罚计时
                punishTimer += 500
                if (punishTimer >= PUNISH_INTERVAL) {
                    punishTimer = 0
                    strategy.addRandomRock() // 生成石头
                }
            } else {
                // -> 没检测到黑名单 (可能是 null，可能是桌面，可能是瞬时切换)

                // 🔥【核心修复逻辑】宽限期判断
                // 只有当“当前时间”距离“上次目击黑名单时间”超过 3秒，才真正认为用户退出了
                if (now - lastSeenBlacklistTime > GRACE_PERIOD) {
                    punishTimer = 0 // 重置惩罚计时
                    // 隐藏石头 (石头还在内存里，只是不显示，下次出来还在，符合逻辑)
                    mainHandler.post { overlayManager.setVisible(false) }

                    // 如果你想彻底清空石头，可以在这里调用 strategy.clearRocks()
                } else {
                    // 在宽限期内，保持 View 可见，但不增加惩罚计时，也不生成新石头
                    // 这样石头不会“空中消失”，体验会很连贯
                }
            }
        }
    }
}
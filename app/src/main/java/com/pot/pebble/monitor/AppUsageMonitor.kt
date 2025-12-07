package com.pot.pebble.monitor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.util.Log

class AppUsageMonitor(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // 🔥 新增：用于缓存上一次检测到的应用包名
    private var lastKnownPackage: String? = null

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * 获取当前前台的应用包名
     * 改进策略：
     * 1. 如果是第一次查（lastKnownPackage == null），查过去 1 小时，确保能拿到长停留应用。
     * 2. 如果已经有记录，只查过去 1 分钟（节省性能）。
     * 3. 如果查不到新事件，说明用户没有切换应用，直接返回上一次的包名。
     */
    fun getCurrentTopPackage(): String? {
        val endTime = System.currentTimeMillis()

        // 🔥 动态调整时间窗口：
        // 如果我们不知道当前是谁(刚启动)，就查久一点(1小时)以防漏掉；
        // 如果我们已经知道当前是谁，只需要查最近(1分钟)有没有发生切换事件。
        val timeRange = if (lastKnownPackage == null) {
            60 * 60 * 1000L // 1小时
        } else {
            60 * 1000L      // 1分钟
        }

        val startTime = endTime - timeRange

        val events = usageStatsManager.queryEvents(startTime, endTime) ?: return lastKnownPackage

        val event = UsageEvents.Event()
        var latestEventTime = 0L
        var foundNewPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // 只关注“移动到前台”事件
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > latestEventTime) {
                    latestEventTime = event.timeStamp
                    foundNewPackage = event.packageName
                }
            }
        }

        // 逻辑判定：
        return if (foundNewPackage != null) {
            // 发现了新应用切换，更新缓存
            lastKnownPackage = foundNewPackage
            foundNewPackage
        } else {
            // 最近 1 分钟没有切换，说明用户还在原来的应用里，返回缓存
            lastKnownPackage
        }
    }
}
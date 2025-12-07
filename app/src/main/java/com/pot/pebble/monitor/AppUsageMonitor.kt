package com.pot.pebble.monitor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

class AppUsageMonitor(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * 检查是否有权限
     */
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * 跳转到授权页面
     */
    fun requestPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * 获取当前前台的应用包名
     * 原理：用 queryEvents 查询系统事件流，精准捕获“Activity 移动到前台”的瞬间
     */
    fun getCurrentTopPackage(): String? {
        val endTime = System.currentTimeMillis()
        // 查最近 1 分钟的事件（范围给大一点没关系，因为我们会找最新的那个）
        val startTime = endTime - 60 * 1000

        val events = usageStatsManager.queryEvents(startTime, endTime) ?: return null

        val event = UsageEvents.Event()
        var lastPackageName: String? = null
        var lastEventTime = 0L

        // 遍历这1分钟内发生的所有事件
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // 我们只关心“移动到前台 (MOVE_TO_FOREGROUND)”这件事
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                // 找到发生时间最晚（最新）的那一次
                if (event.timeStamp > lastEventTime) {
                    lastEventTime = event.timeStamp
                    lastPackageName = event.packageName
                }
            }
        }

        return lastPackageName
    }
}
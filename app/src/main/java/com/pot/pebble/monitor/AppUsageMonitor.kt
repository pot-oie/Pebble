package com.pot.pebble.monitor

import android.app.AppOpsManager
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
     * 原理：查询最近 2 秒内的使用记录，按“最后使用时间”排序，取第一个
     */
    fun getCurrentTopPackage(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 2000 // 查最近 2 秒

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        if (usageStatsList.isNullOrEmpty()) return null

        // 找到最后使用时间最近的那个 APP
        val sortedStats = usageStatsList.sortedByDescending { it.lastTimeUsed }
        return sortedStats.firstOrNull()?.packageName
    }
}
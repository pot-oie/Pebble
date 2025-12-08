package com.pot.pebble.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

class UsageCollector(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun getTopPackageName(): String? {
        val endTime = System.currentTimeMillis()
        // 查过去 10 秒。
        val startTime = endTime - 1000 * 10

        val events = usageStatsManager.queryEvents(startTime, endTime)
        if (events == null) return null

        val event = UsageEvents.Event()
        var lastPkg: String? = null
        var lastTimeStamp = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            // 逻辑不变，依然只看 ACTIVITY_RESUMED
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.timeStamp > lastTimeStamp) {
                    lastTimeStamp = event.timeStamp
                    lastPkg = event.packageName
                }
            }
        }

        if (lastPkg == null) {
            return getTopPackageFromStats()
        }

        return lastPkg
    }

    private fun getTopPackageFromStats(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}
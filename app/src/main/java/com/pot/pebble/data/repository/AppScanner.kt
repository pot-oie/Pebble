package com.pot.pebble.data.repository

import android.content.Context
import android.content.Intent
import com.pot.pebble.data.dao.AppConfigDao
import com.pot.pebble.data.entity.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScanner(
    private val context: Context,
    private val dao: AppConfigDao
) {

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager

        // 1. 获取所有有启动入口的应用 (过滤掉系统后台服务)
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        // 2. 转换为 Entity 列表
        val detectedApps = resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val packageName = activityInfo.packageName

            // 排除自己，别把自己禁了
            if (packageName == context.packageName) return@mapNotNull null

            val appName = resolveInfo.loadLabel(pm).toString()

            AppConfig(
                packageName = packageName,
                appName = appName,
                isBlacklisted = false // 默认为 false，如果数据库里已有，会被 IGNORE
            )
        } // 去重，防止同一个应用有多个入口导致主键冲突
            .distinctBy { it.packageName }

        // 3. 插入数据库 (策略是 IGNORE，所以不会覆盖旧数据)
        dao.insertConfigs(detectedApps)
    }
}
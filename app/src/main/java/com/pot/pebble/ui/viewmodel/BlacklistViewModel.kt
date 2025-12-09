package com.pot.pebble.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pot.pebble.data.AppDatabase
import com.pot.pebble.data.entity.AppConfig
import com.pot.pebble.data.repository.AppScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class BlacklistViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val configDao = db.appConfigDao()
    private val analyticsDao = db.analyticsDao()
    private val scanner = AppScanner(application, configDao)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 应用列表逻辑 (保持不变)
    private val _allApps = configDao.getAllConfigsFlow()

    val totalBlacklistedCount: StateFlow<Int> = _allApps
        .map { list -> list.count { it.isBlacklisted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appList: StateFlow<List<AppConfig>> = combine(_allApps, _searchQuery) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter { it.appName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 统计数据 ---

    // 1. 今日数据 (实时 Flow)
    val todayStats = flow {
        // 每分钟更新一次今日起始时间，防止跨天时数据不刷新
        while (true) {
            emit(getStartOfDay())
            kotlinx.coroutines.delay(60000)
        }
    }.flatMapLatest { startOfDay ->
        combine(
            analyticsDao.getInterferenceCountSince(startOfDay),
            analyticsDao.getFocusDurationSince(startOfDay)
        ) { count, duration ->
            Pair(count, duration ?: 0L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0, 0L))

    // 2. 7日趋势数据
    // 返回一个 List<Int>，代表过去7天(包括今天)每天的拦截次数
    val weekTrend = flow {
        emit(getStartOf7DaysAgo())
    }.flatMapLatest { startOf7Days ->
        analyticsDao.getLogsSinceFlow(startOf7Days).map { logs ->
            processWeekTrend(logs)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0 })

    init {
        refreshApps()
    }

    // --- 辅助逻辑 ---

    private fun getStartOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getStartOf7DaysAgo(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6) // 往前推6天，加上今天共7天
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun processWeekTrend(logs: List<com.pot.pebble.data.entity.InterferenceLog>): List<Int> {
        val result = IntArray(7) { 0 }
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        logs.filter { it.type == 0 }.forEach { log -> // 只统计落石(type=0)
            calendar.timeInMillis = log.timestamp
            val logDay = calendar.get(Calendar.DAY_OF_YEAR)
            val logYear = calendar.get(Calendar.YEAR)

            // 计算是几天前 (0 = 今天, 1 = 昨天...)
            // 简单处理跨年逻辑
            val diff = if (year == logYear) {
                today - logDay
            } else {
                today + (if (isLeap(logYear)) 366 else 365) - logDay
            }

            if (diff in 0..6) {
                // 存入数组，6-diff 是为了让 result[6] 存放今天，result[0] 存放6天前
                result[6 - diff]++
            }
        }
        return result.toList()
    }

    private fun isLeap(year: Int) = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun refreshApps() { viewModelScope.launch { scanner.syncInstalledApps() } }
    fun toggleBlacklist(app: AppConfig, isChecked: Boolean) {
        viewModelScope.launch { configDao.updateStatus(app.packageName, isChecked) }
    }
}
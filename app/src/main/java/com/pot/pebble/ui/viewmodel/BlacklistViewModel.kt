package com.pot.pebble.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pot.pebble.data.AppDatabase
import com.pot.pebble.data.entity.AppConfig
import com.pot.pebble.data.repository.AppScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlacklistViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).appConfigDao()
    private val scanner = AppScanner(application, dao)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 原始数据源
    private val _allApps = dao.getAllConfigsFlow()

    // 使用 map 从原始数据流中实时计算
    val totalBlacklistedCount: StateFlow<Int> = _allApps
        .map { list -> list.count { it.isBlacklisted } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 过滤后的显示列表
    val appList: StateFlow<List<AppConfig>> = combine(_allApps, _searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshApps()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun refreshApps() {
        viewModelScope.launch {
            scanner.syncInstalledApps()
        }
    }

    fun toggleBlacklist(app: AppConfig, isChecked: Boolean) {
        viewModelScope.launch {
            dao.updateStatus(app.packageName, isChecked)
        }
    }
}
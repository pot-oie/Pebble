package com.pot.pebble.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pot.pebble.common.SYSTEM_PROTECTED_PACKAGES
import com.pot.pebble.data.AppDatabase
import com.pot.pebble.data.entity.AppConfig
import com.pot.pebble.data.repository.AppScanner
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlacklistViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).appConfigDao()
    private val scanner = AppScanner(application, dao)

    // 将 Flow 转换为 StateFlow 供 UI 观察
    // initialValue 是空列表，等待数据库加载
    val appList: StateFlow<List<AppConfig>> = dao.getAllConfigsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // 初始化时扫描已安装应用
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch {
            // 1. 扫描安装的应用
            scanner.syncInstalledApps()

            // 2. ✨ 自动修正：把误入黑名单的系统应用踢出去
            SYSTEM_PROTECTED_PACKAGES.forEach { pkg ->
                dao.updateStatus(pkg, false)
            }
        }
    }

    fun toggleBlacklist(app: AppConfig, isChecked: Boolean) {
        viewModelScope.launch {
            dao.updateStatus(app.packageName, isChecked)
        }
    }
}
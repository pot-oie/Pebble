package com.pot.pebble.service

import kotlinx.coroutines.flow.MutableStateFlow

object ServiceState {
    // Service 是否正在运行 (UI 按钮状态)
    val isRunning = MutableStateFlow(false)

    // 当前前台应用的包名 (由无障碍服务实时推送)
    val currentPackage = MutableStateFlow<String?>(null)
}
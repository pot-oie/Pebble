package com.pot.pebble.service

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 全局单例，用于 UI 和 Service 之间的简单状态同步
 */
object ServiceState {
    // Service 是否正在运行？UI 观察这个变量来改变 FAB 按钮的颜色和文字
    val isRunning = MutableStateFlow(false)
}
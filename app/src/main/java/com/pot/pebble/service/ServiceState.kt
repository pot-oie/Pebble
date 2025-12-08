package com.pot.pebble.service

import kotlinx.coroutines.flow.MutableStateFlow

object ServiceState {
    // Service 是否正在运行
    val isRunning = MutableStateFlow(false)

    // 当前前台应用的包名
    val currentPackage = MutableStateFlow<String?>(null)

    // 专注开始时间 (计算时长)
    val startTime = MutableStateFlow(0L)

    // 本次触发次数 (落石数)
    val triggerCount = MutableStateFlow(0)
}
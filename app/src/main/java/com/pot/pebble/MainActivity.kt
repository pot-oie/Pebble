package com.pot.pebble

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pot.pebble.monitor.AppUsageMonitor
import com.pot.pebble.service.InterferenceService
import com.pot.pebble.service.ServiceState
import com.pot.pebble.ui.screen.BlacklistScreen
import com.pot.pebble.ui.screen.GuideScreen
import com.pot.pebble.ui.theme.MossGreen
import com.pot.pebble.ui.theme.PebbleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val monitor = AppUsageMonitor(this)

        setContent {
            PebbleTheme {
                // 观察服务状态
                val isServiceRunning by ServiceState.isRunning.collectAsState()

                // 状态导航
                var currentScreen by remember {
                    mutableStateOf(
                        if (checkAllPermissions(monitor)) Screen.Home else Screen.Guide
                    )
                }

                when (currentScreen) {
                    Screen.Guide -> {
                        GuideScreen(
                            onAllGranted = { currentScreen = Screen.Home }
                        )
                    }
                    Screen.Home -> {
                        // 主页布局
                        Scaffold(
                            floatingActionButton = {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        if (isServiceRunning) {
                                            stopPebbleService()
                                        } else {
                                            // ✅ 调用修改后的带参方法
                                            startPebbleService(monitor)
                                        }
                                    },
                                    // 状态颜色切换：运行中红，停止绿
                                    containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else MossGreen, // ✅ 修复 MaterialTheme
                                    contentColor = Color.White,
                                    icon = {
                                        Icon(
                                            imageVector = if (isServiceRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text(if (isServiceRunning) "停止专注" else "启动专注") }
                                )
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                BlacklistScreen(
                                    onNavigateToSettings = { currentScreen = Screen.Guide }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAllPermissions(monitor: AppUsageMonitor): Boolean {
        if (!Settings.canDrawOverlays(this)) return false
        if (!monitor.hasPermission()) return false
        return true
    }

    private fun startPebbleService(monitor: AppUsageMonitor) {
        // 双重保险检查
        if (!Settings.canDrawOverlays(this)) return

        val intent = Intent(this, InterferenceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        moveTaskToBack(true)
    }

    private fun stopPebbleService() {
        val intent = Intent(this, InterferenceService::class.java)
        stopService(intent)
    }
}

enum class Screen {
    Guide, Home
}
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.pot.pebble.monitor.AppUsageMonitor
import com.pot.pebble.service.InterferenceService
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
                // 状态管理
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
                                    onClick = { startPebbleService() },
                                    containerColor = MossGreen,
                                    contentColor = androidx.compose.ui.graphics.Color.White,
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow, // 或者 Icons.Default.Security
                                            contentDescription = null
                                        )
                                    },
                                    text = { Text("启动专注") }
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
        // 1. 悬浮窗
        if (!Settings.canDrawOverlays(this)) return false
        // 2. 使用记录
        if (!monitor.hasPermission()) return false
        return true
    }

    private fun startPebbleService() {
        // 双重保险：虽然UI上做了限制，但点击时再查一次更安全
        if (!Settings.canDrawOverlays(this)) return

        val intent = Intent(this, InterferenceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // 最小化 Activity，给桌面让路
        moveTaskToBack(true)
    }
}

// 简单的枚举用于页面切换
enum class Screen {
    Guide, Home
}
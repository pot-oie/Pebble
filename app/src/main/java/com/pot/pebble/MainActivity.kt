package com.pot.pebble

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pot.pebble.monitor.UsageCollector
import com.pot.pebble.service.InterferenceService
import com.pot.pebble.service.ServiceState
import com.pot.pebble.ui.screen.BlacklistScreen
import com.pot.pebble.ui.screen.FocusScreen
import com.pot.pebble.ui.screen.GuideScreen
import com.pot.pebble.ui.theme.MossGreen
import com.pot.pebble.ui.theme.PebbleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PebbleTheme {
                val isServiceRunning by ServiceState.isRunning.collectAsState()
                val usageCollector = remember { UsageCollector(this) }

                // 导航状态：如果服务正在运行，默认进入 Focus 页
                var currentScreen by remember {
                    mutableStateOf(
                        if (isServiceRunning) Screen.Focus
                        else if (checkAllPermissions(this, usageCollector)) Screen.Home
                        else Screen.Guide
                    )
                }

                // 监听服务状态，如果服务意外停止（比如在通知栏关掉），自动切回主页
                LaunchedEffect(isServiceRunning) {
                    if (!isServiceRunning && currentScreen == Screen.Focus) {
                        currentScreen = Screen.Home
                    }
                }

                when (currentScreen) {
                    Screen.Guide -> {
                        GuideScreen(onAllGranted = { currentScreen = Screen.Home })
                    }
                    Screen.Home -> {
                        Scaffold(
                            floatingActionButton = {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        if (isServiceRunning) {
                                            // 逻辑上 Home 页只有“启动”按钮，
                                            // 如果已经在运行，应该显示“回到专注页”或者直接停掉
                                            // 这里做“启动”动作
                                            currentScreen = Screen.Focus
                                        } else {
                                            startPebbleService()
                                            // 启动后跳转专注页
                                            currentScreen = Screen.Focus
                                        }
                                    },
                                    containerColor = MossGreen,
                                    contentColor = Color.White
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("启动专注")
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                BlacklistScreen(
                                    onNavigateToSettings = { currentScreen = Screen.Guide }
                                )
                            }
                        }
                    }
                    Screen.Focus -> {
                        // 专注页
                        FocusScreen(
                            onStopFocus = {
                                stopPebbleService()
                                currentScreen = Screen.Home
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val usageCollector = UsageCollector(this)
        // 日志检查权限状态
        val hasAll = checkAllPermissions(this, usageCollector)

        if (ServiceState.isRunning.value && !hasAll) {
            stopPebbleService()
        }
    }

    private fun checkAllPermissions(context: Context, usageCollector: UsageCollector): Boolean {
        if (!Settings.canDrawOverlays(context)) {
            return false
        }
        if (!usageCollector.hasPermission()) {
            return false
        }
        return true
    }

    private fun startPebbleService() {
        if (!Settings.canDrawOverlays(this)) return
        val intent = Intent(this, InterferenceService::class.java)
        startForegroundService(intent)
    }

    private fun stopPebbleService() {
        val intent = Intent(this, InterferenceService::class.java)
        stopService(intent)
    }
}

enum class Screen {
    Guide, Home, Focus
}
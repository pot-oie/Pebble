package com.pot.pebble

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pot.pebble.data.ThemeStore
import com.pot.pebble.monitor.UsageCollector
import com.pot.pebble.service.InterferenceService
import com.pot.pebble.service.ServiceState
import com.pot.pebble.ui.screen.BlacklistScreen
import com.pot.pebble.ui.screen.FocusScreen
import com.pot.pebble.ui.screen.GuideScreen
import com.pot.pebble.ui.screen.SettingsScreen
import com.pot.pebble.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 初始化主题存储
        ThemeStore.init(this)

        setContent {
            val S = LanguageManager.s
            PebbleTheme {
                val isServiceRunning by ServiceState.isRunning.collectAsState()
                val usageCollector = remember { UsageCollector(this) }

                var currentScreen by remember {
                    mutableStateOf(
                        if (isServiceRunning) Screen.Focus
                        else if (checkAllPermissions(this, usageCollector)) Screen.Home
                        else Screen.Guide
                    )
                }

                // ... (LaunchedEffect 保持不变) ...
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
                            containerColor = CozyPaperWhite,
                            floatingActionButton = {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        if (!isServiceRunning) startPebbleService()
                                        currentScreen = Screen.Focus
                                    },
                                    containerColor = CozyCharcoal,
                                    contentColor = PureWhite,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(S.btnStartFocus, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                BlacklistScreen(
                                    // 点击设置按钮跳转到 SettingsScreen
                                    onNavigateToSettings = { currentScreen = Screen.Settings }
                                )
                            }
                        }
                    }
                    Screen.Settings -> {
                        SettingsScreen(
                            onBack = { currentScreen = Screen.Home },
                            onNavigateToGuide = { currentScreen = Screen.Guide }
                        )
                    }
                    Screen.Focus -> {
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
        if (ServiceState.isRunning.value && !checkAllPermissions(this, usageCollector)) {
            stopPebbleService()
        }
    }

    private fun checkAllPermissions(context: Context, usageCollector: UsageCollector): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        if (!usageCollector.hasPermission()) return false
        return true
    }

    private fun startPebbleService() {
        if (!Settings.canDrawOverlays(this)) return
        val intent = Intent(this, InterferenceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopPebbleService() {
        val intent = Intent(this, InterferenceService::class.java)
        stopService(intent)
    }
}

// Settings 枚举
enum class Screen {
    Guide, Home, Focus, Settings
}
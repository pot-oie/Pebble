package com.pot.pebble

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.pot.pebble.ui.screen.GuideScreen
import com.pot.pebble.ui.theme.MossGreen
import com.pot.pebble.ui.theme.PebbleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PebbleDebug", "MainActivity: onCreate")

        setContent {
            PebbleTheme {
                val isServiceRunning by ServiceState.isRunning.collectAsState()
                val usageCollector = remember { UsageCollector(this) }

                var currentScreen by remember {
                    mutableStateOf(
                        if (checkAllPermissions(this, usageCollector)) Screen.Home else Screen.Guide
                    )
                }

                when (currentScreen) {
                    Screen.Guide -> {
                        GuideScreen(
                            onAllGranted = {
                                Log.d("PebbleDebug", "Guide: All Permissions Granted, moving to Home")
                                currentScreen = Screen.Home
                            }
                        )
                    }
                    Screen.Home -> {
                        Scaffold(
                            floatingActionButton = {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        if (isServiceRunning) {
                                            stopPebbleService()
                                        } else {
                                            startPebbleService()
                                        }
                                    },
                                    containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else MossGreen,
                                    contentColor = Color.White
                                ) {
                                    Icon(
                                        imageVector = if (isServiceRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isServiceRunning) "停止专注" else "启动专注")
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
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val usageCollector = UsageCollector(this)
        // 日志检查权限状态
        val hasAll = checkAllPermissions(this, usageCollector)
        Log.d("PebbleDebug", "MainActivity: onResume (ServiceRunning=${ServiceState.isRunning.value}, Perms=$hasAll)")

        if (ServiceState.isRunning.value && !hasAll) {
            Log.w("PebbleDebug", "Permissions lost while service running! Stopping...")
            stopPebbleService()
        }
    }

    private fun checkAllPermissions(context: Context, usageCollector: UsageCollector): Boolean {
        if (!Settings.canDrawOverlays(context)) {
            Log.d("PebbleDebug", "❌ Missing Overlay Permission")
            return false
        }
        if (!usageCollector.hasPermission()) {
            Log.d("PebbleDebug", "❌ Missing UsageStats Permission")
            return false
        }
        return true
    }

    private fun startPebbleService() {
        Log.i("PebbleDebug", "Attempting to START service...")
        if (!Settings.canDrawOverlays(this)) {
            Log.e("PebbleDebug", "START failed: No overlay permission")
            return
        }

        val intent = Intent(this, InterferenceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        moveTaskToBack(true)
    }

    private fun stopPebbleService() {
        Log.i("PebbleDebug", "Attempting to STOP service...")
        val intent = Intent(this, InterferenceService::class.java)
        stopService(intent)
    }
}

enum class Screen {
    Guide, Home
}
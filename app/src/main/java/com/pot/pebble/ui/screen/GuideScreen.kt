package com.pot.pebble.ui.screen

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pot.pebble.monitor.UsageCollector
import com.pot.pebble.ui.theme.MossGreen
import com.pot.pebble.ui.theme.NatureBeige

@Composable
fun GuideScreen(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasUsagePermission by remember { mutableStateOf(false) }
    var isIgnoringBatteryOpt by remember { mutableStateOf(false) }

    val usageCollector = remember { UsageCollector(context) }

    fun checkPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasUsagePermission = usageCollector.hasPermission()

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isIgnoringBatteryOpt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = NatureBeige,
        bottomBar = {
            Surface(
                color = NatureBeige,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onAllGranted,
                    enabled = hasOverlayPermission && hasUsagePermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MossGreen,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("开始使用", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "权限设置",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pebble 需要以下权限以正常运行检测与干扰功能。",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. 必要权限
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("必要权限", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MossGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionItem(
                        title = "悬浮窗权限",
                        desc = "用于显示干扰元素",
                        isGranted = hasOverlayPermission,
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(color = NatureBeige, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    PermissionItem(
                        title = "使用情况访问",
                        desc = "用于识别当前前台应用",
                        isGranted = hasUsagePermission,
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                            Toast.makeText(context, "请找到 Pebble 并允许访问", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            // 2. 防杀设置
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("后台保活 (推荐)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MossGreen)
                    Text("防止服务被系统暂停导致检测失效", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionItem(
                        title = "忽略电池优化",
                        desc = "允许后台持续运行",
                        isGranted = isIgnoringBatteryOpt,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                try { context.startActivity(intent) } catch (e: Exception) { }
                            }
                        }
                    )
                    HorizontalDivider(color = NatureBeige, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                    SettingsItem(
                        title = "自启动管理",
                        desc = "请手动开启自启动权限",
                        onClick = { openAutoStartSettings(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun PermissionItem(title: String, desc: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isGranted) onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(desc, fontSize = 12.sp, color = Color.Gray)
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isGranted) MossGreen else Color.Gray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, desc: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(desc, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

fun openAutoStartSettings(context: Context) {
    val intents = listOf(
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent().setComponent(ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
    )

    var success = false
    for (intent in intents) {
        try {
            context.startActivity(intent)
            success = true
            break
        } catch (e: Exception) { continue }
    }
    if (!success) {
        Toast.makeText(context, "无法跳转，请手动在设置中查找", Toast.LENGTH_LONG).show()
    }
}
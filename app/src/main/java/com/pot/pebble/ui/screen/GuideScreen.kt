package com.pot.pebble.ui.screen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pot.pebble.monitor.AppUsageMonitor
import com.pot.pebble.ui.theme.MossGreen
import com.pot.pebble.ui.theme.NatureBeige

@Composable
fun GuideScreen(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val monitor = remember { AppUsageMonitor(context) }

    // 状态追踪
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasUsagePermission by remember { mutableStateOf(false) }
    var isIgnoringBatteryOpt by remember { mutableStateOf(false) }

    // 检查权限的函数
    fun checkPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasUsagePermission = monitor.hasPermission()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isIgnoringBatteryOpt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    // 监听生命周期：当用户从设置页返回 APP 时，自动刷新状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NatureBeige)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "开始之前的准备",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "为了让 Pebble 稳定运行，我们需要一些特殊的权限。",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1. 核心权限卡片
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("必要权限", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MossGreen)
                Spacer(modifier = Modifier.height(12.dp))

                PermissionItem(
                    title = "显示悬浮窗",
                    desc = "用于显示落石",
                    isGranted = hasOverlayPermission,
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
                Divider(color = NatureBeige, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                PermissionItem(
                    title = "访问使用记录",
                    desc = "用于识别当前应用",
                    isGranted = hasUsagePermission,
                    onClick = { monitor.requestPermission() }
                )
            }
        }

        // 2. 稳定性设置卡片 (针对小米等系统)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("防杀设置 (推荐)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MossGreen)
                Text("防止服务被系统误杀", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                PermissionItem(
                    title = "电池优化 (无限制)",
                    desc = "允许后台运行",
                    isGranted = isIgnoringBatteryOpt,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            try {
                                context.startActivity(intent)
                                Toast.makeText(context, "请找到 Pebble 并选择【无限制】", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开电池优化设置", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                Divider(color = NatureBeige, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // 自启动没有标准 API，只能尽量跳转或者引导去应用详情页
                SettingsItem(
                    title = "自启动权限",
                    desc = "小米/OV必须开启",
                    onClick = {
                        openAutoStartSettings(context)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3. 进入按钮
        Button(
            onClick = onAllGranted,
            enabled = hasOverlayPermission && hasUsagePermission, // 只有核心权限有了才能进
            colors = ButtonDefaults.buttonColors(
                containerColor = MossGreen,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("进入花园", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

// 🔧 辅助方法：尝试打开各大厂商的自启动页面
fun openAutoStartSettings(context: Context) {
    val intents = listOf(
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent().setComponent(ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        // 最后的保底：应用详情页
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
    )

    var success = false
    for (intent in intents) {
        try {
            context.startActivity(intent)
            success = true
            break
        } catch (e: Exception) {
            continue
        }
    }
    if (!success) {
        Toast.makeText(context, "无法自动跳转，请手动在设置中开启自启动", Toast.LENGTH_LONG).show()
    } else {
        Toast.makeText(context, "请找到 Pebble 并开启【自启动】", Toast.LENGTH_LONG).show()
    }
}
package com.pot.pebble.ui.screen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pot.pebble.monitor.UsageCollector
import com.pot.pebble.ui.theme.*

@Composable
fun GuideScreen(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    // 语言管理器
    val S = LanguageManager.s

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
        containerColor = CozyPaperWhite, // 暖背景
        bottomBar = {
            Surface(
                color = CozyPaperWhite,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color(0xFFF0F0F0)) // 加个极淡的顶部分割线
            ) {
                Button(
                    onClick = onAllGranted,
                    enabled = hasOverlayPermission && hasUsagePermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CozyCharcoal,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(S.getStarted, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
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
                text = S.guideTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, // 衬线体标题
                color = CozyCharcoal
            )
            Text(
                text = S.guideDesc,
                fontSize = 16.sp,
                color = CozyGrey,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 语言切换卡片
            CozySectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { LanguageManager.toggle() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(S.settingsLanguage, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CozyCharcoal)
                    }
                    // 简单的切换按钮外观
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (LanguageManager.currentLang == Lang.CN) CozyGreen else CozyBorder,
                        modifier = Modifier.width(60.dp).height(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (LanguageManager.currentLang == Lang.CN) "CN" else "EN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (LanguageManager.currentLang == Lang.CN) PureWhite else CozyCharcoal
                            )
                        }
                    }
                }
            }

            // 必要权限
            CozySectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.permRequired, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CozyRed)
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionItem(
                        title = S.permOverlay,
                        desc = S.permOverlayDesc,
                        isGranted = hasOverlayPermission,
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                            Toast.makeText(context, S.toastOverlay, Toast.LENGTH_LONG).show()
                        }
                    )
                    HorizontalDivider(color = CozyBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    PermissionItem(
                        title = S.permUsage,
                        desc = S.permUsageDesc,
                        isGranted = hasUsagePermission,
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                            Toast.makeText(context, S.toastUsage, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            // 防杀设置
            CozySectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.permRecommended, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CozyGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionItem(
                        title = S.permBattery,
                        desc = S.permBatteryDesc,
                        isGranted = isIgnoringBatteryOpt,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Toast.makeText(context, S.toastBattery, Toast.LENGTH_LONG).show()
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                try { context.startActivity(intent) } catch (e: Exception) { }
                            }
                        }
                    )
                    HorizontalDivider(color = CozyBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    SettingsItem(
                        title = S.permAutoStart,
                        desc = S.permAutoStartDesc,
                        onClick = {
                            Toast.makeText(context, S.toastAutoStart, Toast.LENGTH_LONG).show()
                            openAutoStartSettings(context)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// 封装 Cozy 风格卡片容器
@Composable
fun CozySectionCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, CozyBorder),
        colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
        content()
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
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CozyCharcoal)
            Text(desc, fontSize = 12.sp, color = CozyGrey)
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isGranted) CozyGreen else Color(0xFFF0F0F0)), // 绿色或浅灰
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
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
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CozyCharcoal)
            Text(desc, fontSize = 12.sp, color = CozyGrey)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = CozyBorder)
    }
}

// 跳转各大厂商自启动设置
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
        Toast.makeText(context, LanguageManager.s.toastAutoStartFail, Toast.LENGTH_LONG).show()    }
}
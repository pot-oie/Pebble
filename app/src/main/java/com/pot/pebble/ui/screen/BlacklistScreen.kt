package com.pot.pebble.ui.screen

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pot.pebble.data.entity.AppConfig
import com.pot.pebble.service.InterferenceService // ✅ 修复 Service 引用报错
import com.pot.pebble.ui.theme.MossGreen
import com.pot.pebble.ui.theme.NatureBeige
import com.pot.pebble.ui.viewmodel.BlacklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    viewModel: BlacklistViewModel = viewModel(),
    onNavigateToSettings: () -> Unit
) {
    val apps by viewModel.appList.collectAsState()
    val context = LocalContext.current // 获取 Context 用于发送 Intent

    // 数据分组
    val blacklistedApps = remember(apps) { apps.filter { it.isBlacklisted } }
    val otherApps = remember(apps) { apps.filter { !it.isBlacklisted } }

    // 展开状态控制
    var isOtherExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("专注花园", fontWeight = FontWeight.Bold)
                        Text("已种下 ${blacklistedApps.size} 块顽石", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NatureBeige),
                actions = {
                    // 清空按钮
                    IconButton(onClick = {
                        val intent = Intent(context, InterferenceService::class.java).apply {
                            action = InterferenceService.ACTION_CLEAR_ROCKS // ✅ 修复 action 引用
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "清空", tint = MossGreen)
                    }

                    // 设置按钮
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = MossGreen)
                    }
                }
            )
        },
        containerColor = NatureBeige
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            // 第一部分：已设禁区
            item {
                Text(
                    text = "🚧 禁区 (生效中)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MossGreen,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            if (blacklistedApps.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Text(
                            "还没有添加应用，点击下方添加 ↓",
                            modifier = Modifier.padding(24.dp),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(blacklistedApps, key = { it.packageName }) { app ->
                    AppItemCard(app = app, isBlacklistedSection = true) { isChecked ->
                        viewModel.toggleBlacklist(app, isChecked)
                    }
                }
            }

            // 第二部分：其他应用 (折叠区)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isOtherExpanded = !isOtherExpanded }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌱 添加干扰源 (${otherApps.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isOtherExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            if (isOtherExpanded) {
                items(otherApps, key = { it.packageName }) { app ->
                    AppItemCard(app = app, isBlacklistedSection = false) { isChecked ->
                        viewModel.toggleBlacklist(app, isChecked)
                    }
                }
            }

            // 底部留白
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AppItemCard(
    app: AppConfig,
    isBlacklistedSection: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val icon = remember(app.packageName) {
        try { packageManager.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isBlacklistedSection) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(modifier = Modifier.size(42.dp).background(Color.LightGray, RoundedCornerShape(10.dp)))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isBlacklistedSection) {
                    Text("拦截中", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Switch(
                checked = app.isBlacklisted,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MossGreen,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = NatureBeige
                )
            )
        }
    }
}
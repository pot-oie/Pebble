package com.pot.pebble.ui.screen

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.pot.pebble.ui.theme.MossGreen
import com.pot.pebble.ui.theme.NatureBeige
import com.pot.pebble.ui.viewmodel.BlacklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    viewModel: BlacklistViewModel = viewModel(),
    onNavigateToSettings: () -> Unit // 跳转回调
) {
    // 观察数据库数据
    val apps by viewModel.appList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "专注花园",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NatureBeige
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MossGreen
                        )
                    }
                }
            )
        },
        containerColor = NatureBeige // 全局米色背景
    ) { innerPadding ->

        if (apps.isEmpty()) {
            // 加载中或空状态
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MossGreen)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item {
                    Text(
                        text = "选择你的干扰源",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }

                items(apps, key = { it.packageName }) { app ->
                    AppItemCard(
                        app = app,
                        onToggle = { isChecked ->
                            viewModel.toggleBlacklist(app, isChecked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppItemCard(app: AppConfig, onToggle: (Boolean) -> Unit) {
    // 获取包管理器来加载图标
    val context = LocalContext.current
    val packageManager = context.packageManager

    // 简单的图标加载 (生产环境可以用 Coil 配合 ImageLoader)
    val icon = remember(app.packageName) {
        try {
            packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null // 默认图标或处理异常
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp), // 圆润的卡片
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // 轻微阴影
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 图标
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(modifier = Modifier.size(48.dp).background(Color.Gray))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. 文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (app.isBlacklisted) "已列入禁区" else "安全",
                    fontSize = 12.sp,
                    color = if (app.isBlacklisted) MaterialTheme.colorScheme.secondary else Color.Gray
                )
            }

            // 3. 开关
            Switch(
                checked = app.isBlacklisted,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MossGreen, // 激活时是苔藓绿
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = NatureBeige
                )
            )
        }
    }
}
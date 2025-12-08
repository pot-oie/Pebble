package com.pot.pebble.ui.screen

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pot.pebble.data.entity.AppConfig
import com.pot.pebble.service.InterferenceService
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalCount by viewModel.totalBlacklistedCount.collectAsState()

    val context = LocalContext.current
    // 🔥 1. 获取焦点管理器
    val focusManager = LocalFocusManager.current

    val blacklistedApps = remember(apps) { apps.filter { it.isBlacklisted } }
    val otherApps = remember(apps) { apps.filter { !it.isBlacklisted } }

    var isOtherExpanded by remember { mutableStateOf(false) }

    val isSearching = searchQuery.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pebble 列表", fontWeight = FontWeight.Bold)
                        Text("$totalCount 个应用受控", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NatureBeige),
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, InterferenceService::class.java).apply {
                            action = InterferenceService.ACTION_CLEAR_ROCKS
                        }
                        context.startService(intent)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "清空", tint = MossGreen)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = MossGreen)
                    }
                }
            )
        },
        containerColor = NatureBeige
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // 🔍 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索应用...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                // 🔥 2. 修改右侧图标逻辑：点击清除时，同时移除焦点
                trailingIcon = if (isSearching) {
                    {
                        IconButton(onClick = {
                            viewModel.onSearchQueryChanged("")
                            focusManager.clearFocus() // 移除焦点，收起键盘，停止闪烁
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "清除", tint = Color.Gray)
                        }
                    }
                } else null,
                // 🔥 3. 增加键盘动作：点击键盘上的“搜索”或“完成”也移除焦点
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MossGreen,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (isSearching) {
                    item {
                        Text(
                            text = "搜索结果",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (apps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("没有找到相关应用", color = Color.Gray)
                            }
                        }
                    } else {
                        items(apps, key = { it.packageName }) { app ->
                            AppItemCard(app = app, isStatusTextVisible = true) { isChecked ->
                                viewModel.toggleBlacklist(app, isChecked)
                            }
                        }
                    }
                } else {
                    if (blacklistedApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "已启用",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MossGreen
                            )
                        }
                        items(blacklistedApps, key = { it.packageName }) { app ->
                            AppItemCard(app = app, isStatusTextVisible = false) { isChecked ->
                                viewModel.toggleBlacklist(app, isChecked)
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isOtherExpanded = !isOtherExpanded }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "未启用 (${otherApps.size})",
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
                            AppItemCard(app = app, isStatusTextVisible = false) { isChecked ->
                                viewModel.toggleBlacklist(app, isChecked)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun AppItemCard(
    app: AppConfig,
    isStatusTextVisible: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val icon = remember(app.packageName) {
        try { packageManager.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.5f)),
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
                if (isStatusTextVisible && app.isBlacklisted) {
                    Text("受控", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
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
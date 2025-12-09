package com.pot.pebble.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pot.pebble.data.entity.AppConfig
import com.pot.pebble.ui.theme.*
import com.pot.pebble.ui.viewmodel.BlacklistViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    viewModel: BlacklistViewModel = viewModel(),
    onNavigateToSettings: () -> Unit
) {
    val apps by viewModel.appList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalCount by viewModel.totalBlacklistedCount.collectAsState()

    // 订阅统计数据
    val todayStats by viewModel.todayStats.collectAsState() // Pair(Count, DurationMillis)
    val weekTrend by viewModel.weekTrend.collectAsState()   // List<Int>

    val focusManager = LocalFocusManager.current
    val S = LanguageManager.s

    val blacklistedApps = remember(apps) { apps.filter { it.isBlacklisted } }
    val otherApps = remember(apps) { apps.filter { !it.isBlacklisted } }
    var isOtherExpanded by remember { mutableStateOf(false) }
    val isSearching = searchQuery.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(S.listTitle, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = CozyCharcoal)
                        Text("$totalCount ${S.activeCountSuffix}", fontSize = 12.sp, color = CozyGrey, fontFamily = FontFamily.SansSerif)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CozyPaperWhite),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = CozyCharcoal)
                    }
                }
            )
        },
        containerColor = CozyPaperWhite
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(S.searchHint, color = CozyGrey) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CozyGrey) },
                trailingIcon = if (isSearching) { { IconButton(onClick = { viewModel.onSearchQueryChanged(""); focusManager.clearFocus() }) { Icon(Icons.Default.Close, contentDescription = null, tint = CozyGrey) } } } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CozyCharcoal, unfocusedBorderColor = CozyBorder,
                    focusedContainerColor = PureWhite, unfocusedContainerColor = PureWhite,
                    cursorColor = CozyCharcoal
                ),
                singleLine = true
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 如果不在搜索状态，显示统计卡片
                if (!isSearching) {
                    item {
                        StatisticsCard(
                            todayCount = todayStats.first,
                            todayDurationMs = todayStats.second,
                            weekTrend = weekTrend,
                            S = S
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (isSearching) {
                    if (apps.isEmpty()) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text(S.noAppsFound, color = CozyGrey) } }
                    } else {
                        items(apps, key = { it.packageName }) { app -> CozyAppCard(app = app) { viewModel.toggleBlacklist(app, it) } }
                    }
                } else {
                    if (blacklistedApps.isNotEmpty()) {
                        item { HeaderText(S.groupActive) }
                        items(blacklistedApps, key = { it.packageName }) { app -> CozyAppCard(app = app) { viewModel.toggleBlacklist(app, it) } }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { isOtherExpanded = !isOtherExpanded }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${S.groupIdle} (${otherApps.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CozyGrey, modifier = Modifier.weight(1f))
                            Icon(if (isOtherExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = CozyGrey)
                        }
                    }
                    if (isOtherExpanded) {
                        items(otherApps, key = { it.packageName }) { app -> CozyAppCard(app = app) { viewModel.toggleBlacklist(app, it) } }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// 统计卡片组件
@Composable
fun StatisticsCard(
    todayCount: Int,
    todayDurationMs: Long,
    weekTrend: List<Int>,
    S: Strings
) {
    val durationMins = TimeUnit.MILLISECONDS.toMinutes(todayDurationMs)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CozyBorder),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题
            Text(S.statTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CozyGrey)

            Spacer(modifier = Modifier.height(16.dp))

            // 核心数据行
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(todayCount.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CozyCharcoal)
                    Text(S.statBlockCount, fontSize = 12.sp, color = CozyGrey)
                }

                // 竖线分隔
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(CozyBorder))

                Column {
                    Text(durationMins.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CozyGreen)
                    Text(S.statSavedTime, fontSize = 12.sp, color = CozyGrey)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 趋势图标题
            Text(S.statWeekTrend, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CozyGrey.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))

            // 迷你柱状图 Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                if (weekTrend.all { it == 0 }) {
                    // 空状态
                    Text(S.emptyData, fontSize = 12.sp, color = CozyBorder, modifier = Modifier.align(Alignment.Center))
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width / (weekTrend.size * 2f - 1f) // 间隔和柱子一样宽
                        val maxVal = weekTrend.maxOrNull() ?: 1
                        val safeMax = if (maxVal == 0) 1 else maxVal

                        weekTrend.forEachIndexed { index, value ->
                            val x = index * barWidth * 2
                            val barHeight = (value.toFloat() / safeMax) * size.height
                            val safeHeight = if (value > 0) barHeight.coerceAtLeast(10f) else 4f

                            val color = if (index == 6) CozyCharcoal else CozyBorder // 今天的柱子深色

                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, size.height - safeHeight),
                                size = Size(barWidth, safeHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = CozyCharcoal,
        fontFamily = FontFamily.Serif,
        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
    )
}

@Composable
fun CozyAppCard(
    app: AppConfig,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val icon = remember(app.packageName) {
        try { packageManager.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
    }

    val targetBorderColor = if (app.isBlacklisted) CozyRed else CozyBorder
    val targetBackgroundColor = if (app.isBlacklisted) CozyRedBg else PureWhite

    val borderColor by animateColorAsState(targetBorderColor, tween(300), label = "border")
    val containerColor by animateColorAsState(targetBackgroundColor, tween(300), label = "bg")

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyCharcoal
                )
                if (app.isBlacklisted) {
                    Surface(
                        color = Color.White.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = LanguageManager.s.tagBlocked,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Switch(
                checked = app.isBlacklisted,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhite,
                    checkedTrackColor = CozyRed,
                    uncheckedThumbColor = CozyGrey,
                    uncheckedTrackColor = CozyBorder,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}
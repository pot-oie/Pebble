package com.pot.pebble.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pot.pebble.core.model.EntityType
import com.pot.pebble.data.ThemeStore
import com.pot.pebble.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToGuide: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by ThemeStore.currentTheme.collectAsState()
    val customImageUri by ThemeStore.customImageUri.collectAsState()
    // 订阅历史记录
    val historyList by ThemeStore.customHistory.collectAsState()
    val S = LanguageManager.s

    // 弹窗状态
    var showDanmakuDialog by remember { mutableStateOf(false) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // 尝试获取持久权限（如果是相册URI）
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { e.printStackTrace() }

            // 保存到本地并设为当前
            ThemeStore.addCustomImage(context, uri)
            ThemeStore.setTheme(context, EntityType.CUSTOM)
        }
    }

    if (showDanmakuDialog) {
        DanmakuEditDialog(onDismiss = { showDanmakuDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(S.settingsTitle, color = CozyCharcoal, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CozyCharcoal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CozyPaperWhite)
            )
        },
        containerColor = CozyPaperWhite
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- 干扰模式标题 ---
            item {
                Text(
                    text = S.themeTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyGrey,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // --- 模式列表 ---
            val themes = listOf(
                EntityType.CIRCLE to S.themeRock,
                EntityType.BOX to S.themeTetris,
                EntityType.TEXT to S.themeDanmaku,
                EntityType.CRACK to S.themeCrack,
                EntityType.CUSTOM to S.themeCustom
            )

            items(themes) { (type, title) ->
                ThemeOptionCard(
                    title = title,
                    isSelected = currentTheme == type,
                    onClick = { ThemeStore.setTheme(context, type) },

                    // 弹幕模式：显示编辑按钮
                    showEditIcon = (type == EntityType.TEXT),
                    onEditClick = { showDanmakuDialog = true },

                    // 自定义模式：显示图片选择和历史记录
                    isCustomImageMode = (type == EntityType.CUSTOM),
                    customImageUri = if (type == EntityType.CUSTOM) customImageUri else null,
                    onPickImage = {
                        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    historyList = if (type == EntityType.CUSTOM) historyList else emptyList(),
                    onHistorySelect = { uri -> ThemeStore.selectCustomImage(context, uri) },
                    onHistoryDelete = { uri -> ThemeStore.deleteCustomImage(context, uri) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = CozyBorder) }

            // --- 系统设置 ---
            item {
                Text(
                    text = S.settingsSystemTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyGrey,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CozyBorder),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToGuide)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = CozyCharcoal)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = S.settingsPermissionTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CozyCharcoal
                            )
                            Text(
                                text = S.settingsPermissionDesc,
                                fontSize = 12.sp,
                                color = CozyGrey
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeOptionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showEditIcon: Boolean = false,
    onEditClick: () -> Unit = {},

    // 自定义图片相关参数
    isCustomImageMode: Boolean = false,
    customImageUri: String? = null,
    onPickImage: () -> Unit = {},
    historyList: List<String> = emptyList(),
    onHistorySelect: (String) -> Unit = {},
    onHistoryDelete: (String) -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PureWhite else PureWhite.copy(alpha = 0.5f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, CozyGreen) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = if (isSelected) CozyCharcoal else CozyGrey,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )

                if (isSelected && showEditIcon) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CozyCharcoal, modifier = Modifier.size(18.dp))
                    }
                }

                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = CozyGreen)
                }
            }

            // 自定义模式扩展区域
            if (isCustomImageMode && isSelected) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. 历史记录横向列表
                if (historyList.isNotEmpty()) {
                    Text("History", fontSize = 12.sp, color = CozyGrey, modifier = Modifier.padding(bottom = 8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { uri ->
                            Box(
                                modifier = Modifier.size(56.dp) // 容器大小
                            ) {
                                // 图片缩略图
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp) // 图片略小，留出删除按钮位置
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (uri == customImageUri) 2.dp else 1.dp,
                                            color = if (uri == customImageUri) CozyGreen else CozyBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onHistorySelect(uri) }
                                        .align(Alignment.BottomStart)
                                )

                                // 删除按钮 (右上角)
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .background(CozyCharcoal, CircleShape)
                                        .clickable { onHistoryDelete(uri) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = PureWhite, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 2. 选择新图片按钮
                Button(
                    onClick = onPickImage,
                    colors = ButtonDefaults.buttonColors(containerColor = CozyCharcoal, contentColor = PureWhite),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    // 使用 Star 代替 Image，保证编译通过
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(LanguageManager.s.btnSelectImage, fontSize = 12.sp)
                }
            }
        }
    }
}

// 弹幕编辑弹窗
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DanmakuEditDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val danmakuList by ThemeStore.danmakuList.collectAsState()
    var newText by remember { mutableStateOf("") }
    val S = LanguageManager.s

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        titleContentColor = CozyCharcoal,
        textContentColor = CozyCharcoal,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 使用 Email 代替 ChatBubbleOutline，保证编译通过
                Icon(Icons.Default.Email, contentDescription = null, tint = CozyGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(S.dialogDanmakuTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    placeholder = { Text(S.dialogDanmakuHint, color = CozyGrey) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CozyCharcoal,
                        unfocusedBorderColor = CozyBorder,
                        focusedTextColor = CozyCharcoal,
                        cursorColor = CozyCharcoal
                    ),
                    trailingIcon = {
                        FilledIconButton(
                            onClick = {
                                if (newText.isNotBlank()) {
                                    val newList = danmakuList + newText
                                    ThemeStore.saveDanmakuList(context, newList)
                                    newText = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = CozyCharcoal),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = PureWhite, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                HorizontalDivider(color = CozyBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(danmakuList) { item ->
                        Surface(
                            color = CozyPaperWhite,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CozyBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 14.sp,
                                    color = CozyCharcoal,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = CozyGrey,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            val newList = danmakuList - item
                                            ThemeStore.saveDanmakuList(context, newList)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CozyCharcoal),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(S.dialogBtnDone, fontWeight = FontWeight.Bold)
            }
        }
    )
}
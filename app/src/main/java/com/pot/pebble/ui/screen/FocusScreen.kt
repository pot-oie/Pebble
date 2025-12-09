package com.pot.pebble.ui.screen

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pot.pebble.service.InterferenceService
import com.pot.pebble.service.ServiceState
import com.pot.pebble.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onStopFocus: () -> Unit
) {
    val startTime by ServiceState.startTime.collectAsState()
    val triggerCount by ServiceState.triggerCount.collectAsState()
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    val context = LocalContext.current
    val S = LanguageManager.s

    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val durationMillis = if (startTime > 0) currentTime - startTime else 0L
    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
    val timerText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Scaffold(
        containerColor = CozyPaperWhite,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    // 右上角：清空按钮 (Surface 胶囊样式)
                    Surface(
                        onClick = {
                            val intent = Intent(context, InterferenceService::class.java).apply {
                                action = InterferenceService.ACTION_CLEAR_ROCKS
                            }
                            context.startService(intent)
                        },
                        shape = RoundedCornerShape(50),
                        color = PureWhite,
                        border = BorderStroke(1.dp, CozyBorder),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = CozyCharcoal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CozyCharcoal)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 核心仪表盘
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CozyBorder),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(0.75f) // 稍微拉长一点比例
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    // 1. 标题
                    Text(
                        text = S.focusTimerTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CozyGrey,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.SansSerif
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 呼吸计时器
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .scale(pulseScale)
                                .background(CozyGreen.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, CozyGreen.copy(alpha = pulseAlpha), CircleShape)
                        )

                        Text(
                            text = timerText,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = CozyCharcoal,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 分割线
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        thickness = 1.dp,
                        color = CozyBorder.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 落石统计区
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$triggerCount",
                            fontSize = 40.sp, // 大数字
                            fontWeight = FontWeight.Bold,
                            color = if (triggerCount > 0) CozyRed else CozyCharcoal, // 触发变红
                            fontFamily = FontFamily.Monospace // 等宽数字
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = S.statStonesDropped, // "落石触发"
                            fontSize = 13.sp,
                            color = CozyGrey,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部停止按钮
            Button(
                onClick = onStopFocus,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CozyCharcoal,
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 32.dp)
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(
                    text = S.btnStopFocus,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
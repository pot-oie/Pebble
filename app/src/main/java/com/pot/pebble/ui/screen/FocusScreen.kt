package com.pot.pebble.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pot.pebble.service.ServiceState
import com.pot.pebble.ui.theme.MossGreen
import com.pot.pebble.ui.theme.NatureBeige
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun FocusScreen(
    onStopFocus: () -> Unit
) {
    // 订阅数据
    val startTime by ServiceState.startTime.collectAsState()
    val triggerCount by ServiceState.triggerCount.collectAsState()

    // 本地计时器状态
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // 每秒刷新界面时间
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    // 计算时长
    val durationMillis = if (startTime > 0) currentTime - startTime else 0L
    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

    val timerText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NatureBeige),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // 顶部占位
        Spacer(modifier = Modifier.weight(1f))

        // ⏱️ 计时器大圆环
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "专注时长",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = timerText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MossGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 📊 数据统计
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            StatItem(label = "落石触发", value = "$triggerCount 次")
        }

        Spacer(modifier = Modifier.weight(1f))

        // 🛑 停止按钮
        Button(
            onClick = onStopFocus,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("停止专注", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MossGreen)
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
    }
}
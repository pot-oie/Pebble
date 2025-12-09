package com.pot.pebble.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 定义浅色模式颜色映射
private val CozyColorScheme = lightColorScheme(
    primary = CozyCharcoal,       // 主色调：炭黑
    onPrimary = PureWhite,
    background = CozyPaperWhite,  // 背景：羊皮纸
    surface = PureWhite,          // 卡片表面：纯白
    onSurface = CozyCharcoal,     // 卡片文字：炭黑
    secondary = CozyGreen,        // 次要色：苔藓绿
    error = CozyRed,              // 错误/警告：陶土红
    outline = CozyBorder          // 边框色
)

@Composable
fun PebbleTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = CozyColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 状态栏也设为暖白色，保持一体感
            window.statusBarColor = CozyPaperWhite.toArgb()
            // 状态栏图标设为深色
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
package com.pot.pebble.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NatureColorScheme = lightColorScheme(
    primary = MossGreen,
    onPrimary = NatureSurface,
    background = NatureBeige,
    surface = NatureSurface,
    onSurface = EarthBrown,
    secondary = AlertOrange
)

@Composable
fun PebbleTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = NatureColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏颜色为米色，图标为深色
            window.statusBarColor = NatureBeige.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
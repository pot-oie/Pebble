package com.pot.pebble

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pot.pebble.monitor.AppUsageMonitor
import com.pot.pebble.service.InterferenceService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btn_start)
        val monitor = AppUsageMonitor(this)

        btnStart.setOnClickListener {
            // 1. 检查悬浮窗权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return@setOnClickListener
            }

            // 2. 检查使用情况权限 (新增)
            if (!monitor.hasPermission()) {
                Toast.makeText(this, "请在列表中找到 Pebble 并授予【访问使用记录】权限", Toast.LENGTH_LONG).show()
                monitor.requestPermission()
                return@setOnClickListener
            }

            // 3. 全都有了，启动！
            val intent = Intent(this, InterferenceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            // 启动后可以把 Activity 关掉，或者最小化
            moveTaskToBack(true)
        }
    }
}
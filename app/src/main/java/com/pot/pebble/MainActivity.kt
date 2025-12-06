package com.pot.pebble

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.pot.pebble.service.InterferenceService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 确保你有个 layout xml

        // 假设 layout 里有一个 ID 为 btn_start 的按钮
        // 如果你是 Empty Views Activity 模板，自己去 res/layout/activity_main.xml 加个 Button
        val btnStart = findViewById<Button>(R.id.btn_start) // 临时用一下 id，没有就自己加

        btnStart.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                // 没权限，去申请
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 100)
            } else {
                // 有权限，启动地狱模式
                val intent = Intent(this, InterferenceService::class.java)
                startService(intent)
            }
        }
    }
}
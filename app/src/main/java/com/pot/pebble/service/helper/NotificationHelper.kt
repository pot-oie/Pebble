package com.pot.pebble.service.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import com.pot.pebble.R

class NotificationHelper(private val service: Service) {

    fun startForeground() {
        val channelId = "pebble_service_channel_high" // 改个ID，强制刷新配置

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Pebble 专注保活" // 改个名字

            // 提升重要性到 HIGH (会弹出通知声音/震动，极大提升保活率)
            // 用户如果不喜欢，可以手动在系统设置里静音，但作为开发者请求最高级
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lightColor = Color.BLUE
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null) // 尽量静音，减少打扰
                enableVibration(false)
            }

            val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(service, channelId)
                .setContentTitle("Pebble 正在运行")
                .setContentText("保持专注监测中...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true) // 设置为常驻
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(service)
                .setContentTitle("Pebble 正在运行")
                .setContentText("保持专注监测中...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
        }

        if (Build.VERSION.SDK_INT >= 34) {
            service.startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            service.startForeground(1, notification)
        }
    }
}
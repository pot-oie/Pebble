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

// 通知管理组件
class NotificationHelper(private val service: Service) {

    fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "pebble_service_channel"
            val channelName = "Pebble 专注服务"

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lightColor = Color.BLUE
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val notification: Notification = Notification.Builder(service, channelId)
                .setContentTitle("Pebble 正在运行")
                .setContentText("正在监测专注状态...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                service.startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                service.startForeground(1, notification)
            }
        }
    }
}
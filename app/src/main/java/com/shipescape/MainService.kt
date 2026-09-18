package com.shipescape

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shipescape.utils.BeaconScanner

// 获取坐标、连接服务器
class MainService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()
        startScanner()

        return START_STICKY
    }

    private fun startScanner() {
        try {
            val context = this
            val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
            BeaconScanner(bluetoothAdapter!!.bluetoothLeScanner!!).start()
        } catch (_: Exception) { // 蓝牙未开启
            stopSelf()
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "scanner"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, this.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(this.getString(R.string.app_name)).setContentText("正在运行")
            .setSmallIcon(android.R.drawable.ic_menu_info_details).setOngoing(true).build()
        startForeground(1001, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
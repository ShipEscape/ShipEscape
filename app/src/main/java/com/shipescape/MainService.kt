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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shipescape.utils.BeaconScanner
import com.shipescape.utils.SharedState
import com.shipescape.utils.serverUrlStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

// 获取坐标、连接服务器
class MainService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            serverUrlStore.data
                .distinctUntilChanged()
                .collect { url ->
                    Log.d("MainService", "serverUrlStore changed: $url")
                    reconnectWebSocket(url)
                }
        }
    }

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


    private fun reconnectWebSocket(url: String) {
        try {
            webSocket?.close(1000, "已重新设置 URL")
            webSocket = null
            val request = Request.Builder().url(url).build()
            webSocket = client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                        SharedState._connected.value = true
                    }

                    /**
                     *接收服务端报警信息
                     */
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val json = JSONObject(text)
                            Log.d("MainService", json.toString())
                            SharedState._alert.value = json.optBoolean("alert", false)
                            SharedState._fireCenterX.value =
                                json.optInt("fireCenterX", -1)
                            SharedState._fireCenterY.value =
                                json.optInt("fireCenterY", -1)
                            SharedState._fireIntensity.value =
                                json.optDouble("fireIntensity", 1.0).toFloat()
                        } catch (_: Exception) {
                        }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: okhttp3.Response?
                    ) {
                        SharedState._errorMsg.value = "连接失败: ${t.message}"
                        SharedState._connected.value = false
                        serviceScope.launch {
                            delay(1000.milliseconds)
                            reconnectWebSocket(url)
                        }
                    }


                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        SharedState._errorMsg.value = "连接关闭: $reason"
                        webSocket.close(1000, null)
                        serviceScope.launch {
                            delay(1000.milliseconds)
                            if (webSocket === this@MainService.webSocket) {
                                SharedState._connected.value = false
                            }
                            reconnectWebSocket(url)
                        }
                    }
                }
            )
        } catch (_: Exception) {
            SharedState._connected.value = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SharedState._connected.value = false
        webSocket?.close(1000, "服务已关闭")
        webSocket = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
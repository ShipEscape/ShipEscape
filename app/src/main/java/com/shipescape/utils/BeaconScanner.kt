package com.shipescape.utils

import android.annotation.SuppressLint
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
val maxTimeMillis: Long = 1000 * 60

class BeaconScanner(private val scanner: BluetoothLeScanner) {
    private val scanSettings =
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE).build()

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            val name = result.device.name
            val deviceAddress = result.device.address
            val rssi = result.rssi
            // 获取平滑后的 RSSI
            val smoothRssi = filterRssi(deviceAddress, rssi)
            SharedState.bluetoothDevices[deviceAddress] =
                BluetoothDevice(name, smoothRssi, System.currentTimeMillis())
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BeaconScanner", "扫描失败，错误代码: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun start(targetMacs: List<String> = emptyList()) {
        val filters = mutableListOf<ScanFilter>()

        // TODO:过滤规则
        for (i in targetMacs) filters.add(ScanFilter.Builder().setDeviceAddress(i).build())

        scanner.startScan(filters, scanSettings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        scanner.stopScan(scanCallback)
    }

    // 滤波
    private val rssiHistory = mutableMapOf<String, ArrayDeque<Int>>()
    private val windowSize = 5 // 缓存最近 5 次信号取均值

    private fun filterRssi(mac: String, newRssi: Int): Double {
        val queue = rssiHistory.getOrPut(mac) { ArrayDeque() }
        if (queue.size >= windowSize) {
            queue.removeFirst()
        }
        queue.addLast(newRssi)
        return queue.average()
    }
}
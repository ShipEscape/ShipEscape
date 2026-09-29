package com.shipescape.utils

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BluetoothDevice(
    val name: String?,
    val rssi: Double?,
    val lastRefreshMillis: Long = 0
)


object SharedState {
    val bluetoothDevices = mutableStateMapOf<String, BluetoothDevice>()
    val _errorMsg = MutableStateFlow("")
    val errorMsg = _errorMsg.asStateFlow()
    val _alert = MutableStateFlow(false)
    val alert = _alert.asStateFlow()
    val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()
}

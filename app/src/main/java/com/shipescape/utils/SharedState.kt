package com.shipescape.utils

import androidx.compose.runtime.mutableStateMapOf

data class BluetoothDevice(
    val name: String?,
    val rssi: Double?,
    val lastRefreshMillis:Long=0
)


object SharedState {
//    val _errorMsg = MutableStateFlow("")
//    val errorMsg = _errorMsg.asStateFlow()
    val bluetoothDevices = mutableStateMapOf<String, BluetoothDevice>()
}

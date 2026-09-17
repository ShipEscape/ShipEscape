package com.shipescape.utils

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
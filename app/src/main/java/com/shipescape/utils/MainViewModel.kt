package com.shipescape.utils

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyMap

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var addrToSave by mutableStateOf("")
    var nameToSave by mutableStateOf("")
    var txPowerToSave by mutableStateOf("-59")
    var beaconDialogExpanded by mutableStateOf(false)
    private val beaconStore = application.beaconStore

    val beaconMapState = beaconStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun saveBeaconKeyValue(key: String, value: String) {
        viewModelScope.launch {
            beaconStore.updateData { it + (key to value) }
        }
    }

    fun deleteBeaconKey(key: String) {
        viewModelScope.launch {
            beaconStore.updateData { it - key }
        }
    }


    private val beaconPositionStore = application.beaconPositionStore

    val beaconPositionMapState = beaconPositionStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun saveBeaconPositionKeyValue(key: String, value: String) {
        viewModelScope.launch {
            beaconPositionStore.updateData { it + (key to value) }
        }
    }

    fun deleteBeaconPositionKey(key: String) {
        viewModelScope.launch {
            beaconPositionStore.updateData { it - key }
        }
    }

    private val beaconTxPowerStore = application.beaconTxPowerStore

    val beaconTxPowerMapState = beaconTxPowerStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun saveBeaconTxPowerKeyValue(key: String, value: Double) {
        viewModelScope.launch {
            beaconTxPowerStore.updateData { it + (key to value) }
        }
    }

    fun deleteBeaconTxPowerKey(key: String) {
        viewModelScope.launch {
            beaconTxPowerStore.updateData { it - key }
        }
    }
}
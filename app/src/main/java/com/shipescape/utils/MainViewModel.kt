package com.shipescape.utils

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    var beaconDialogExpanded by mutableStateOf(false)
    private val dataStore = application.beaconStore

    val beaconMapState = dataStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun saveKeyValue(key: String, value: String) {
        viewModelScope.launch {
            dataStore.updateData { it + (key to value) }
        }
    }

    fun deleteKey(key: String) {
        viewModelScope.launch {
            dataStore.updateData { it - key }
        }
    }
}
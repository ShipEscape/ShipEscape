package com.shipescape.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipescape.utils.BluetoothDevice
import com.shipescape.utils.MainViewModel
import com.shipescape.utils.SharedState

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer, topBar = {
        LargeFlexibleTopAppBar(
            title = { Text("设置") }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ), scrollBehavior = scrollBehavior
        )
    }, modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("已添加的蓝牙信标", color = MaterialTheme.colorScheme.primary)
            val beaconMap by viewModel.beaconMapState.collectAsStateWithLifecycle()
            val sortedBeaconList = remember(beaconMap) {
                beaconMap.entries.sortedBy { it.value }
            }
            LazyColumn(Modifier.heightIn(0.dp, 500.dp)) {
                items(
                    items = sortedBeaconList, key = { entry -> entry.key }) { entry ->
                    BluetoothDeviceRow(
                        entry.key, BluetoothDevice(
                            entry.value, SharedState.bluetoothDevices[entry.key]?.rssi
                        ), beaconMap, viewModel
                    )
                    Spacer(Modifier.size(6.dp))
                }
            }
            Text("添加蓝牙信标", color = MaterialTheme.colorScheme.primary)
            val deviceList by remember {
                derivedStateOf {
                    SharedState.bluetoothDevices.entries.sortedByDescending { it.value.rssi }
                }
            }
            LazyColumn(Modifier.height(500.dp)) {
                items(
                    items = deviceList, key = { (addr, _) -> addr }) { (addr, device) ->
                    BluetoothDeviceRow(addr, device, beaconMap, viewModel)
                    Spacer(Modifier.size(6.dp))
                }
            }
        }
    }
    if (viewModel.beaconDialogExpanded) AlertDialog(
        { viewModel.beaconDialogExpanded = false },
        title = { Text("编辑") },
        text = {
            TextField(viewModel.nameToSave, { viewModel.nameToSave = it }, label = { Text("名称") })
        },
        confirmButton = {
            TextButton({
                viewModel.saveKeyValue(viewModel.addrToSave, viewModel.nameToSave)
                viewModel.beaconDialogExpanded = false
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton({
                viewModel.deleteKey(viewModel.addrToSave)
                viewModel.beaconDialogExpanded = false
            }) {
                Text("删除")
            }
        })
}


@Composable
fun BluetoothDeviceRow(
    addr: String, device: BluetoothDevice, beaconMap: Map<String, String>, viewModel: MainViewModel
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(device.name ?: "<无名称>", fontSize = 16.sp)
            Text(
                addr,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 12.sp
            )
            if (device.rssi != null) Text(
                device.rssi.toString(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 12.sp
            )
        }
        IconButton({
            viewModel.addrToSave = addr
            viewModel.nameToSave = beaconMap.getOrDefault(addr, device.name ?: addr)
            viewModel.beaconDialogExpanded = true
        }) {
            if (addr in beaconMap.keys) Icon(Icons.Default.Edit, null)
            else Icon(Icons.Default.Add, null)
        }
    }
}
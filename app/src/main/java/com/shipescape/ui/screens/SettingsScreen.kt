package com.shipescape.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipescape.utils.BluetoothDevice
import com.shipescape.utils.MainViewModel
import com.shipescape.utils.SharedState
import com.shipescape.utils.exportDatastore
import com.shipescape.utils.importDatastore
import com.shipescape.utils.restartApp
import java.io.File
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var exportDialogExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dataStoreDir = remember { File(context.filesDir, "datastore") }
    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer, topBar = {
        LargeFlexibleTopAppBar(
            title = { Text("设置") }, actions = {
            IconButton(
                {
                    exportDialogExpanded = true
                }, colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.1f
                    )
                )
            ) { Icon(Icons.Default.ImportExport, null) }
        }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ), scrollBehavior = scrollBehavior
        )
    }, modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) { innerPadding ->
        val beaconMap by viewModel.beaconMapState.collectAsStateWithLifecycle()
        val beaconTxPowerMap by viewModel.beaconTxPowerMapState.collectAsStateWithLifecycle()
        val sortedBeaconList = remember(beaconMap) {
            beaconMap.entries.sortedBy { it.value }
        }
        var beaconFilter by remember { mutableStateOf("") }
        val deviceList by remember {
            derivedStateOf {
                SharedState.bluetoothDevices.entries.sortedByDescending { it.value.rssi }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "title_added_beacons") {
                Text("已添加的蓝牙信标", color = MaterialTheme.colorScheme.primary)
            }

            items(
                items = sortedBeaconList, key = { entry -> "added_${entry.key}" }) { entry ->
                BluetoothDeviceRow(
                    entry.key, BluetoothDevice(
                        entry.value, SharedState.bluetoothDevices[entry.key]?.rssi
                    ), beaconMap, beaconTxPowerMap, viewModel
                )
            }
            item(key = "title_scan") {
                Spacer(Modifier.height(10.dp))
                Text("添加蓝牙信标", color = MaterialTheme.colorScheme.primary)
                TextField(
                    beaconFilter,
                    { beaconFilter = it },
                    Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Search, null) })
            }

            items(
                items = deviceList, key = { (addr, _) -> "scanned_$addr" }) { (addr, device) ->
                if (beaconFilter.isEmpty() || device.name?.contains(
                        beaconFilter, ignoreCase = true
                    ) == true || addr.contains(
                        beaconFilter, ignoreCase = true
                    )
                ) {
                    BluetoothDeviceRow(
                        addr, device, beaconMap, beaconTxPowerMap, viewModel
                    )
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { exportDatastore(context, dataStoreDir, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && importDatastore(context, dataStoreDir, uri)) {
            Toast.makeText(context, "导入成功! 应用即将重启", Toast.LENGTH_LONG).show()
            restartApp(context)
        } else if (uri != null) {
            Toast.makeText(context, "导入失败, 请选择本应用导出的文件", Toast.LENGTH_LONG).show()
        }
    }

    if (exportDialogExpanded) {
        AlertDialog({ exportDialogExpanded = false }, title = { Text("配置管理") }, text = {
            Column {
                ConfigItem("导出", "导出当前配置", Icons.Default.Download) {
                    exportLauncher.launch("ShipEscape_" + System.currentTimeMillis() + ".zip")
                    exportDialogExpanded = false
                }
                ConfigItem("导入", "导入配置并覆盖当前配置", Icons.Default.Upload) {
                    importLauncher.launch(
                        arrayOf(
                            "application/zip", "application/x-zip-compressed", "*/*"
                        )
                    )
                    exportDialogExpanded = false
                }
            }
        }, confirmButton = {
            TextButton({
                exportDialogExpanded = false
            }) {
                Text("取消")
            }
        })
    }

    if (viewModel.beaconDialogExpanded) AlertDialog(
        { viewModel.beaconDialogExpanded = false },
        title = { Text("编辑") },
        text = {
            Column {
                TextField(
                    viewModel.nameToSave,
                    { viewModel.nameToSave = it },
                    label = { Text("名称") })
                TextField(
                    viewModel.txPowerToSave,
                    { viewModel.txPowerToSave = it },
                    label = { Text("TxPower") })
            }
        },
        confirmButton = {
            TextButton({
                viewModel.saveBeaconKeyValue(viewModel.addrToSave, viewModel.nameToSave)
                try {
                    viewModel.saveBeaconTxPowerKeyValue(
                        viewModel.addrToSave, viewModel.txPowerToSave.toDouble()
                    )
                } catch (_: Exception) { // 用户输入不合法
                    viewModel.saveBeaconTxPowerKeyValue(
                        viewModel.addrToSave,
                        SharedState.bluetoothDevices[viewModel.addrToSave]?.rssi ?: -59.0
                    )
                }
                viewModel.beaconDialogExpanded = false
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton({
                viewModel.deleteBeaconKey(viewModel.addrToSave)
                viewModel.deleteBeaconPositionKey(viewModel.addrToSave)
                viewModel.deleteBeaconTxPowerKey(viewModel.addrToSave)
                viewModel.beaconDialogExpanded = false
            }) { Text("删除") }
        })
}


@Composable
fun BluetoothDeviceRow(
    addr: String,
    device: BluetoothDevice,
    beaconMap: Map<String, String>,
    beaconTxPowerMap: Map<String, Double>,
    viewModel: MainViewModel
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
                String.format(Locale.US, "%.1f", device.rssi),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 12.sp
            )
        }
        FilledIconButton(
            {
                viewModel.addrToSave = addr
                viewModel.nameToSave = beaconMap.getOrDefault(addr, device.name ?: addr)
                viewModel.txPowerToSave = String.format(
                    Locale.US, "%.1f", beaconTxPowerMap.getOrDefault(addr, device.rssi ?: "-59.0")
                )
                viewModel.beaconDialogExpanded = true
            }, colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            if (addr in beaconMap.keys) Icon(Icons.Default.Edit, null)
            else Icon(Icons.Default.Add, null)
        }
    }
}

@Composable
fun ConfigItem(
    title: String,
    desc: String? = null,
    icon: ImageVector? = null,
    background: Color = MaterialTheme.colorScheme.secondaryContainer,
    onClick: () -> Unit
) {
    Row(Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable { onClick() }
        .background(background)
        .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(36.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            if (desc != null) {
                Text(
                    desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}
package com.shipescape

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shipescape.ui.theme.ShipEscapeTheme
import com.shipescape.utils.locationPermissions
import com.shipescape.utils.nearbyPermissions
import com.shipescape.utils.notificationPermissions

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShipEscapeTheme {
                Surface { WelcomeUI() }
            }
        }
    }
}

@Composable
fun WelcomeUI() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var locationPermissionGranted by remember { mutableStateOf(false) }
    var nearbyDevicesPermissionGranted by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var batteryOptimizationIgnored by remember { mutableStateOf(false) }

    fun checkPermissions() {
        locationPermissionGranted = locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        nearbyDevicesPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            nearbyPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        } else true

        notificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        batteryOptimizationIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
    }

    val nearbyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
    }

    // 切回时刷新状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 启动时检查权限
    LaunchedEffect(Unit) {
        checkPermissions()
    }

    val allRequiredGranted = locationPermissionGranted && nearbyDevicesPermissionGranted

    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp), contentAlignment = Alignment.Center
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(80.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painterResource(R.drawable.ic_launcher), null,
                    Modifier.clip(CircleShape).size(150.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text("欢迎使用 " + stringResource(R.string.app_name), fontSize = 32.sp)
                Spacer(Modifier.size(40.dp))
            }

            Column {
                Text(
                    "需要", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(8.dp))

                PermissionItem(
                    granted = locationPermissionGranted,
                    icon = Icons.Default.LocationOn,
                    title = "位置权限",
                    desc = "使用蓝牙信标定位",
                    onClick = {
                        if (!locationPermissionGranted) {
                            locationLauncher.launch(locationPermissions)
                        }
                    })

                PermissionItem(
                    granted = nearbyDevicesPermissionGranted,
                    icon = Icons.Default.Bluetooth,
                    title = "附近设备权限",
                    desc = "发现附近的蓝牙信标",
                    onClick = {
                        if (!nearbyDevicesPermissionGranted) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                nearbyLauncher.launch(nearbyPermissions)
                            }
                        }
                    })

                Spacer(Modifier.height(16.dp))

                Text(
                    "可选", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(8.dp))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionItem(
                        granted = notificationPermissionGranted,
                        icon = Icons.Default.Notifications,
                        title = "通知权限",
                        desc = "接收警报通知",
                        onClick = {
                            if (!notificationPermissionGranted) {
                                notificationLauncher.launch(notificationPermissions)
                            }
                        })
                }

                PermissionItem(
                    granted = batteryOptimizationIgnored,
                    icon = Icons.Default.BatterySaver,
                    title = "忽略电池优化",
                    desc = "在后台接收警报",
                    onClick = {
                        if (!batteryOptimizationIgnored) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    })

                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, MainActivity::class.java))
                            (context as ComponentActivity).finish()
                        }, enabled = allRequiredGranted
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("开始使用")
                            Spacer(Modifier.size(4.dp))
                            Icon(Icons.Default.ArrowUpward, null, Modifier.rotate(90f))
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun PermissionItem(
    granted: Boolean, icon: ImageVector, title: String, desc: String, onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(if (granted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (granted) Icons.Default.CheckCircle else icon, null, Modifier.size(36.dp)
        )
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}
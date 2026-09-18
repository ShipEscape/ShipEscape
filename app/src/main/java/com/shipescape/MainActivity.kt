package com.shipescape

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.shipescape.ui.screens.EditScreen
import com.shipescape.ui.screens.MapScreen
import com.shipescape.ui.screens.SettingsScreen
import com.shipescape.ui.theme.ShipEscapeTheme
import com.shipescape.utils.BluetoothViewModel
import com.shipescape.utils.MainViewModel
import com.shipescape.utils.locationPermissions
import com.shipescape.utils.nearbyPermissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShipEscapeTheme {
                ShipEscapeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@PreviewScreenSizes
@Composable
fun ShipEscapeApp() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val bluetoothViewModel: BluetoothViewModel = viewModel()
    // 变量
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }
    val isBluetoothEnabled by bluetoothViewModel.isBluetoothEnabled.collectAsStateWithLifecycle()

    fun checkPermissions(): Boolean {
        val locationPermissionGranted: Boolean = locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        val nearbyDevicesPermissionGranted: Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                nearbyPermissions.all {
                    ContextCompat.checkSelfPermission(
                        context, it
                    ) == PackageManager.PERMISSION_GRANTED
                }
            } else true
        return locationPermissionGranted && nearbyDevicesPermissionGranted
    }

    // isBluetoothEnabled 变成 true 时，检查权限，启动服务
    // 应用启动时该值默认为 true
    LaunchedEffect(isBluetoothEnabled) {
        if (isBluetoothEnabled) {
            if (!checkPermissions()) {
                val intent = Intent(context, WelcomeActivity::class.java)
                context.startActivity(intent)
                (context as ComponentActivity).finish()
            } else {
                val intent = Intent(context, MainService::class.java)
                context.startForegroundService(intent)
            }
        }
    }

    // 底部选项卡
    // 横屏时，导航栏在左侧
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val layoutType = with(adaptiveInfo) {
        if (windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT && windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT) {
            NavigationSuiteType.NavigationRail
        } else NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    }

    NavigationSuiteScaffold(
        layoutType = layoutType, navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it })
            }
        }) {
        AnimatedContent(
            currentDestination, transitionSpec = {
                val duration = 150
                val slideSpec = tween<IntOffset>(durationMillis = duration, easing = FastOutSlowInEasing)
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally(slideSpec) { width -> width }).togetherWith(
                        slideOutHorizontally(slideSpec) { width -> -width })
                } else {
                    (slideInHorizontally(slideSpec) { width -> -width }).togetherWith(
                        slideOutHorizontally(slideSpec) { width -> width })
                }
            }) { destination ->
            when (destination) {
                AppDestinations.MAP -> MapScreen(viewModel)
                AppDestinations.EDIT -> EditScreen(viewModel)
                AppDestinations.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }

    // 蓝牙未开启提示
    AnimatedVisibility(
        visible = bluetoothViewModel.showBluetoothHint && !isBluetoothEnabled,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
    ) {
        Box(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 10.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("蓝牙未开启", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
                Row {
                    Spacer(Modifier.weight(1f))
                    TextButton({
                        bluetoothViewModel.showBluetoothHint = false
                    }) { Text("忽略") }
                    TextButton({
                        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        context.startActivity(intent)
                    }) { Text("开启蓝牙") }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    MAP("地图", Icons.Default.Map), EDIT("编辑", Icons.Default.Edit), SETTINGS(
        "设置", Icons.Default.Settings
    )
}

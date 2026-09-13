package com.shipescape

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.shipescape.ui.screens.EditScreen
import com.shipescape.ui.screens.MapScreen
import com.shipescape.ui.screens.SettingsScreen
import com.shipescape.ui.theme.ShipEscapeTheme
import com.shipescape.utils.MainViewModel

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

@PreviewScreenSizes
@Composable
fun ShipEscapeApp() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    // 变量
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }

    // 应用打开时，自动启动服务
    LaunchedEffect(Unit) {
        val intent = Intent(context, MainService::class.java)
        context.startForegroundService(intent)
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
        when (currentDestination) {
            AppDestinations.MAP -> MapScreen(viewModel)
            AppDestinations.EDIT -> EditScreen(viewModel)
            AppDestinations.SETTINGS -> SettingsScreen(viewModel)
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

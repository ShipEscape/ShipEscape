package com.shipescape

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.shipescape.ui.screens.MapScreen
import com.shipescape.ui.screens.SettingsScreen
import com.shipescape.ui.theme.ShipEscapeTheme

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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }
    val context= LocalContext.current

    // 应用打开时，自动启动服务
    LaunchedEffect(Unit) {

        val intent = Intent(context, MainService::class.java)
        context.startForegroundService(intent)
    }

    // 底部选项卡
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.MAP -> MapScreen()
            AppDestinations.EDIT -> MapScreen()
            AppDestinations.SETTINGS -> SettingsScreen()
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    MAP("地图", Icons.Default.Map),
    EDIT("编辑", Icons.Default.Edit),
    SETTINGS("设置", Icons.Default.Settings)
}

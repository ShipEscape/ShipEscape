package com.shipescape.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shipescape.R
import com.shipescape.utils.MainViewModel
import com.shipescape.utils.SharedState
import com.shipescape.utils.maxTimeMillis
import com.shipescape.utils.parseCoordinates
import com.shipescape.utils.pathfinding.AStar
import com.shipescape.utils.pathfinding.FirePosition
import com.shipescape.utils.pathfinding.NavGraph
import com.shipescape.utils.pathfinding.PathResult
import com.shipescape.utils.pathfinding.loadMask
import com.shipescape.utils.rssi2Distance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapScreen(viewModel: MainViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    val beaconMap by viewModel.beaconMapState.collectAsStateWithLifecycle()
    val beaconPositionMap by viewModel.beaconPositionMapState.collectAsStateWithLifecycle()
    val exitPositionMap by viewModel.exitPositionMapState.collectAsStateWithLifecycle()
    val beaconTxPowerMap by viewModel.beaconTxPowerMapState.collectAsStateWithLifecycle()

    val alert by SharedState.alert.collectAsStateWithLifecycle()
    val fireCenterX by SharedState.fireCenterX.collectAsStateWithLifecycle()
    val fireCenterY by SharedState.fireCenterY.collectAsStateWithLifecycle()
    val fireIntensity by SharedState.fireIntensity.collectAsStateWithLifecycle()

    var path by remember { mutableStateOf(intArrayOf()) }
    val graph: NavGraph = remember { loadMask(context, "ship_floor_plan.bin") }
    var findingPath by remember { mutableStateOf(false) }

    // 用蓝牙信标信号强度推算当前坐标
    val currentPos by remember {
        derivedStateOf {
            var weightedXSum = 0.0
            var weightedYSum = 0.0
            var weightSum = 0.0

            beaconPositionMap.forEach { (key, valueStr) ->
                val imgCoords = parseCoordinates(valueStr) ?: return@forEach
                val rssi =
                    if ((System.currentTimeMillis() - (SharedState.bluetoothDevices[key]?.lastRefreshMillis
                            ?: 0)) <= maxTimeMillis
                    ) SharedState.bluetoothDevices[key]?.rssi ?: -150.0 else -150.0

                val distance = rssi2Distance(rssi, beaconTxPowerMap[key] ?: -59.0)
                if (distance <= 0.0) return@forEach

                val weight = 1.0 / (distance * distance)
                weightedXSum += imgCoords.x * weight
                weightedYSum += imgCoords.y * weight
                weightSum += weight
            }

            if (weightSum > 0.0) {
                Offset((weightedXSum / weightSum).toFloat(), (weightedYSum / weightSum).toFloat())
            } else {
                null
            }
        }
    }

    /**
     * 规划逃生路线
     */
    fun planEscapeRoute() {
        viewModel.viewModelScope.launch(Dispatchers.Default) {
            findingPath = true
            val start = graph.indexOf(
                currentPos?.x?.roundToInt() ?: 0, currentPos?.y?.roundToInt() ?: 0
            )
            FirePosition(fireCenterX, fireCenterY).applyTo(graph, fireIntensity)
            var minCostPathResult = PathResult(intArrayOf(), Float.MAX_VALUE)
            for (exit in exitPositionMap.values) {
                val (x, y) = parseCoordinates(exit) ?: continue
                val goal = graph.indexOf(x.roundToInt(), y.roundToInt())
                val pathResult = AStar.findPath(graph, start, goal)
                if (pathResult.cost < minCostPathResult.cost) {
                    minCostPathResult = pathResult
                }
            }
            path = minCostPathResult.path
            findingPath = false
        }
    }

    LaunchedEffect(alert) {
        if (alert) {
            planEscapeRoute()
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer, topBar = {
        LargeFlexibleTopAppBar(
            title = {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.app_name))
                    if (findingPath) {
                        Spacer(Modifier.size(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LoadingIndicator(Modifier.size(16.dp))
                                Text("路线规划中...")
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            scrollBehavior = scrollBehavior
        )
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = {
                planEscapeRoute()
            }) { Icon(Default.Navigation, null, Modifier.rotate(45f)) }
    }) { innerPadding ->
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        LaunchedEffect(scale) { // 随着放大收起 LargeFlexibleTopAppBar
            scrollBehavior.state.heightOffset = (scale - 1) * scrollBehavior.state.heightOffsetLimit
        }

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(1f, 8f)
                        offset = (offset - centroid) * (newScale / oldScale) + centroid + pan
                        scale = newScale
                    }
                }) {
            // 背景地图
            val painter = painterResource(id = R.drawable.ship_floor_plan)
            val imgWidth = painter.intrinsicSize.width
            val imgHeight = painter.intrinsicSize.height

            var mapContainerSize by remember { mutableStateOf(IntSize.Zero) }
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .onSizeChanged { size ->
                        mapContainerSize = size
                    })

            if (mapContainerSize.width > 0 && imgWidth > 0) {
                val fitScale =
                    minOf(mapContainerSize.width / imgWidth, mapContainerSize.height / imgHeight)
                val mapImageLeft = (mapContainerSize.width - imgWidth * fitScale) / 2f
                val mapImageTop = (mapContainerSize.height - imgHeight * fitScale) / 2f

                val sizeDp = 24.dp
                val radiusPx = with(LocalDensity.current) { (sizeDp / 2).toPx() }


                // 信标圆点
                beaconPositionMap.forEach { (key, valueStr) ->
                    val imgCoords = parseCoordinates(valueStr)

                    if (imgCoords != null) {
                        val originalX = mapImageLeft + imgCoords.x * fitScale
                        val originalY = mapImageTop + imgCoords.y * fitScale
                        val screenX = originalX * scale + offset.x
                        val screenY = originalY * scale + offset.y

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (screenX - radiusPx).roundToInt(),
                                        y = (screenY - radiusPx).roundToInt()
                                    )
                                }
                                .size(sizeDp)
                                .background(MaterialTheme.colorScheme.background, CircleShape)
                                .border(
                                    width = 2.dp, color = Color.Gray, shape = CircleShape
                                ), contentAlignment = Alignment.Center) {
                            Text(
                                text = (beaconMap[key] ?: key).take(2),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }


                // 出口圆点
                exitPositionMap.forEach { (key, valueStr) ->
                    val imgCoords = parseCoordinates(valueStr)

                    if (imgCoords != null) {
                        val originalX = mapImageLeft + imgCoords.x * fitScale
                        val originalY = mapImageTop + imgCoords.y * fitScale
                        val screenX = originalX * scale + offset.x
                        val screenY = originalY * scale + offset.y

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (screenX - radiusPx).roundToInt(),
                                        y = (screenY - radiusPx).roundToInt()
                                    )
                                }
                                .size(sizeDp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ), contentAlignment = Alignment.Center) {
                            Text(
                                text = (key).take(2),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }


                // 火焰范围
                if (alert) {
                    val originalRadiusPx = FirePosition.BASE_RADIUS_CELLS * sqrt(fireIntensity)*fitScale
                    val currentRadiusPx = originalRadiusPx * scale
                    val currentDiameterDp =
                        with(LocalDensity.current) { (currentRadiusPx * 2).toDp() }
                    // 未缩放时的坐标
                    val originalFireCenterX = mapImageLeft + fireCenterX * fitScale
                    val originalFireCenterY = mapImageTop + fireCenterY * fitScale
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (originalFireCenterX * scale + offset.x - currentRadiusPx).roundToInt(),
                                    y = (originalFireCenterY * scale + offset.y - currentRadiusPx).roundToInt()
                                )
                            }
                            .size(currentDiameterDp)
                            .background(
                                color = Color.Red.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }


                // 当前坐标圆点
                // 未缩放时的坐标
                val originalCurrentPosX = mapImageLeft + (currentPos?.x?.toDouble() ?: 0.0) * fitScale
                val originalCurrentPosY = mapImageTop + (currentPos?.y?.toDouble() ?: 0.0) * fitScale
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (originalCurrentPosX * scale + offset.x - radiusPx).roundToInt(),
                                y = (originalCurrentPosY * scale + offset.y - radiusPx).roundToInt()
                            )
                        }
                        .size(sizeDp)
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                        .border(
                            5.dp, MaterialTheme.colorScheme.primary, CircleShape
                        ))
            }

            // 逃生路线
            if (path.isNotEmpty() && mapContainerSize.width > 0 && imgWidth > 0) {
                val fitScale =
                    minOf(mapContainerSize.width / imgWidth, mapContainerSize.height / imgHeight)
                val left = (mapContainerSize.width - imgWidth * fitScale) / 2f
                val top = (mapContainerSize.height - imgHeight * fitScale) / 2f

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            transformOrigin = TransformOrigin(0f, 0f)
                        }) {
                    val composePath = Path().apply {
                        path.forEachIndexed { index, idx ->
                            val px = left + (graph.xOf(idx) + 0.5f) * fitScale
                            val py = top + (graph.yOf(idx) + 0.5f) * fitScale
                            if (index == 0) moveTo(px, py) else lineTo(px, py)
                        }
                    }

                    drawPath(
                        path = composePath, color = Color(0xFF00E676), style = Stroke(
                            width = 4.dp.toPx() / scale,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
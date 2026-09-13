package com.shipescape.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shipescape.R
import com.shipescape.utils.MainViewModel
import com.shipescape.utils.parseCoordinates
import com.shipescape.utils.x
import com.shipescape.utils.y
import kotlin.math.roundToInt

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(viewModel: MainViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val beaconMap by viewModel.beaconMapState.collectAsStateWithLifecycle()
    val beaconPositionMap by viewModel.beaconPositionMapState.collectAsStateWithLifecycle()

    var selectedBeaconKey by remember { mutableStateOf<String?>(null) }
    var draggingPos by remember { mutableStateOf<Offset?>(null) }

    var mapContainerSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 背景地图
    val painter = painterResource(id = R.drawable.ship_floor_plan)
    val imgWidth = painter.intrinsicSize.width
    val imgHeight = painter.intrinsicSize.height

    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer, topBar = {
        LargeFlexibleTopAppBar(
            title = { Text("编辑") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            scrollBehavior = scrollBehavior,
        )
    }, floatingActionButton = {
        var fabExpanded by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = fabExpanded, enter = fadeIn(tween(200)) + expandVertically(
                    tween(200), expandFrom = Alignment.Bottom
                ), exit = fadeOut(tween(150)) + shrinkVertically(
                    tween(150), shrinkTowards = Alignment.Bottom
                )
            ) {
                Column(
                    Modifier
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.7f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    beaconMap.forEach { i ->
                        FabBeaconItem(
                            i.value,
                            if (i.key in beaconPositionMap.keys) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background,
                            {
                                fabExpanded = false
                                if (i.key !in beaconPositionMap.keys) {
                                    selectedBeaconKey = i.key
                                    // 在屏幕中心创建信标点
                                    if (mapContainerSize.width > 0 && imgWidth > 0) {
                                        val fitScale = minOf(
                                            mapContainerSize.width / imgWidth,
                                            mapContainerSize.height / imgHeight
                                        )
                                        val mapImageLeft =
                                            (mapContainerSize.width - imgWidth * fitScale) / 2f
                                        val mapImageTop =
                                            (mapContainerSize.height - imgHeight * fitScale) / 2f

                                        val screenCenterX = mapContainerSize.width / 2f
                                        val screenCenterY = mapContainerSize.height / 2f
                                        val initialImgX =
                                            (((screenCenterX - offset.x) / scale) - mapImageLeft) / fitScale
                                        val initialImgY =
                                            (((screenCenterY - offset.y) / scale) - mapImageTop) / fitScale

                                        val clampedPos = Offset(
                                            x = initialImgX.coerceIn(0f, imgWidth),
                                            y = initialImgY.coerceIn(0f, imgHeight)
                                        )
                                        draggingPos = clampedPos
                                        viewModel.saveBeaconPositionKeyValue(
                                            i.key, "${clampedPos.x},${clampedPos.y}"
                                        )
                                    }
                                } else viewModel.deleteBeaconPositionKey(i.key)
                            })
                    }
                }
            }
            FloatingActionButton(
                onClick = { fabExpanded = !fabExpanded },
                modifier = Modifier,
                shape = if (fabExpanded) CircleShape else RoundedCornerShape(20.dp)
            ) { Icon(Default.Add, null) }
        }
    }) { innerPadding ->
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

            // 当前坐标圆点
            if (mapContainerSize.width > 0 && imgWidth > 0) {
                val fitScale =
                    minOf(mapContainerSize.width / imgWidth, mapContainerSize.height / imgHeight)
                val mapImageLeft = (mapContainerSize.width - imgWidth * fitScale) / 2f
                val mapImageTop = (mapContainerSize.height - imgHeight * fitScale) / 2f

                // 未缩放时的坐标
                val originalX = mapImageLeft + x * fitScale
                val originalY = mapImageTop + y * fitScale

                val sizeDp = 24.dp
                val radiusPx = with(LocalDensity.current) { (sizeDp / 2).toPx() }
                Box(modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (originalX * scale + offset.x - radiusPx).roundToInt(),
                            y = (originalY * scale + offset.y - radiusPx).roundToInt()
                        )
                    }
                    .size(sizeDp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .border(
                        5.dp, MaterialTheme.colorScheme.primary, CircleShape
                    ))
            }


            // 信标圆点
            if (mapContainerSize.width > 0 && imgWidth > 0) {
                val fitScale = minOf(
                    mapContainerSize.width / imgWidth, mapContainerSize.height / imgHeight
                )
                val mapImageLeft = (mapContainerSize.width - imgWidth * fitScale) / 2f
                val mapImageTop = (mapContainerSize.height - imgHeight * fitScale) / 2f

                val sizeDp = 24.dp
                val radiusPx = with(LocalDensity.current) { (sizeDp / 2).toPx() }

                beaconPositionMap.forEach { (key, valueStr) ->
                    val imgCoords = if (key == selectedBeaconKey && draggingPos != null) draggingPos
                    else parseCoordinates(valueStr)


                    if (imgCoords != null) {
                        val originalX = mapImageLeft + imgCoords.x * fitScale
                        val originalY = mapImageTop + imgCoords.y * fitScale
                        val screenX = originalX * scale + offset.x
                        val screenY = originalY * scale + offset.y
                        val isSelected = key == selectedBeaconKey

                        val currentImgCoords by rememberUpdatedState(imgCoords)
                        val currentFitScale by rememberUpdatedState(fitScale)

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (screenX - radiusPx).roundToInt(),
                                        y = (screenY - radiusPx).roundToInt()
                                    )
                                }
                                .size(sizeDp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                                    CircleShape
                                )
                                .border(
                                    width = if (isSelected) 4.dp else 2.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .pointerInput(key) {
                                    detectDragGestures(onDragStart = {
                                        selectedBeaconKey = key
                                        draggingPos = currentImgCoords
                                    }, onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingPos?.let { current ->
                                            val deltaX = dragAmount.x / (currentFitScale * scale)
                                            val deltaY = dragAmount.y / (currentFitScale * scale)

                                            val newX = (current.x + deltaX).coerceIn(0f, imgWidth)
                                            val newY = (current.y + deltaY).coerceIn(0f, imgHeight)

                                            draggingPos = Offset(newX, newY)
                                        }
                                    }, onDragEnd = {
                                        draggingPos?.let { finalPos ->
                                            viewModel.saveBeaconPositionKeyValue(
                                                key, "${finalPos.x},${finalPos.y}"
                                            )
                                        }
                                    })
                                }, contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (beaconMap[key] ?: key).take(2),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FabBeaconItem(name: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = CircleShape, shadowElevation = 2.dp, color = color
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
            Text(
                text = name, fontSize = 16.sp
            )
        }
    }
}
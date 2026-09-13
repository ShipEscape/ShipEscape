package com.shipescape.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.pow

fun parseCoordinates(value: String): Offset? {
    val parts = value.split(",")
    if (parts.size == 2) {
        val x = parts[0].trim().toFloatOrNull()
        val y = parts[1].trim().toFloatOrNull()
        if (x != null && y != null) return Offset(x, y)
    }
    return null
}

fun rssi2Distance(
    rssi: Double,
    txPower: Double = -59.0,   // TODO: 信标 1 米处的 RSSI
    pathLossExponent: Double = 2.0
): Double {
    if (rssi == 0.0) return -1.0
    return 10.0.pow((txPower - rssi) / (10 * pathLossExponent))
}
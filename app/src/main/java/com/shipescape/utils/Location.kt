package com.shipescape.utils

import androidx.compose.ui.geometry.Offset

fun parseCoordinates(value: String): Offset? {
    val parts = value.split(",")
    if (parts.size == 2) {
        val x = parts[0].trim().toFloatOrNull()
        val y = parts[1].trim().toFloatOrNull()
        if (x != null && y != null) return Offset(x, y)
    }
    return null
}

val x=500
val y=392
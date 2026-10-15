package com.shipescape.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.shipescape.utils.pathfinding.NavGraph
import kotlin.math.roundToInt

/**
 * 生成代价 Bitmap
 */
fun generateCostBitmap(g: NavGraph): ImageBitmap? {
    val w = g.width
    val h = g.height
    val total = g.cellCount()
    val pixels = IntArray(total)

    var maxCost = 8f
    for (i in 0 until total) {
        val c = g.costAt(i)
        if (!c.isInfinite() && c > maxCost) {
            maxCost = c
        }
    }
    val costRange = (maxCost - 8f).coerceAtLeast(1f)

    var hasCostAboveThreshold = false
    for (i in 0 until total) {
        val c = g.costAt(i)
        if (c >= 8f) {
            hasCostAboveThreshold = true
            val ratio = if (c.isInfinite()) 1f else ((c - 8f) / costRange).coerceIn(0f, 1f)
            val alpha = (80 + 150 * ratio).roundToInt().coerceIn(0, 255)
            val red = (255 - 115 * ratio).roundToInt().coerceIn(0, 255)
            val green = (60 * (1f - ratio)).roundToInt().coerceIn(0, 255)
            val blue = 0

            pixels[i] = android.graphics.Color.argb(alpha, red, green, blue)
        } else pixels[i] = 0

    }

    if (!hasCostAboveThreshold) return null
    val bmp = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    return bmp.asImageBitmap()
}
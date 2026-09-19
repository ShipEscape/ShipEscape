package com.shipescape.utils.pathfinding

import android.content.Context
import java.io.DataInputStream


/**
 * 构建 NavGraph
 */
fun loadMask(context: Context, assetPath: String): NavGraph {
    context.assets.open(assetPath).use { `is` ->
        DataInputStream(`is`).use { dis ->
            // 读取文件头, 获取宽高
            // 结构: [width (4 bytes)][height (4 bytes)][像素数据 (width*height bytes)]
            val width = dis.readInt()
            val height = dis.readInt()

            val totalCells = width * height
            val rawBytes = ByteArray(totalCells)

            dis.readFully(rawBytes)

            val mask = BooleanArray(totalCells)
            for (i in 0..<totalCells) {
                mask[i] = (rawBytes[i].toInt() == 1) // 1 为通路，0 为障碍
            }

            val graph = NavGraph(width, height)
            graph.fillFromMask(mask)
            return graph
        }
    }
}
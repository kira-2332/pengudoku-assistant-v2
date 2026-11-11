package com.pengudoku.assistant

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

data class DetectedGrid(
    val bounds: BoardBounds,
    val size: Int,
    val cellWidth: Float,
    val cellHeight: Float
)

class BoardDetector {

    fun detectGrid(
        bitmap: Bitmap,
        expectedSize: Int = 6
    ): DetectedGrid? {
        if (bitmap.isRecycled) return null
        if (bitmap.width < 100 || bitmap.height < 100) return null
        if (expectedSize !in 2..12) return null

        val xLines = findGridLines(bitmap, horizontal = false)
        val yLines = findGridLines(bitmap, horizontal = true)

        val left = xLines.firstOrNull() ?: return null
        val right = xLines.lastOrNull() ?: return null
        val top = yLines.firstOrNull() ?: return null
        val bottom = yLines.lastOrNull() ?: return null

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) return null

        val cellWidth = width.toFloat() / expectedSize
        val cellHeight = height.toFloat() / expectedSize

        if (abs(cellWidth - cellHeight) > maxOf(cellWidth, cellHeight) * 0.35f) {
            return null
        }

        return DetectedGrid(
            bounds = BoardBounds(left, top, right, bottom),
            size = expectedSize,
            cellWidth = cellWidth,
            cellHeight = cellHeight
        )
    }

    private fun findGridLines(
        bitmap: Bitmap,
        horizontal: Boolean
    ): List<Int> {
        val length = if (horizontal) bitmap.height else bitmap.width
        val crossLength = if (horizontal) bitmap.width else bitmap.height
        val step = maxOf(1, crossLength / 80)
        val threshold = maxOf(5, crossLength / 20)

        val lines = mutableListOf<Int>()
        var runStart = -1

        for (position in 0 until length) {
            var darkCount = 0

            var cross = 0
            while (cross < crossLength) {
                val x = if (horizontal) cross else position
                val y = if (horizontal) position else cross

                val color = bitmap.getPixel(x, y)
                if (Color.red(color) < 75 &&
                    Color.green(color) < 75 &&
                    Color.blue(color) < 75
                ) {
                    darkCount++
                }
                cross += step
            }

            val isLine = darkCount >= threshold

            if (isLine && runStart < 0) runStart = position

            if ((!isLine || position == length - 1) && runStart >= 0) {
                lines.add((runStart + position) / 2)
                runStart = -1
            }
        }

        return lines
    }
}

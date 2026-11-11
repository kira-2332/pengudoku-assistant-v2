package com.pengudoku.assistant

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

data class ClassifiedCell(
    val row: Int,
    val column: Int,
    val colorId: Int,
    val blocked: Boolean
)

class CellClassifier {

    fun classify(
        bitmap: Bitmap,
        grid: DetectedGrid
    ): List<ClassifiedCell>? {
        if (bitmap.isRecycled || grid.size <= 0) return null

        val bounds = grid.bounds
        if (!bounds.isValid()) return null

        val samples = mutableListOf<Int>()

        for (row in 0 until grid.size) {
            for (column in 0 until grid.size) {
                val x = (bounds.left +
                    (column + 0.5f) * grid.cellWidth).toInt()
                val y = (bounds.top +
                    (row + 0.5f) * grid.cellHeight).toInt()

                if (x !in 0 until bitmap.width ||
                    y !in 0 until bitmap.height
                ) return null

                samples.add(bitmap.getPixel(x, y))
            }
        }

        val palette = mutableListOf<Int>()
        val result = mutableListOf<ClassifiedCell>()

        var index = 0
        for (row in 0 until grid.size) {
            for (column in 0 until grid.size) {
                val color = samples[index++]
                val blocked = isBlockedCell(bitmap, grid, row, column)

                var colorId = -1
                if (!blocked) {
                    colorId = palette.indexOfFirst {
                        colorDistance(it, color) <= 45.0
                    }

                    if (colorId < 0) {
                        palette.add(color)
                        colorId = palette.lastIndex
                    }
                }

                result.add(
                    ClassifiedCell(
                        row = row,
                        column = column,
                        colorId = colorId,
                        blocked = blocked
                    )
                )
            }
        }

        return result
    }

    private fun isBlockedCell(
        bitmap: Bitmap,
        grid: DetectedGrid,
        row: Int,
        column: Int
    ): Boolean {
        val centerX = grid.bounds.left +
            (column + 0.5f) * grid.cellWidth
        val centerY = grid.bounds.top +
            (row + 0.5f) * grid.cellHeight

        val radiusX = (grid.cellWidth * 0.22f).toInt().coerceAtLeast(2)
        val radiusY = (grid.cellHeight * 0.22f).toInt().coerceAtLeast(2)

        var darkPixels = 0
        var total = 0

        for (offset in -3..3) {
            val x = (centerX + offset * radiusX / 3f).toInt()
            val y = (centerY + offset * radiusY / 3f).toInt()

            if (x in 0 until bitmap.width &&
                y in 0 until bitmap.height
            ) {
                val color = bitmap.getPixel(x, y)
                if (Color.red(color) < 55 &&
                    Color.green(color) < 55 &&
                    Color.blue(color) < 55
                ) {
                    darkPixels++
                }
                total++
            }
        }

        return total > 0 && darkPixels >= 5
    }

    private fun colorDistance(first: Int, second: Int): Double {
        val red = Color.red(first) - Color.red(second)
        val green = Color.green(first) - Color.green(second)
        val blue = Color.blue(first) - Color.blue(second)

        return sqrt(
            (red * red + green * green + blue * blue).toDouble()
        )
    }
}

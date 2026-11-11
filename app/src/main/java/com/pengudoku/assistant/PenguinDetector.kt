package com.pengudoku.assistant

import android.graphics.Bitmap
import android.graphics.Color

class PenguinDetector {

    fun detect(
        bitmap: Bitmap,
        grid: DetectedGrid
    ): Set<Pair<Int, Int>> {
        if (bitmap.isRecycled || grid.size <= 0) {
            return emptySet()
        }

        val detected = mutableSetOf<Pair<Int, Int>>()

        for (row in 0 until grid.size) {
            for (column in 0 until grid.size) {
                val centerX = (
                    grid.bounds.left +
                    (column + 0.5f) * grid.cellWidth
                ).toInt()

                val centerY = (
                    grid.bounds.top +
                    (row + 0.5f) * grid.cellHeight
                ).toInt()

                if (centerX !in 0 until bitmap.width ||
                    centerY !in 0 until bitmap.height
                ) continue

                if (looksLikePenguin(bitmap, centerX, centerY, grid)) {
                    detected.add(row to column)
                }
            }
        }

        return detected
    }

    private fun looksLikePenguin(
        bitmap: Bitmap,
        centerX: Int,
        centerY: Int,
        grid: DetectedGrid
    ): Boolean {
        val radiusX = (grid.cellWidth * 0.18f).toInt().coerceAtLeast(2)
        val radiusY = (grid.cellHeight * 0.18f).toInt().coerceAtLeast(2)

        var darkPixels = 0
        var lightPixels = 0
        var samples = 0

        for (dy in -2..2) {
            for (dx in -2..2) {
                val x = centerX + dx * radiusX / 2
                val y = centerY + dy * radiusY / 2

                if (x !in 0 until bitmap.width ||
                    y !in 0 until bitmap.height
                ) continue

                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                if (red < 75 && green < 75 && blue < 75) {
                    darkPixels++
                }

                if (red > 180 && green > 180 && blue > 180) {
                    lightPixels++
                }

                samples++
            }
        }

        if (samples == 0) return false

        val darkRatio = darkPixels.toFloat() / samples
        val lightRatio = lightPixels.toFloat() / samples

        return darkRatio in 0.15f..0.75f &&
            lightRatio >= 0.08f
    }
}

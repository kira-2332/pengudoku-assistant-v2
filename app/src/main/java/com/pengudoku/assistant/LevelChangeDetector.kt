package com.pengudoku.assistant

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

class LevelChangeDetector(
    private val changeThreshold: Double = 0.30
) {
    private var previousSignature: IntArray? = null

    fun hasLevelChanged(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled) return false
        if (bitmap.width <= 0 || bitmap.height <= 0) return false

        val current = createSignature(bitmap)
        val previous = previousSignature

        previousSignature = current

        if (previous == null || previous.size != current.size) {
            return true
        }

        var differences = 0

        for (index in current.indices) {
            if (colorDifference(
                    previous[index],
                    current[index]
                ) > 65
            ) {
                differences++
            }
        }

        val ratio = differences.toDouble() / current.size

        return ratio >= changeThreshold
    }

    fun reset() {
        previousSignature = null
    }

    private fun createSignature(bitmap: Bitmap): IntArray {
        val columns = 12
        val rows = 12
        val result = IntArray(columns * rows)

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val x = ((column + 0.5f) * bitmap.width / columns)
                    .toInt().coerceIn(0, bitmap.width - 1)

                val y = ((row + 0.5f) * bitmap.height / rows)
                    .toInt().coerceIn(0, bitmap.height - 1)

                result[row * columns + column] =
                    bitmap.getPixel(x, y)
            }
        }

        return result
    }

    private fun colorDifference(first: Int, second: Int): Int {
        return abs(Color.red(first) - Color.red(second)) +
            abs(Color.green(first) - Color.green(second)) +
            abs(Color.blue(first) - Color.blue(second))
    }
}

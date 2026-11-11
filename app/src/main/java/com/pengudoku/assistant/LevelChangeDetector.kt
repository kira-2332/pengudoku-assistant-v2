package com.pengudoku.assistant

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

class LevelChangeDetector(
    private val confirmationFrames: Int = 3,
    private val changeThreshold: Double = 0.35
) {
    private var previousSignature: IntArray? = null
    private var candidateSignature: IntArray? = null
    private var confirmationCount = 0

    /**
     * Compare only the puzzle board, not the full screen.
     * Returns true only after a major board change persists
     * across multiple frames.
     */
    fun hasLevelChanged(
        bitmap: Bitmap,
        board: BoardBounds
    ): Boolean {
        if (bitmap.isRecycled || !board.isValid()) return false

        val left = board.left.coerceIn(0, bitmap.width - 1)
        val right = board.right.coerceIn(left + 1, bitmap.width)
        val top = board.top.coerceIn(0, bitmap.height - 1)
        val bottom = board.bottom.coerceIn(top + 1, bitmap.height)

        val current = createSignature(
            bitmap, left, top, right, bottom
        )

        val previous = previousSignature

        if (previous == null) {
            previousSignature = current
            return false
        }

        val difference = differenceRatio(previous, current)

        if (difference < changeThreshold) {
            candidateSignature = null
            confirmationCount = 0
            previousSignature = current
            return false
        }

        val candidate = candidateSignature

        if (candidate != null &&
            differenceRatio(candidate, current) < 0.12
        ) {
            confirmationCount++
        } else {
            candidateSignature = current
            confirmationCount = 1
        }

        if (confirmationCount >= confirmationFrames) {
            previousSignature = current
            candidateSignature = null
            confirmationCount = 0
            return true
        }

        return false
    }

    fun reset() {
        previousSignature = null
        candidateSignature = null
        confirmationCount = 0
    }

    private fun createSignature(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): IntArray {
        val columns = 12
        val rows = 12
        val result = IntArray(columns * rows)

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val x = left +
                    ((column + 0.5f) * (right - left) / columns).toInt()

                val y = top +
                    ((row + 0.5f) * (bottom - top) / rows).toInt()

                result[row * columns + column] =
                    bitmap.getPixel(x, y)
            }
        }

        return result
    }

    private fun differenceRatio(
        first: IntArray,
        second: IntArray
    ): Double {
        if (first.size != second.size || first.isEmpty()) return 1.0

        var changed = 0

        for (index in first.indices) {
            if (colorDifference(first[index], second[index]) > 65) {
                changed++
            }
        }

        return changed.toDouble() / first.size
    }

    private fun colorDifference(first: Int, second: Int): Int {
        return abs(Color.red(first) - Color.red(second)) +
            abs(Color.green(first) - Color.green(second)) +
            abs(Color.blue(first) - Color.blue(second))
    }
}

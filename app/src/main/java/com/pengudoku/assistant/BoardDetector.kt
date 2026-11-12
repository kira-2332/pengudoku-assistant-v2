package com.pengudoku.assistant

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

        val xGroup = findBestGridGroup(xLines, expectedSize) ?: return null
        val yGroup = findBestGridGroup(yLines, expectedSize) ?: return null

        val left = xGroup.first()
        val right = xGroup.last()
        val top = yGroup.first()
        val bottom = yGroup.last()

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) return null

        val cellWidth = width.toFloat() / expectedSize
        val cellHeight = height.toFloat() / expectedSize

        val difference = abs(cellWidth - cellHeight)
        if (difference > max(cellWidth, cellHeight) * 0.25f) {
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

        val step = max(1, crossLength / 120)
        val threshold = max(5, crossLength / 25)

        val lines = mutableListOf<Int>()
        var runStart = -1

        for (position in 0 until length) {
            var darkCount = 0
            var cross = 0

            while (cross < crossLength) {
                val x = if (horizontal) cross else position
                val y = if (horizontal) position else cross

                val pixel = bitmap.getPixel(x, y)

                if (Color.red(pixel) < 75 &&
                    Color.green(pixel) < 75 &&
                    Color.blue(pixel) < 75
                ) {
                    darkCount++
                }

                cross += step
            }

            val isLine = darkCount >= threshold

            if (isLine && runStart == -1) {
                runStart = position
            }

            if ((!isLine || position == length - 1) && runStart >= 0) {
                val end = if (isLine) position else position - 1
                lines.add((runStart + end) / 2)
                runStart = -1
            }
        }

        return lines
    }

    private fun findBestGridGroup(
        lines: List<Int>,
        size: Int
    ): List<Int>? {
        val required = size + 1
        if (lines.size < required) return null

        var bestGroup: List<Int>? = null
        var bestScore = Float.MAX_VALUE

        for (start in 0..lines.size - required) {
            val group = lines.subList(start, start + required)
            val gaps = mutableListOf<Int>()

            for (i in 1 until group.size) {
                val gap = group[i] - group[i - 1]
                if (gap <= 0) continue
                gaps.add(gap)
            }

            if (gaps.size != size) continue

            val average = gaps.average().toFloat()
            if (average < 8f) continue

            var error = 0f
            for (gap in gaps) {
                error += abs(gap - average) / average
            }

            error /= size

            if (error < bestScore) {
                bestScore = error
                bestGroup = group.toList()
            }
        }

        return if (bestScore <= 0.25f) bestGroup else null
    }
}

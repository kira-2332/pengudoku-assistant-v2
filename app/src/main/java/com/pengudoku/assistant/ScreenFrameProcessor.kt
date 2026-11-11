
package com.pengudoku.assistant

import android.graphics.Bitmap
import android.graphics.Color

data class ScreenFrame(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val capturedAt: Long
)

class ScreenFrameProcessor {

    fun process(bitmap: Bitmap): ScreenFrame? {
        if (bitmap.isRecycled) return null
        if (bitmap.width <= 0 || bitmap.height <= 0) return null

        return ScreenFrame(
            bitmap = bitmap,
            width = bitmap.width,
            height = bitmap.height,
            capturedAt = System.currentTimeMillis()
        )
    }

    fun sampleColor(
        bitmap: Bitmap,
        x: Int,
        y: Int
    ): Int? {
        if (bitmap.isRecycled) return null
        if (x !in 0 until bitmap.width) return null
        if (y !in 0 until bitmap.height) return null

        return bitmap.getPixel(x, y)
    }

    fun colorDistance(first: Int, second: Int): Double {
        val red = Color.red(first) - Color.red(second)
        val green = Color.green(first) - Color.green(second)
        val blue = Color.blue(first) - Color.blue(second)

        return kotlin.math.sqrt(
            (red * red + green * green + blue * blue).toDouble()
        )
    }

    fun isSimilarColor(
        first: Int,
        second: Int,
        tolerance: Double = 35.0
    ): Boolean {
        return colorDistance(first, second) <= tolerance
    }
}

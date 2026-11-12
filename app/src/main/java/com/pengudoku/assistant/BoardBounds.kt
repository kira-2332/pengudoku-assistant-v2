package com.pengudoku.assistant

data class BoardBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    fun isValid(): Boolean {
        return left >= 0 &&
            top >= 0 &&
            right > left &&
            bottom > top
    }
}

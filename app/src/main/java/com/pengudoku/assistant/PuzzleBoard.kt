package com.pengudoku.assistant

data class BoardBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top

    fun isValid(): Boolean {
        return width > 0 && height > 0
    }
}

data class PuzzleCell(
    val row: Int,
    val column: Int,
    val colorId: Int,
    val blocked: Boolean = false,
    val hasPenguin: Boolean = false
)

data class PuzzleBoard(
    val size: Int,
    val bounds: BoardBounds,
    val cells: List<PuzzleCell>,
    val detectedAt: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        if (size <= 0 || !bounds.isValid()) return false
        if (cells.size != size * size) return false

        val positions = cells.map {
            it.row to it.column
        }.toSet()

        if (positions.size != size * size) return false

        return cells.all {
            it.row in 0 until size &&
            it.column in 0 until size &&
            (it.blocked || it.colorId >= 0)
        }
    }

    fun cellAt(row: Int, column: Int): PuzzleCell? {
        return cells.firstOrNull {
            it.row == row && it.column == column
        }
    }
}

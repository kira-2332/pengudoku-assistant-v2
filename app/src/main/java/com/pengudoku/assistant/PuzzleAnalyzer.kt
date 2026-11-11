
package com.pengudoku.assistant

data class PuzzleAnalysis(
    val colorGrid: Array<IntArray>,
    val blockedGrid: Array<BooleanArray>,
    val existingPenguins: Set<Pair<Int, Int>>
)

class PuzzleAnalyzer {

    fun analyze(
        size: Int,
        classifiedCells: List<ClassifiedCell>,
        penguins: Set<Pair<Int, Int>>
    ): PuzzleAnalysis? {

        if (size <= 0) return null
        if (classifiedCells.size != size * size) return null

        val colorGrid = Array(size) { IntArray(size) { -1 } }
        val blockedGrid = Array(size) { BooleanArray(size) }
        val occupied = Array(size) { BooleanArray(size) }

        for (cell in classifiedCells) {
            val row = cell.row
            val column = cell.column

            if (row !in 0 until size || column !in 0 until size) {
                return null
            }

            // Reject duplicate cell coordinates.
            if (occupied[row][column]) return null
            occupied[row][column] = true

            if (!cell.blocked && cell.colorId < 0) return null

            colorGrid[row][column] =
                if (cell.blocked) -1 else cell.colorId

            blockedGrid[row][column] = cell.blocked
        }

        // Ensure every board position was classified.
        if (occupied.any { row -> row.any { !it } }) return null

        if (penguins.any { (row, column) ->
                row !in 0 until size ||
                column !in 0 until size ||
                blockedGrid[row][column]
            }
        ) return null

        return PuzzleAnalysis(
            colorGrid = colorGrid,
            blockedGrid = blockedGrid,
            existingPenguins = penguins
        )
    }
}

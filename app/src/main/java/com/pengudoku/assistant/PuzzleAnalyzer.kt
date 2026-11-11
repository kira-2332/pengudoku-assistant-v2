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

        for (cell in classifiedCells) {
            if (cell.row !in 0 until size ||
                cell.column !in 0 until size
            ) return null

            colorGrid[cell.row][cell.column] =
                if (cell.blocked) -1 else cell.colorId

            blockedGrid[cell.row][cell.column] = cell.blocked
        }

        if (penguins.any { (row, column) ->
                row !in 0 until size || column !in 0 until size
            }
        ) return null

        return PuzzleAnalysis(
            colorGrid = colorGrid,
            blockedGrid = blockedGrid,
            existingPenguins = penguins
        )
    }
}

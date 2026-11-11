
package com.pengudoku.assistant

data class PenguinCell(
    val row: Int,
    val column: Int,
    val color: Int
)

class PuzzleSolver {

    fun solve(
        colorGrid: Array<IntArray>,
        blocked: Array<BooleanArray>? = null
    ): List<PenguinCell>? {

        val size = colorGrid.size

        if (size == 0) return null
        if (colorGrid.any { it.size != size }) return null

        if (blocked != null) {
            if (blocked.size != size) return null
            if (blocked.any { it.size != size }) return null
        }

        val colors = colorGrid
            .flatMap { it.toList() }
            .filter { it >= 0 }
            .distinct()

        if (colors.size != size) return null

        val candidates = colors.associateWith { color ->
            (0 until size).flatMap { row ->
                (0 until size).mapNotNull { column ->
                    if (
                        colorGrid[row][column] == color &&
                        (blocked == null || !blocked[row][column])
                    ) {
                        PenguinCell(row, column, color)
                    } else {
                        null
                    }
                }
            }
        }

        if (candidates.values.any { it.isEmpty() }) {
            return null
        }

        val orderedColors = colors.sortedBy {
            candidates[it]!!.size
        }

        val solution = mutableListOf<PenguinCell>()
        val usedRows = BooleanArray(size)
        val usedColumns = BooleanArray(size)

        fun canPlace(cell: PenguinCell): Boolean {
            if (usedRows[cell.row]) return false
            if (usedColumns[cell.column]) return false

            for (other in solution) {
                val rowDistance =
                    kotlin.math.abs(cell.row - other.row)
                val columnDistance =
                    kotlin.math.abs(cell.column - other.column)

                if (rowDistance <= 1 && columnDistance <= 1) {
                    return false
                }
            }

            return true
        }

        fun search(colorIndex: Int): Boolean {
            if (colorIndex == orderedColors.size) {
                return true
            }

            val color = orderedColors[colorIndex]

            for (cell in candidates[color]!!) {
                if (!canPlace(cell)) continue

                solution.add(cell)
                usedRows[cell.row] = true
                usedColumns[cell.column] = true

                if (search(colorIndex + 1)) {
                    return true
                }

                solution.removeAt(solution.lastIndex)
                usedRows[cell.row] = false
                usedColumns[cell.column] = false
            }

            return false
        }

        return if (search(0)) {
            solution.toList()
        } else {
            null
        }
    }
}

package com.pengudoku.assistant

data class PenguinCell(
    val row: Int,
    val column: Int,
    val color: Int
)

class PuzzleSolver {

    fun solve(
        colorGrid: Array<IntArray>,
        blocked: Array<BooleanArray>? = null,
        existingPenguins: Set<Pair<Int, Int>> = emptySet()
    ): List<PenguinCell>? {

        val size = colorGrid.size
        if (size == 0 || colorGrid.any { it.size != size }) return null

        if (blocked != null &&
            (blocked.size != size || blocked.any { it.size != size })
        ) return null

        if (existingPenguins.any { (r, c) ->
                r !in 0 until size || c !in 0 until size
            }
        ) return null

        val colors = colorGrid.flatMap { it.toList() }
            .filter { it >= 0 }
            .distinct()

        if (colors.size != size) return null

        val fixed = existingPenguins.map { (r, c) ->
            val color = colorGrid[r][c]
            if (color < 0 || (blocked?.get(r)?.get(c) == true)) return null
            PenguinCell(r, c, color)
        }

        if (fixed.map { it.color }.distinct().size != fixed.size) return null
        if (fixed.map { it.row }.distinct().size != fixed.size) return null
        if (fixed.map { it.column }.distinct().size != fixed.size) return null

        for (i in fixed.indices) {
            for (j in i + 1 until fixed.size) {
                if (kotlin.math.abs(fixed[i].row - fixed[j].row) <= 1 &&
                    kotlin.math.abs(fixed[i].column - fixed[j].column) <= 1
                ) return null
            }
        }

        val fixedColors = fixed.map { it.color }.toSet()
        val solution = fixed.toMutableList()

        val usedRows = BooleanArray(size)
        val usedColumns = BooleanArray(size)

        fixed.forEach {
            usedRows[it.row] = true
            usedColumns[it.column] = true
        }

        val remainingColors = colors.filterNot { it in fixedColors }

        val candidates = remainingColors.associateWith { color ->
            (0 until size).flatMap { row ->
                (0 until size).mapNotNull { column ->
                    if (colorGrid[row][column] == color &&
                        blocked?.get(row)?.get(column) != true
                    ) PenguinCell(row, column, color) else null
                }
            }
        }

        if (candidates.values.any { it.isEmpty() }) return null

        val orderedColors = remainingColors.sortedBy {
            candidates[it]!!.size
        }

        fun canPlace(cell: PenguinCell): Boolean {
            if (usedRows[cell.row] || usedColumns[cell.column]) return false

            return solution.none { other ->
                kotlin.math.abs(cell.row - other.row) <= 1 &&
                kotlin.math.abs(cell.column - other.column) <= 1
            }
        }

        fun search(index: Int): Boolean {
            if (index == orderedColors.size) {
                return solution.size == size
            }

            val color = orderedColors[index]

            for (cell in candidates[color]!!) {
                if (!canPlace(cell)) continue

                solution.add(cell)
                usedRows[cell.row] = true
                usedColumns[cell.column] = true

                if (search(index + 1)) return true

                solution.removeAt(solution.lastIndex)
                usedRows[cell.row] = false
                usedColumns[cell.column] = false
            }

            return false
        }

        return if (search(0)) solution.toList() else null
    }
}

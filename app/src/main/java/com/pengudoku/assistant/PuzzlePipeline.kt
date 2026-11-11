package com.pengudoku.assistant

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap

class PuzzlePipeline(
    private val context: Context
) {
    private val boardDetector = BoardDetector()
    private val cellClassifier = CellClassifier()
    private val penguinDetector = PenguinDetector()
    private val analyzer = PuzzleAnalyzer()
    private val solver = PuzzleSolver()

    fun process(
        bitmap: Bitmap,
        expectedSize: Int = 6
    ): Boolean {
        if (bitmap.isRecycled) return false

        val grid = boardDetector.detectGrid(bitmap, expectedSize)
            ?: return false

        val classifiedCells = cellClassifier.classify(bitmap, grid)
            ?: return false

        val existingPenguins = penguinDetector.detect(bitmap, grid)

        val analysis = analyzer.analyze(
            size = grid.size,
            classifiedCells = classifiedCells,
            penguins = existingPenguins
        ) ?: return false

        val solution = solver.solve(
            colorGrid = analysis.colorGrid,
            blocked = analysis.blockedGrid,
            existingPenguins = analysis.existingPenguins
        ) ?: return false

        val suggestedCells = solution.filterNot {
            (it.row to it.column) in existingPenguins
        }

        if (suggestedCells.isEmpty()) return false

        val cellCoordinates = IntArray(suggestedCells.size * 2)

        suggestedCells.forEachIndexed { index, cell ->
            cellCoordinates[index * 2] = cell.row
            cellCoordinates[index * 2 + 1] = cell.column
        }

        val overlayIntent = Intent(
            context,
            OverlayService::class.java
        ).apply {
            action = OverlayService.ACTION_SHOW
            putExtra(OverlayService.EXTRA_CELLS, cellCoordinates)
            putExtra(OverlayService.EXTRA_SIZE, grid.size)
            putExtra(OverlayService.EXTRA_LEFT, grid.bounds.left)
            putExtra(OverlayService.EXTRA_TOP, grid.bounds.top)
            putExtra(OverlayService.EXTRA_CELL_WIDTH, grid.cellWidth)
            putExtra(OverlayService.EXTRA_CELL_HEIGHT, grid.cellHeight)
        }

        return try {
            context.startService(overlayIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hideOverlay() {
        val intent = Intent(
            context,
            OverlayService::class.java
        ).apply {
            action = OverlayService.ACTION_HIDE
        }

        try {
            context.startService(intent)
        } catch (_: Exception) {
        }
    }
}

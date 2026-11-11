package com.pengudoku.assistant

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap

class PuzzlePipeline(
    private val context: Context,
    private val expectedSize: Int = 6
) {
    private val boardDetector = BoardDetector()
    private val cellClassifier = CellClassifier()
    private val penguinDetector = PenguinDetector()
    private val analyzer = PuzzleAnalyzer()
    private val solver = PuzzleSolver()
    private val levelChangeDetector = LevelChangeDetector()

    fun process(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            hideOverlay()
            return false
        }

        val board = boardDetector.detectGrid(bitmap, expectedSize)
            ?: return false

        val bounds = board.bounds

        if (levelChangeDetector.hasLevelChanged(bitmap, bounds)) {
            hideOverlay()
        }

        val classifiedCells = cellClassifier.classify(bitmap, board)
            ?: return false

        val penguins = penguinDetector.detect(bitmap, board)

        val analysis = analyzer.analyze(
            size = board.size,
            classifiedCells = classifiedCells,
            penguins = penguins
        ) ?: return false

        val solution = solver.solve(
            colorGrid = analysis.colorGrid,
            blocked = analysis.blockedGrid,
            existingPenguins = analysis.existingPenguins
        ) ?: return false

        val suggestions = solution.filterNot {
            (it.row to it.column) in analysis.existingPenguins
        }

        if (suggestions.isEmpty()) {
            hideOverlay()
            return true
        }

        val cells = IntArray(suggestions.size * 2)

        suggestions.forEachIndexed { index, cell ->
            cells[index * 2] = cell.row
            cells[index * 2 + 1] = cell.column
        }

        val cellWidth = bounds.width.toFloat() / board.size
        val cellHeight = bounds.height.toFloat() / board.size

        val overlayIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW
            putExtra(OverlayService.EXTRA_CELLS, cells)
            putExtra(OverlayService.EXTRA_SIZE, board.size)
            putExtra(OverlayService.EXTRA_LEFT, bounds.left)
            putExtra(OverlayService.EXTRA_TOP, bounds.top)
            putExtra(OverlayService.EXTRA_CELL_WIDTH, cellWidth)
            putExtra(OverlayService.EXTRA_CELL_HEIGHT, cellHeight)
        }

        context.startService(overlayIntent)
        return true
    }

    fun hideOverlay() {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE
        }
        context.startService(intent)
    }

    fun resetLevelDetection() {
        levelChangeDetector.reset()
        hideOverlay()
    }
}

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

    private var lastBoardBounds: BoardBounds? = null

    fun process(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return false
        }

        val board = boardDetector.detectGrid(bitmap, expectedSize)
            ?: return false

        val bounds = board.bounds

        // Check for a stable level change inside the board area.
        if (levelChangeDetector.hasLevelChanged(bitmap, bounds)) {
            hideOverlay()
        }

        lastBoardBounds = bounds

        val classifiedCells = cellClassifier.classify(bitmap, board)
        val penguins = penguinDetector.detect(bitmap, board, classifiedCells)

        val analysis = analyzer.analyze(
            board = board,
            classifiedCells = classifiedCells,
            penguins = penguins
        ) ?: return false

        val solution = solver.solve(
            colorGrid = analysis.colorGrid,
            blocked = analysis.blockedGrid,
            existingPenguins = analysis.existingPenguins
        ) ?: return false

        val suggestions = solution.filterNot { cell ->
            cell.row to cell.column in analysis.existingPenguins
        }

        if (suggestions.isEmpty()) {
            hideOverlay()
            return true
        }

        val cellWidth = bounds.width.toFloat() / board.size
        val cellHeight = bounds.height.toFloat() / board.size

        val coordinates = ArrayList<Float>(suggestions.size * 2)

        suggestions.forEach { cell ->
            val centerX = bounds.left + (cell.column + 0.5f) * cellWidth
            val centerY = bounds.top + (cell.row + 0.5f) * cellHeight

            coordinates.add(centerX)
            coordinates.add(centerY)
        }

        val overlayIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW
            putExtra(OverlayService.EXTRA_COORDINATES, coordinates.toFloatArray())
            putExtra(OverlayService.EXTRA_GRID_SIZE, board.size)
            putExtra(OverlayService.EXTRA_LEFT, bounds.left.toFloat())
            putExtra(OverlayService.EXTRA_TOP, bounds.top.toFloat())
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
        lastBoardBounds = null
        hideOverlay()

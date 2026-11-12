
package com.pengudoku.assistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.pengudoku.assistant.SHOW"
        const val ACTION_HIDE = "com.pengudoku.assistant.HIDE"

        const val EXTRA_CELLS = "solution_cells"
        const val EXTRA_SIZE = "board_size"
        const val EXTRA_LEFT = "board_left"
        const val EXTRA_TOP = "board_top"
        const val EXTRA_CELL_WIDTH = "cell_width"
        const val EXTRA_CELL_HEIGHT = "cell_height"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: SolutionOverlayView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay(intent)
            ACTION_HIDE -> removeOverlay()
        }

        return START_NOT_STICKY
    }

    private fun showOverlay(intent: Intent) {
        removeOverlay()

        val cells = intent.getIntArrayExtra(EXTRA_CELLS) ?: return
        val size = intent.getIntExtra(EXTRA_SIZE, 0)
        val left = intent.getIntExtra(EXTRA_LEFT, 0)
        val top = intent.getIntExtra(EXTRA_TOP, 0)
        val cellWidth =
            intent.getFloatExtra(EXTRA_CELL_WIDTH, 0f)
        val cellHeight =
            intent.getFloatExtra(EXTRA_CELL_HEIGHT, 0f)

        if (size !in 2..12) return
        if (cellWidth <= 0f || cellHeight <= 0f) return
        if (cells.isEmpty() || cells.size % 2 != 0) return

        for (index in cells.indices step 2) {
            if (cells[index] !in 0 until size ||
                cells[index + 1] !in 0 until size
            ) return
        }

        val view = SolutionOverlayView(
            this,
            cells,
            left,
            top,
            cellWidth,
            cellHeight
        )

        val overlayType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START

        try {
            windowManager?.addView(view, params)
            overlayView = view
        } catch (_: Exception) {
            overlayView = null
        }
    }

    private fun removeOverlay() {
        val view = overlayView ?: return

        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        }

        overlayView = null
    }

    override fun onDestroy() {
        removeOverlay()
        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private class SolutionOverlayView(
        context: Context,
        private val cells: IntArray,
        private val boardLeft: Int,
        private val boardTop: Int,
        private val cellWidth: Float,
        private val cellHeight: Float
    ) : View(context) {

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 6f
            setShadowLayer(14f, 0f, 0f, Color.CYAN)
        }

        private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        init {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            isClickable = false
            isFocusable = false
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            for (index in cells.indices step 2) {
                val row = cells[index]
                val column = cells[index + 1]

                val centerX =
                    boardLeft + (column + 0.5f) * cellWidth

                val centerY =
                    boardTop + (row + 0.5f) * cellHeight

                val radius =
                    minOf(cellWidth, cellHeight) * 0.27f

                // Glowing circle around the suggested cell.
                canvas.drawCircle(
                    centerX,
                    centerY,
                    radius,
                    glowPaint
                )

                // Arrow pointing toward the suggested position.
                val arrowStartY = centerY + radius * 1.6f
                val arrowEndY = centerY + radius * 0.65f

                canvas.drawLine(
                    centerX,
                    arrowStartY,
                    centerX,
                    arrowEndY,
                    arrowPaint
                )

                // Left arrow head.
                canvas.drawLine(
                    centerX,
                    arrowEndY,
                    centerX - radius * 0.35f,
                    arrowEndY + radius * 0.4f,
                    arrowPaint
                )

                // Right arrow head.
                canvas.drawLine(
                    centerX,
                    arrowEndY,
                    centerX + radius * 0.35f,
                    arrowEndY + radius * 0.4f,
                    arrowPaint
                )
            }
        }
    }
}

package com.pengudoku.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private const val CHANNEL_ID = "pengudoku_capture"
        private const val NOTIFICATION_ID = 2001
        private const val CAPTURE_INTERVAL_MS = 1200L
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private var pipeline: PuzzlePipeline? = null
    private var lastProcessedAt = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        pipeline = PuzzlePipeline(applicationContext)

        captureThread = HandlerThread("PengudokuCapture").apply {
            start()
        }
        captureHandler = Handler(captureThread!!.looper)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        if (projection != null) {
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(
            EXTRA_RESULT_CODE, 0
        ) ?: 0

        @Suppress("DEPRECATION")
        val resultData = intent?.getParcelableExtra<Intent>(
            EXTRA_RESULT_DATA
        )

        if (resultData == null || resultCode == 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        projection = manager.getMediaProjection(
            resultCode,
            resultData
        )

        if (projection == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startCapture()
        return START_NOT_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()

        @Suppress("DEPRECATION")
        val windowManager =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager

        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            2
        )

        imageReader?.setOnImageAvailableListener(
            { reader ->
                val now = SystemClock.elapsedRealtime()

                if (now - lastProcessedAt < CAPTURE_INTERVAL_MS) {
                    val skippedImage = reader.acquireLatestImage()
                    skippedImage?.close()
                    return@setOnImageAvailableListener
                }

                val image = reader.acquireLatestImage()
                    ?: return@setOnImageAvailableListener

                lastProcessedAt = now

                try {
                    val bitmap = imageToBitmap(image)

                    if (bitmap != null) {
                        try {
                            pipeline?.process(bitmap, expectedSize = 6)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                } catch (_: Exception) {
                    // Ignore a bad frame and continue capturing.
                } finally {
                    image.close()
                }
            },
            captureHandler
        )

        virtualDisplay = projection?.createVirtualDisplay(
            "PengudokuScreen",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            captureHandler
        )
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val width = image.width
        val height = image.height

        if (width <= 0 || height <= 0) return null
        if (pixelStride < 4 || rowStride < width * pixelStride) return null

        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = y * rowStride + x * pixelStride

                if (offset + 3 >= bytes.size) return null

                val red = bytes[offset].toInt() and 0xFF
                val green = bytes[offset + 1].toInt() and 0xFF
                val blue = bytes[offset + 2].toInt() and 0xFF
                val alpha = bytes[offset + 3].toInt() and 0xFF

                pixels[y * width + x] =
                    (alpha shl 24) or
                    (red shl 16) or
                    (green shl 8) or
                    blue
            }
        }

        return Bitmap.createBitmap(
            pixels,
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengudoku Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Pengudoku Assistant")
                .setContentText("Screen capture is active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Pengudoku Assistant")
                .setContentText("Screen capture is active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build()
        }
    }

    override fun onDestroy() {
        imageReader?.setOnImageAvailableListener(null, null)

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        projection?.stop()
        projection = null

        pipeline?.hideOverlay()
        pipeline = null

        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

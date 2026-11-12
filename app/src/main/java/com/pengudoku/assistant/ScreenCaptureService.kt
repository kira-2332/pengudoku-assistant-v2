
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
import java.nio.ByteBuffer

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

    private lateinit var pipeline: PuzzlePipeline
    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null

    @Volatile
    private var processingImage = false

    private var lastProcessedTime = 0L

    override fun onCreate() {
        super.onCreate()
        pipeline = PuzzlePipeline(this)
        createNotificationChannel()

        workerThread = HandlerThread("PengudokuProcessor").apply {
            start()
        }
        workerHandler = Handler(workerThread!!.looper)
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

        val resultCode =
            intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0

        @Suppress("DEPRECATION")
        val resultData = intent?.getParcelableExtra<Intent>(
            EXTRA_RESULT_DATA
        )

        if (resultData == null || resultCode == 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
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
        } catch (_: Exception) {
            stopSelf()
        }

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

        if (width <= 0 || height <= 0) {
            stopSelf()
            return
        }

        imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            2
        )

        imageReader?.setOnImageAvailableListener(
            { reader ->
                val now = SystemClock.elapsedRealtime()

                if (now - lastProcessedTime < CAPTURE_INTERVAL_MS) {
                    reader.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }

                if (processingImage) {
                    reader.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }

                val image = reader.acquireLatestImage()
                    ?: return@setOnImageAvailableListener

                processingImage = true
                lastProcessedTime = now

                workerHandler?.post {
                    try {
                        val bitmap = imageToBitmap(image)

                        if (bitmap != null) {
                            try {
                                pipeline.process(bitmap)
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    } catch (_: Exception) {
                        // Continue after a failed frame.
                    } finally {
                        image.close()
                        processingImage = false
                    }
                } ?: run {
                    image.close()
                    processingImage = false
                }
            },
            null
        )

        virtualDisplay = projection?.createVirtualDisplay(
            "PengudokuScreen",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null

        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride

        if (pixelStride <= 0 || rowStride <= 0) return null

        val rowPadding =
            rowStride - pixelStride * image.width

        val paddedWidth =
            image.width + rowPadding / pixelStride

        if (paddedWidth <= 0) return null

        val paddedBitmap = Bitmap.createBitmap(
            paddedWidth,
            image.height,
            Bitmap.Config.ARGB_8888
        )

        return try {
            buffer.rewind()
            paddedBitmap.copyPixelsFromBuffer(buffer)

            val croppedBitmap = Bitmap.createBitmap(
                paddedBitmap,
                0,
                0,
                image.width,
                image.height
            )

            if (croppedBitmap !== paddedBitmap) {
                paddedBitmap.recycle()
            }

            croppedBitmap
        } catch (_: Exception) {
            if (!paddedBitmap.isRecycled) {
                paddedBitmap.recycle()
            }
            null
        }
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

        pipeline.hideOverlay()

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        projection?.stop()
        projection = null

        workerHandler?.removeCallbacksAndMessages(null)
        workerThread?.quitSafely()

        workerThread = null
        workerHandler = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

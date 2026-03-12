package com.gesturecontrol.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.gesturecontrol.GestureEventBus
import com.gesturecontrol.GestureSettings
import com.gesturecontrol.MainActivity
import com.gesturecontrol.R
import com.gesturecontrol.recognition.GestureRecognizerHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraForegroundService : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private var gestureRecognizerHelper: GestureRecognizerHelper? = null
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lastActioned = "None"
    private var lastActionTime = 0L

    companion object {
        const val CHANNEL_ID = "gesture_ctrl_channel"
        const val NOTIFICATION_ID = 1001
        const val TAG = "CameraFgService"
        const val ACTION_STOP = "ACTION_STOP"

        @Volatile var isRunning = false

        // Called from TestScreen to attach/detach live preview
        var previewView: PreviewView? = null
            set(value) {
                field = value
                instance?.rebindCamera()
            }

        private var instance: CameraForegroundService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        cameraExecutor = Executors.newSingleThreadExecutor()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundWithNotification()
        setupGestureRecognizer()
        bindCamera()
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        gestureRecognizerHelper?.close()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        isRunning = false
        instance = null
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "GestureControl", NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.notification_text)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun startForegroundWithNotification() {
        val openApp = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stopSvc = PendingIntent.getService(this, 1,
            Intent(this, CameraForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_gesture)
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_gesture, getString(R.string.stop_service), stopSvc)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun setupGestureRecognizer() {
        gestureRecognizerHelper = GestureRecognizerHelper(
            context = this,
            onResult = ::handleGestureResult,
            onError = { Log.e(TAG, "Gesture error: ${it.message}") }
        )
    }

    private fun handleGestureResult(gestureName: String, confidence: Float) {
        val threshold = GestureSettings.confidenceThreshold
        val now = System.currentTimeMillis()
        val cooldown = GestureSettings.actionCooldownMs

        val action: String? = when {
            gestureName == "Thumb_Up" && GestureSettings.thumbsUpEnabled && confidence >= threshold ->
                GestureAccessibilityService.ACTION_DOUBLE_TAP
            gestureName == "Pointing_Up" && GestureSettings.pointingUpEnabled && confidence >= threshold ->
                GestureAccessibilityService.ACTION_SWIPE_UP
            else -> null
        }

        val triggered = if (action != null && (now - lastActionTime) > cooldown && gestureName != lastActioned) {
            lastActionTime = now
            lastActioned = gestureName
            GestureAccessibilityService.instance?.performGestureAction(action)
            action
        } else null

        if (gestureName == "None") lastActioned = "None"

        GestureEventBus.emit(
            GestureEventBus.GestureResult(
                gestureName = gestureName,
                confidence = confidence,
                actionTriggered = triggered
            )
        )
    }

    private fun bindCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            cameraProvider = future.get()
            rebindCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    internal fun rebindCamera() {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().apply {
                    setAnalyzer(cameraExecutor) { imageProxy ->
                        gestureRecognizerHelper?.recognizeAsync(imageProxy)
                            ?: imageProxy.close()
                    }
                }

            val pView = previewView
            if (pView != null) {
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(pView.surfaceProvider)
                }
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis
                )
            } else {
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    analysis
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera bind failed: ${e.message}")
        }
    }
}

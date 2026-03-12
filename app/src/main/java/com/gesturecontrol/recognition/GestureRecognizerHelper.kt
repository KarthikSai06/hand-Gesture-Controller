package com.gesturecontrol.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import android.util.Log

class GestureRecognizerHelper(
    private val context: Context,
    private val onResult: (String, Float) -> Unit,
    private val onError: (Exception) -> Unit
) {
    private var gestureRecognizer: GestureRecognizer? = null

    companion object {
        const val MODEL_PATH = "gesture_recognizer.task"
        const val TAG = "GestureRecognizerHelper"
    }

    init {
        setupGestureRecognizer()
    }

    private fun setupGestureRecognizer() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_PATH)
                .build()

            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumHands(1)
                .setResultListener { result, _ -> handleResult(result) }
                .setErrorListener { error -> onError(error) }
                .build()

            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun recognizeAsync(imageProxy: ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        
        imageProxy.planes[0].buffer.rewind()
        bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
        
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            postScale(-1f, 1f)
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, false
        )
        
        imageProxy.close()

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        gestureRecognizer?.recognizeAsync(mpImage, SystemClock.uptimeMillis())
    }

    private fun handleResult(result: GestureRecognizerResult) {
        if (result.gestures().isEmpty() || result.gestures()[0].isEmpty()) {
            onResult("None", 0f)
            return
        }
        val top = result.gestures()[0][0]
        onResult(top.categoryName(), top.score())
    }

    fun close() {
        gestureRecognizer?.close()
        gestureRecognizer = null
    }
}

package com.gesturecontrol.recognition

import android.content.Context
import android.util.Log
import com.gesturecontrol.GestureEventBus
import org.json.JSONArray
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Loads a custom TFLite gesture classifier trained from the app's Recorder screen.
 *
 * Required in assets/:
 *   • gesture_custom.tflite   — MLP model  (input 1×63, output 1×N)
 *
 * Optional in assets/:
 *   • gesture_labels.json     — ["label0","label1",...]
 *     If absent, classes are named "gesture_0", "gesture_1", etc.
 *
 * FALSE-POSITIVE GUARD
 * ---------------------
 * Two conditions must BOTH be true before a prediction is accepted:
 *   1. Top-class confidence ≥ MIN_CONFIDENCE (90%)
 *   2. Margin between top-1 and top-2 ≥ MIN_MARGIN (35%)
 *      → prevents misfires on a single-class model or when all probs are similar.
 *
 * **Training advice**: Always include a "No_Gesture" / "None" class in your dataset
 * so the model has seen negative examples. Otherwise softmax will always pick
 * whatever class it was trained on, even for random input.
 */
class CustomGestureClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String>       = emptyList()
    private var numClasses: Int            = 0

    val isAvailable: Boolean get() = interpreter != null && numClasses > 0

    companion object {
        const val MODEL_FILE      = "gesture_custom.tflite"
        const val LABELS_FILE     = "gesture_labels.json"
        const val TAG             = "CustomGestureClassifier"

        /** Top-class softmax probability must exceed this. */
        const val MIN_CONFIDENCE  = 0.90f

        /**
         * Gap between top-1 and top-2 probability.
         * Prevents misfires when all classes score similarly (model is uncertain)
         * and prevents single-class models from always firing at 100%.
         * Requires at least 2 classes — single-class models are always disabled.
         */
        const val MIN_MARGIN      = 0.35f
    }

    init { load() }

    private fun load() {
        try {
            val assetList = context.assets.list("") ?: emptyArray()
            Log.d(TAG, "Assets: ${assetList.toList()}")

            if (!assetList.contains(MODEL_FILE)) {
                Log.w(TAG, "❌ '$MODEL_FILE' not found. Classifier disabled.")
                return
            }

            labels = if (assetList.contains(LABELS_FILE)) {
                val json = context.assets.open(LABELS_FILE).bufferedReader().readText()
                val arr  = JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }
                    .also { Log.i(TAG, "Labels from JSON: $it") }
            } else {
                Log.w(TAG, "⚠️ No labels JSON — using generic names.")
                emptyList()
            }

            val fd      = context.assets.openFd(MODEL_FILE)
            val mBuffer = FileInputStream(fd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
            )
            interpreter = Interpreter(mBuffer)

            val outShape = interpreter!!.getOutputTensor(0).shape()
            numClasses   = if (outShape.size >= 2) outShape[1] else outShape[0]

            if (numClasses < 2) {
                Log.e(TAG, "❌ Model has only $numClasses output class(es). " +
                        "You MUST train with at least 2 classes (including a 'No_Gesture' class). " +
                        "Classifier disabled to prevent constant false positives.")
                interpreter?.close(); interpreter = null
                return
            }

            if (labels.isEmpty()) labels = (0 until numClasses).map { "gesture_$it" }
            if (labels.size != numClasses) labels = (0 until numClasses).map { labels.getOrElse(it) { "gesture_$it" } }

            Log.i(TAG, "✅ Ready — classes: $labels | min_conf=${MIN_CONFIDENCE*100}% | min_margin=${MIN_MARGIN*100}%")

        } catch (e: Exception) {
            Log.e(TAG, "Load failed: ${e.message}", e)
            interpreter = null
        }
    }

    /**
     * Run inference. Returns a result ONLY when BOTH guards pass:
     *   confidence ≥ 90%  AND  margin over second-best ≥ 35%.
     *
     * Returns null for anything ambiguous or if the model isn't loaded.
     */
    fun predict(landmarks: List<GestureEventBus.Landmark>): Pair<String, Float>? {
        val interp = interpreter ?: return null
        if (landmarks.size != 21) return null

        val inputBuf = ByteBuffer.allocateDirect(63 * Float.SIZE_BYTES).apply {
            order(ByteOrder.nativeOrder())
            landmarks.forEach { putFloat(it.x); putFloat(it.y); putFloat(it.z) }
            rewind()
        }
        val outputBuf = Array(1) { FloatArray(numClasses) }

        return try {
            interp.run(inputBuf, outputBuf)
            val probs = outputBuf[0]

            // Sort descending to find top-1 and top-2
            val sorted  = probs.mapIndexed { i, p -> i to p }.sortedByDescending { it.second }
            val topIdx  = sorted[0].first
            val topConf = sorted[0].second
            val secConf = if (sorted.size > 1) sorted[1].second else 0f
            val margin  = topConf - secConf

            val probStr = probs.mapIndexed { i, p ->
                "${labels[i]}=${"%.0f".format(p * 100)}%"
            }.joinToString(" | ")
            Log.d(TAG, "Probs: $probStr | margin=${"%.0f".format(margin*100)}%")

            when {
                topConf < MIN_CONFIDENCE ->
                    null.also { Log.v(TAG, "Rejected: confidence ${"%.0f".format(topConf*100)}% < ${MIN_CONFIDENCE*100}%") }
                margin < MIN_MARGIN ->
                    null.also { Log.v(TAG, "Rejected: margin ${"%.0f".format(margin*100)}% < ${MIN_MARGIN*100}% (uncertain)") }
                else ->
                    Pair(labels[topIdx], topConf)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}", e)
            null
        }
    }

    fun close() { interpreter?.close(); interpreter = null }
}

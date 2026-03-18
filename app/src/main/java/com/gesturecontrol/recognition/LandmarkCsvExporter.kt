package com.gesturecontrol.recognition

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gesturecontrol.GestureEventBus
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes gesture landmark training data to a CSV file in the user's Downloads folder.
 *
 * CSV format:
 *   gesture,x0,y0,z0,x1,y1,z1,...,x20,y20,z20
 *   (1 label column + 63 landmark feature columns = 64 columns total)
 *
 * Uses MediaStore (scoped storage) on API 29+ — no WRITE_EXTERNAL_STORAGE permission needed.
 * Falls back to app-specific external storage on API 28.
 */
object LandmarkCsvExporter {

    /**
     * Converts a list of 21 [GestureEventBus.Landmark]s to a flat list of 63 floats
     * ordered as [x0,y0,z0, x1,y1,z1, ...].
     */
    fun landmarksToFeatures(landmarks: List<GestureEventBus.Landmark>): List<Float> =
        landmarks.flatMap { listOf(it.x, it.y, it.z) }

    /**
     * Exports [samples] to a timestamped CSV file named after [gestureName].
     *
     * @return The [Uri] of the created file, or null on failure.
     */
    fun export(
        context: Context,
        gestureName: String,
        samples: List<List<Float>>
    ): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "${gestureName}_${timestamp}.csv"
        val csvContent = buildCsv(gestureName, samples)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportViaMediaStore(context, fileName, csvContent)
            } else {
                exportToExternalFile(context, fileName, csvContent)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun buildCsv(gestureName: String, samples: List<List<Float>>): String {
        val header = buildString {
            append("gesture")
            for (i in 0 until 21) append(",x$i,y$i,z$i")
            append("\n")
        }
        val rows = samples.joinToString("\n") { features ->
            gestureName + "," + features.joinToString(",") { "%.6f".format(it) }
        }
        return header + rows + "\n"
    }

    private fun exportViaMediaStore(context: Context, fileName: String, content: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        return uri
    }

    @Suppress("DEPRECATION")
    private fun exportToExternalFile(context: Context, fileName: String, content: String): Uri? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(content.toByteArray()) }
        return Uri.fromFile(file)
    }
}

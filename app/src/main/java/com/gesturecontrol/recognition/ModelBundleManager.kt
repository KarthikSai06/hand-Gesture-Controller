package com.gesturecontrol.recognition

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Bundles gesture_custom.tflite + gesture_labels.json + bundle_meta.json into
 * a single .gcbundle (zip) for easy sharing / import.
 *
 * Export flow:
 *   1. Read tflite + labels from assets/ (or internal files/models/)
 *   2. Generate bundle_meta.json with timestamp + gesture count
 *   3. Zip all 3 → cache/gesture_bundle_TIMESTAMP.gcbundle
 *   4. Return FileProvider URI for sharing
 *
 * Import flow:
 *   1. Receive content URI of a .gcbundle
 *   2. Unzip → files/models/ directory
 *   3. Return true on success (caller should reload CustomGestureClassifier)
 */
object ModelBundleManager {

    private const val TAG            = "ModelBundleManager"
    private const val TFLITE_NAME    = "gesture_custom.tflite"
    private const val LABELS_NAME    = "gesture_labels.json"
    private const val META_NAME      = "bundle_meta.json"
    private const val FILE_PROVIDER  = "com.gesturecontrol.fileprovider"

    // Internal storage dir for imported models
    private fun modelsDir(context: Context): File =
        File(context.filesDir, "models").also { it.mkdirs() }

    // ── Export ─────────────────────────────────────────────────────────────

    /**
     * Creates a .gcbundle zip in cache dir and returns a shareable FileProvider URI.
     * Returns null if neither assets nor internal models dir contains the tflite file.
     */
    fun exportBundle(context: Context): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val outFile   = File(context.cacheDir, "gesture_bundle_$timestamp.gcbundle")

        return try {
            val assetList  = context.assets.list("") ?: emptyArray()
            val internalDir = modelsDir(context)

            // Read tflite bytes
            val tfliteBytes: ByteArray = when {
                File(internalDir, TFLITE_NAME).exists() ->
                    File(internalDir, TFLITE_NAME).readBytes()
                assetList.contains(TFLITE_NAME) ->
                    context.assets.open(TFLITE_NAME).readBytes()
                else -> {
                    Log.w(TAG, "No tflite model found — cannot export bundle")
                    return null
                }
            }

            // Read labels bytes (optional — create placeholder if missing)
            val labelsBytes: ByteArray = when {
                File(internalDir, LABELS_NAME).exists() ->
                    File(internalDir, LABELS_NAME).readBytes()
                assetList.contains(LABELS_NAME) ->
                    context.assets.open(LABELS_NAME).readBytes()
                else ->
                    "[]".toByteArray()
            }

            // Count gestures from labels
            val gestureCount = try {
                val arr = org.json.JSONArray(String(labelsBytes))
                arr.length()
            } catch (_: Exception) { 0 }

            // Metadata JSON
            val meta = JSONObject().apply {
                put("app_version", "1.0")
                put("gesture_count", gestureCount)
                put("created", timestamp)
                put("format", "gcbundle/1.0")
            }
            val metaBytes = meta.toString(2).toByteArray()

            // Write zip
            ZipOutputStream(FileOutputStream(outFile).buffered()).use { zip ->
                zip.putEntry(TFLITE_NAME, tfliteBytes)
                zip.putEntry(LABELS_NAME, labelsBytes)
                zip.putEntry(META_NAME,   metaBytes)
            }

            Log.i(TAG, "Bundle exported: ${outFile.absolutePath} (${outFile.length()} bytes)")

            FileProvider.getUriForFile(context, FILE_PROVIDER, outFile)
        } catch (e: Exception) {
            Log.e(TAG, "Export failed: ${e.message}", e)
            null
        }
    }

    // ── Import ─────────────────────────────────────────────────────────────

    /**
     * Imports a .gcbundle from [uri] into the app's internal files/models dir.
     * Returns true on success. Caller is responsible for reloading the classifier.
     */
    fun importBundle(context: Context, uri: Uri): Boolean {
        return try {
            val resolver = context.contentResolver
            val destDir  = modelsDir(context)

            resolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = File(entry.name).name // strip any path traversal
                        if (name in listOf(TFLITE_NAME, LABELS_NAME, META_NAME)) {
                            val dest = File(destDir, name)
                            FileOutputStream(dest).use { out -> zip.copyTo(out) }
                            Log.i(TAG, "Imported: ${dest.absolutePath}")
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            // Verify tflite was actually written
            val ok = File(destDir, TFLITE_NAME).exists()
            Log.i(TAG, if (ok) "Import successful" else "Import failed — tflite not found in bundle")
            ok
        } catch (e: Exception) {
            Log.e(TAG, "Import failed: ${e.message}", e)
            false
        }
    }

    /** Returns true if a model is available (assets or internal). */
    fun isModelAvailable(context: Context): Boolean {
        val internalTflite = File(modelsDir(context), TFLITE_NAME)
        if (internalTflite.exists()) return true
        return try {
            context.assets.list("")?.contains(TFLITE_NAME) == true
        } catch (_: Exception) { false }
    }

    // ── Zip helper extension ──────────────────────────────────────────────

    private fun ZipOutputStream.putEntry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }
}

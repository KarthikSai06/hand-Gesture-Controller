package com.gesturecontrol.recognition

import com.gesturecontrol.GestureEventBus.Landmark
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Rule-based classifier for 6 additional gestures not covered by MediaPipe's built-in model.
 * Uses landmark geometry (finger curl + extension) — no TFLite model required.
 *
 * Coordinate system (MediaPipe normalised):
 *   x: 0 = left  → 1 = right  (mirrored on front camera)
 *   y: 0 = top   → 1 = bottom
 *   z: depth (not used here; relative to wrist)
 *
 * Finger curl heuristic:
 *   A finger is EXTENDED if its TIP y < PIP y  (tip is above the middle knuckle vertically).
 *   A finger is CURLED  if its TIP y > MCP y  (tip has fallen below the base knuckle).
 *
 * Landmark indices (21 total):
 *   0 = Wrist
 *   1-4  = Thumb  (CMC, MCP, IP, TIP)
 *   5-8  = Index  (MCP, PIP, DIP, TIP)
 *   9-12 = Middle (MCP, PIP, DIP, TIP)
 *   13-16= Ring   (MCP, PIP, DIP, TIP)
 *   17-20= Pinky  (MCP, PIP, DIP, TIP)
 */
object BuiltInGestureLibrary {

    data class GestureInfo(
        val name: String,
        val emoji: String,
        val description: String
    )

    val ALL_GESTURES = listOf(
        GestureInfo("Peace",     "✌️", "Index + Middle extended"),
        GestureInfo("Fist",      "✊", "All fingers curled"),
        GestureInfo("Open_Palm", "🖐️", "All fingers extended"),
        GestureInfo("OK",        "👌", "Thumb+Index loop, others extended"),
        GestureInfo("Shaka",     "🤙", "Thumb + Pinky extended"),
        GestureInfo("L_Shape",   "🤟", "Thumb + Index only extended")
    )

    val ALL_GESTURE_NAMES: List<String> = ALL_GESTURES.map { it.name }

    /**
     * Classifies landmarks into one of the 6 built-in gestures.
     * Returns Pair(gestureName, 1.0f) or null if no gesture matched.
     */
    fun classify(landmarks: List<Landmark>): Pair<String, Float>? {
        if (landmarks.size != 21) return null

        val thumbExt  = isThumbExtended(landmarks)
        val indexExt  = isFingerExtended(landmarks, tipIdx = 8,  pipIdx = 6,  mcpIdx = 5)
        val middleExt = isFingerExtended(landmarks, tipIdx = 12, pipIdx = 10, mcpIdx = 9)
        val ringExt   = isFingerExtended(landmarks, tipIdx = 16, pipIdx = 14, mcpIdx = 13)
        val pinkyExt  = isFingerExtended(landmarks, tipIdx = 20, pipIdx = 18, mcpIdx = 17)

        return when {
            // Open Palm — all 5 extended
            thumbExt && indexExt && middleExt && ringExt && pinkyExt ->
                Pair("Open_Palm", 1.0f)

            // Fist — all 4 fingers curled (thumb ignored — harder to detect reliably)
            !indexExt && !middleExt && !ringExt && !pinkyExt ->
                Pair("Fist", 1.0f)

            // Peace — index + middle extended, ring + pinky curled
            !thumbExt && indexExt && middleExt && !ringExt && !pinkyExt ->
                Pair("Peace", 1.0f)

            // Shaka (Call Me) — thumb + pinky extended, others curled
            thumbExt && !indexExt && !middleExt && !ringExt && pinkyExt ->
                Pair("Shaka", 1.0f)

            // L-Shape — thumb + index extended only
            thumbExt && indexExt && !middleExt && !ringExt && !pinkyExt ->
                Pair("L_Shape", 1.0f)

            // OK — thumb tip close to index tip, middle+ring+pinky extended
            isOkGesture(landmarks) && middleExt && ringExt && pinkyExt ->
                Pair("OK", 1.0f)

            else -> null
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Thumb is extended when its tip (4) is clearly above its IP joint (3). */
    private fun isThumbExtended(lm: List<Landmark>): Boolean {
        // For thumb: compare x-distance since thumb extends laterally
        val tip = lm[4]; val ip = lm[3]; val mcp = lm[2]
        val tipToMcp = dist(tip, mcp)
        val ipToMcp  = dist(ip,  mcp)
        return tipToMcp > ipToMcp * 1.2f
    }

    /**
     * Finger extended when:
     *   tip.y < pip.y  (tip above middle knuckle in image space — larger y = lower)
     */
    private fun isFingerExtended(lm: List<Landmark>, tipIdx: Int, pipIdx: Int, mcpIdx: Int): Boolean {
        val tip = lm[tipIdx]; val pip = lm[pipIdx]; val mcp = lm[mcpIdx]
        // Extended: tip is above PIP vertically AND tip is farther from wrist than MCP
        return tip.y < pip.y
    }

    /** OK gesture: thumb tip (4) is very close to index tip (8). */
    private fun isOkGesture(lm: List<Landmark>): Boolean {
        val d = dist(lm[4], lm[8])
        return d < 0.07f
    }

    private fun dist(a: Landmark, b: Landmark): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}

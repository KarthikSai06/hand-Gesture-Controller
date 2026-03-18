package com.gesturecontrol.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.gesturecontrol.GestureEventBus

/**
 * MediaPipe hand topology — the 21 landmark indices that connect to form
 * the hand skeleton. See: https://developers.google.com/mediapipe/solutions/vision/hand_landmarker
 */
private val HAND_CONNECTIONS = listOf(
    // Wrist → palm base
    0 to 1,  0 to 5,  0 to 17,
    // Thumb
    1 to 2,  2 to 3,  3 to 4,
    // Index
    5 to 6,  6 to 7,  7 to 8,
    // Middle
    9 to 10, 10 to 11, 11 to 12,
    // Ring
    13 to 14, 14 to 15, 15 to 16,
    // Pinky
    17 to 18, 18 to 19, 19 to 20,
    // Palm knuckles
    5 to 9,  9 to 13, 13 to 17
)

// Fingertip indices (draw with a brighter colour)
private val FINGERTIPS = setOf(4, 8, 12, 16, 20)

/**
 * Transparent Compose Canvas that draws the 21-point hand skeleton overlay.
 *
 * [landmarks] are MediaPipe normalised coordinates (0..1), so we multiply
 * by the Canvas width/height to get pixel positions.
 */
@Composable
fun LandmarkOverlay(
    landmarks: List<GestureEventBus.Landmark>,
    modifier: Modifier = Modifier,
    boneColor: Color = Color(0xFF00E5FF).copy(alpha = 0.85f),      // cyan
    jointColor: Color = Color(0xFF00E5FF),
    tipColor: Color = Color(0xFFAB47BC),                            // purple for fingertips
    boneWidth: Float = 3f,
    dotRadius: Float = 7f
) {
    Canvas(modifier = modifier) {
        if (landmarks.size < 21) return@Canvas

        // Helper: landmark index → screen pixel Offset
        fun lm(i: Int) = Offset(landmarks[i].x * size.width, landmarks[i].y * size.height)

        // Draw bone lines
        for ((a, b) in HAND_CONNECTIONS) {
            drawLine(
                color = boneColor,
                start = lm(a),
                end = lm(b),
                strokeWidth = boneWidth,
                cap = StrokeCap.Round
            )
        }

        // Draw joint dots
        for (i in landmarks.indices) {
            val isTip = i in FINGERTIPS
            drawCircle(
                color = if (isTip) tipColor else jointColor,
                radius = if (isTip) dotRadius * 1.4f else dotRadius,
                center = lm(i)
            )
            // White inner highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = if (isTip) dotRadius * 0.55f else dotRadius * 0.4f,
                center = lm(i)
            )
        }
    }
}

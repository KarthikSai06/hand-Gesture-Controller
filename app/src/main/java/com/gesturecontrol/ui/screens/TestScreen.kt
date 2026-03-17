package com.gesturecontrol.ui.screens

import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.gesturecontrol.GestureEventBus
import com.gesturecontrol.recognition.GestureRecognizerHelper
import com.gesturecontrol.service.CameraForegroundService
import com.gesturecontrol.ui.LandmarkOverlay
import com.gesturecontrol.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.Executors

@Composable
fun TestScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var result by remember { mutableStateOf(GestureEventBus.GestureResult("None", 0f)) }
    var flashAction by remember { mutableStateOf<String?>(null) }
    var showOverlay by remember { mutableStateOf(true) }
    val serviceRunning = CameraForegroundService.isRunning

    var localHelper by remember { mutableStateOf<GestureRecognizerHelper?>(null) }
    var localPreviewView by remember { mutableStateOf<PreviewView?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        GestureEventBus.gestureFlow.collectLatest {
            result = it
            it.actionTriggered?.let { a -> flashAction = a; delay(1200); flashAction = null }
        }
    }

    DisposableEffect(serviceRunning) {
        if (serviceRunning) {
            localPreviewView?.let { CameraForegroundService.previewView = it }
        }
        onDispose {
            if (serviceRunning) CameraForegroundService.previewView = null
            localHelper?.close()
            localHelper = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NavyDeep)) {

        // ── Camera preview ──────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    localPreviewView = this

                    if (serviceRunning) {
                        CameraForegroundService.previewView = this
                    } else {
                        val helper = GestureRecognizerHelper(
                            context = ctx,
                            onResult = { name, conf ->
                                GestureEventBus.emit(GestureEventBus.GestureResult(name, conf))
                            },
                            onError = {},
                            onLandmarks = { lms ->
                                // Merge landmarks into the latest emitted result
                                val latest = GestureEventBus.gestureFlow.replayCache.lastOrNull()
                                    ?: return@GestureRecognizerHelper
                                if (latest.landmarks.isEmpty() && lms.isNotEmpty()) {
                                    GestureEventBus.emit(latest.copy(landmarks = lms))
                                }
                            }
                        )
                        localHelper = helper

                        val future = ProcessCameraProvider.getInstance(ctx)
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build()
                                .also { it.setSurfaceProvider(this.surfaceProvider) }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .build().apply {
                                    setAnalyzer(cameraExecutor) { proxy ->
                                        helper.recognizeAsync(proxy)
                                    }
                                }
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview, analysis
                                )
                            } catch (e: Exception) { e.printStackTrace() }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Landmark skeleton overlay ────────────────────────────────────────
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            LandmarkOverlay(
                landmarks = result.landmarks,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Gradient overlays ────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(NavyDeep.copy(alpha = 0.9f), Color.Transparent)))
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, NavyDeep.copy(alpha = 0.95f))))
        )

        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Test Mode", color = TextPrimary, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            // Overlay toggle button
            IconButton(
                onClick = { showOverlay = !showOverlay },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (showOverlay) CyanBright.copy(0.15f) else NavyCard.copy(0.7f)
                    )
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = if (showOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle landmark overlay",
                    tint = if (showOverlay) CyanBright else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (serviceRunning) GreenOk.copy(0.15f) else Amber.copy(0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    if (serviceRunning) "● Service Active" else "● Standalone",
                    color = if (serviceRunning) GreenOk else Amber,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // ── Action flash overlay ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = flashAction != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when (flashAction) {
                            "DOUBLE_TAP"  -> CyanBright.copy(0.85f)
                            "SWIPE_UP"    -> PurpleBright.copy(0.85f)
                            "SWIPE_DOWN"  -> GreenOk.copy(0.85f)
                            else          -> CyanBright.copy(0.85f)
                        }
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    when (flashAction) {
                        "DOUBLE_TAP" -> "🎯  Double Tap!"
                        "SWIPE_UP"   -> "⬆️  Swipe Up!"
                        "SWIPE_DOWN" -> "⬇️  Custom Gesture!"
                        else         -> "✋  Gesture!"
                    },
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Bottom info panel ────────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Landmark count badge
            if (result.landmarks.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyanBright.copy(0.1f))
                            .border(1.dp, CyanBright.copy(0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "✋ ${result.landmarks.size} keypoints",
                            color = CyanBright.copy(0.9f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Gesture name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(gestureEmoji(result.gestureName), fontSize = 36.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        result.gestureName.replace("_", " ").ifEmpty { "No gesture" },
                        color = if (result.gestureName == "None") TextMuted else CyanBright,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Detected gesture", color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
                Text("${(result.confidence * 100).toInt()}%",
                    color = TextPrimary, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold)
            }

            // Confidence bar
            val animConf by animateFloatAsState(result.confidence, label = "conf")
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(3.dp)).background(NavyCard)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(animConf).fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(CyanMid, CyanBright)))
                )
            }

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendChip("👍 = Double Tap", CyanBright)
                LegendChip("☝️ = Swipe Up",  PurpleBright)
                LegendChip("🤖 = Swipe Down", GreenOk)
            }
        }
    }
}

private fun gestureEmoji(name: String): String = when (name) {
    "Thumb_Up"    -> "👍"
    "Thumb_Down"  -> "👎"
    "Pointing_Up" -> "☝️"
    "Victory"     -> "✌️"
    "Open_Palm"   -> "🖐️"
    "Closed_Fist" -> "✊"
    "ILoveYou"    -> "🤟"
    else          -> "❓"
}

@Composable
private fun LegendChip(text: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

package com.gesturecontrol.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gesturecontrol.GestureEventBus
import com.gesturecontrol.recognition.LandmarkCsvExporter
import com.gesturecontrol.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

private const val TARGET_SAMPLES = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureRecorderScreen() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ── State ────────────────────────────────────────────────────────────────
    var gestureName by remember { mutableStateOf("") }
    val samples = remember { mutableStateListOf<List<Float>>() }
    var latestLandmarks by remember { mutableStateOf<List<GestureEventBus.Landmark>>(emptyList()) }
    var exportedPath by remember { mutableStateOf<String?>(null) }

    // Collect live landmarks from the event bus
    LaunchedEffect(Unit) {
        GestureEventBus.gestureFlow.collectLatest { result ->
            if (result.landmarks.isNotEmpty()) latestLandmarks = result.landmarks
        }
    }

    // Animated progress ring
    val progress = if (TARGET_SAMPLES > 0) samples.size.toFloat() / TARGET_SAMPLES else 0f
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    val isNameEmpty = gestureName.isBlank()
    val hasEnoughSamples = samples.size >= 1
    val isDone = samples.size >= TARGET_SAMPLES

    // ── Layout ───────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = NavyDeep,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))

            // ── Title ────────────────────────────────────────────────────────
            Text(
                "Gesture Recorder",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Collect training data • Export to CSV",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(24.dp))

            // ── Gesture name field ───────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gesture Label", color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = gestureName,
                        onValueChange = { gestureName = it },
                        placeholder = { Text("e.g. Thumb_Up", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanBright,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyanBright
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Label, null, tint = CyanMid)
                        }
                    )
                    // Quick-fill chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Thumb_Up", "Pointing_Up", "Open_Palm", "Closed_Fist").forEach { label ->
                            FilterChip(
                                selected = gestureName == label,
                                onClick = { gestureName = label },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanDim,
                                    selectedLabelColor = CyanBright,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Progress ring ────────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                val ringColor = when {
                    isDone -> GreenOk
                    else   -> CyanBright
                }
                // Ring drawn with Canvas
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .drawBehind {
                            val strokeW = 12.dp.toPx()
                            val r = (size.minDimension - strokeW) / 2
                            val cx = size.width / 2
                            val cy = size.height / 2
                            // Track
                            drawCircle(NavyCard, radius = r, center = Offset(cx, cy),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW))
                            // Progress arc
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = animProgress * 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    strokeW, cap = StrokeCap.Round)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${samples.size}",
                            color = if (isDone) GreenOk else CyanBright,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "/ $TARGET_SAMPLES samples",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (isDone) {
                            Text("✓ Ready to Export!", color = GreenOk,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Live landmark status
            val hasHand = latestLandmarks.size == 21
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(if (hasHand) GreenOk else RedAlert)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (hasHand) "Hand detected — ready to capture" else "No hand detected — show hand to camera",
                    color = if (hasHand) GreenOk else RedAlert,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Capture button ───────────────────────────────────────────────
            val canCapture = !isNameEmpty && hasHand && !isDone
            Button(
                onClick = {
                    val features = LandmarkCsvExporter.landmarksToFeatures(latestLandmarks)
                    samples.add(features)
                },
                enabled = canCapture,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanMid,
                    disabledContainerColor = NavyCard
                )
            ) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        isDone        -> "Max samples reached"
                        isNameEmpty   -> "Enter gesture name first"
                        !hasHand      -> "Show hand to camera"
                        else          -> "📸  Capture Sample"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Export + Reset ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reset
                OutlinedButton(
                    onClick = { samples.clear(); exportedPath = null },
                    enabled = samples.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (samples.isNotEmpty()) RedAlert.copy(0.5f) else NavyBorder)
                ) {
                    Icon(Icons.Default.Delete, null, tint = RedAlert.copy(if (samples.isNotEmpty()) 1f else 0.3f),
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset", color = if (samples.isNotEmpty()) RedAlert else TextMuted)
                }

                // Export
                Button(
                    onClick = {
                        val uri = LandmarkCsvExporter.export(context, gestureName, samples.toList())
                        exportedPath = uri?.toString() ?: "Export failed"
                    },
                    enabled = hasEnoughSamples && !isNameEmpty,
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenOk.copy(0.85f),
                        disabledContainerColor = NavyCard
                    )
                ) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("📤  Export CSV", fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Export result card ────────────────────────────────────────────
            exportedPath?.let { path ->
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = GreenOk.copy(0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GreenOk.copy(0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("✅ Exported!", color = GreenOk, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(path, color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Start)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "File saved to Downloads. Open it in a file manager or transfer to PC for Python training.",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // ── Instructions card ─────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("How to use", color = CyanBright,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    listOf(
                        "1️⃣  Enter or select a gesture label",
                        "2️⃣  Show that gesture to the front camera",
                        "3️⃣  Tap 📸 Capture Sample up to 30 times",
                        "4️⃣  Repeat steps 1–3 for each gesture class",
                        "5️⃣  Tap 📤 Export CSV to save your dataset",
                        "6️⃣  Train a classifier in Python (sklearn / TFLite) and drop your .tflite back in assets/"
                    ).forEach { step ->
                        Text(step, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

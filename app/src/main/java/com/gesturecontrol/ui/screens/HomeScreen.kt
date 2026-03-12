package com.gesturecontrol.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.gesturecontrol.GestureEventBus
import com.gesturecontrol.service.CameraForegroundService
import com.gesturecontrol.service.GestureAccessibilityService
import com.gesturecontrol.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    var hasCam by remember { mutableStateOf(false) }
    var hasA11y by remember { mutableStateOf(false) }
    var hasNotif by remember { mutableStateOf(false) }
    var serviceRunning by remember { mutableStateOf(CameraForegroundService.isRunning) }
    var lastResult by remember {
        mutableStateOf(GestureEventBus.GestureResult("None", 0f))
    }

    LaunchedEffect(Unit) {
        while (true) {
            hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PermissionChecker.PERMISSION_GRANTED
            hasA11y = GestureAccessibilityService.instance != null
            hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PermissionChecker.PERMISSION_GRANTED
            } else true
            serviceRunning = CameraForegroundService.isRunning
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        GestureEventBus.gestureFlow.collectLatest { lastResult = it }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, NavyDark, Color(0xFF0B1628))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(CyanMid, PurpleBright))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PanTool, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("GestureControl", style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Hands-free device control", style = MaterialTheme.typography.labelSmall,
                        color = CyanBright)
                }
                Spacer(Modifier.weight(1f))
                if (serviceRunning) {
                    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 0.85f, targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p"
                    )
                    Box(
                        modifier = Modifier.size(10.dp).scale(pulse)
                            .background(GreenOk, CircleShape)
                    )
                }
            }

            AnimatedContent(targetState = lastResult.gestureName, label = "badge") { name ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(gestureEmoji(name), fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Detecting", color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall)
                            Text(
                                if (name == "None") "No gesture" else name.replace("_", " "),
                                color = if (name == "None") TextMuted else CyanBright,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (lastResult.confidence > 0f) {
                            Text(
                                "${(lastResult.confidence * 100).toInt()}%",
                                color = GreenOk, style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Text("Permissions", color = TextSecondary,
                style = MaterialTheme.typography.labelLarge)
            PermissionCard("Camera", "Required for gesture detection", Icons.Default.CameraAlt, hasCam) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
            PermissionCard("Accessibility Service", "Required for system-wide gestures",
                Icons.Default.Accessibility, hasA11y) {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            PermissionCard("Notifications", "Required for background notification",
                Icons.Default.Notifications, hasNotif) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                }
            }

            Spacer(Modifier.height(4.dp))
            ServiceToggleButton(serviceRunning, allGranted = hasCam && hasA11y) {
                val svcIntent = Intent(context, CameraForegroundService::class.java)
                if (serviceRunning) {
                    svcIntent.action = CameraForegroundService.ACTION_STOP
                    context.startService(svcIntent)
                } else {
                    ContextCompat.startForegroundService(context, svcIntent)
                }
            }

            Text("Gesture Map", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
            GestureGuideCard("👍", "Thumbs Up", "Double Tap", "Tap twice at screen center")
            GestureGuideCard("☝️", "Pointing Up", "Swipe Up", "Swipe from bottom to top")

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    title: String, subtitle: String, icon: ImageVector,
    granted: Boolean, onFix: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) NavyCard else Color(0xFF1A0E0E)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null,
                tint = if (granted) CyanBright else RedAlert,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            if (granted) {
                Icon(Icons.Default.CheckCircle, null, tint = GreenOk, modifier = Modifier.size(20.dp))
            } else {
                TextButton(onClick = onFix, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Fix", color = CyanBright, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ServiceToggleButton(running: Boolean, allGranted: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (running) 1f else 0.97f, label = "btn")
    Button(
        onClick = onClick,
        enabled = allGranted || running,
        modifier = Modifier.fillMaxWidth().height(58.dp).scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (running) Color(0xFF1A0E0E) else CyanBright,
            contentColor   = if (running) RedAlert else NavyDeep
        )
    ) {
        Icon(
            if (running) Icons.Default.Stop else Icons.Default.PlayArrow,
            null, modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (running) "Stop Service" else "Start Gesture Service",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GestureGuideCard(emoji: String, gestureName: String, action: String, desc: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(gestureName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(desc, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyanDim.copy(alpha = 0.3f))
                    .border(1.dp, CyanBright.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(action, color = CyanBright, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun gestureEmoji(name: String) = when (name) {
    "Thumb_Up"    -> "👍"
    "Pointing_Up" -> "☝️"
    "Victory"     -> "✌️"
    "Open_Palm"   -> "🖐️"
    "Closed_Fist" -> "✊"
    "ILoveYou"    -> "🤟"
    else          -> "🤚"
}

package com.gesturecontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gesturecontrol.GestureSettings
import com.gesturecontrol.ui.theme.*

@Composable
fun SettingsScreen() {
    var thumbEnabled  by remember { mutableStateOf(GestureSettings.thumbsUpEnabled) }
    var pointEnabled  by remember { mutableStateOf(GestureSettings.pointingUpEnabled) }
    var threshold     by remember { mutableFloatStateOf(GestureSettings.confidenceThreshold) }
    var cooldownIndex by remember {
        mutableIntStateOf(listOf(500L, 1000L, 1500L, 2000L, 3000L)
            .indexOf(GestureSettings.actionCooldownMs).coerceAtLeast(0))
    }
    val cooldownLabels = listOf("0.5s", "1s", "1.5s", "2s", "3s")
    val cooldownValues = listOf(500L, 1000L, 1500L, 2000L, 3000L)

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, NavyDark)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Header
            Text("Settings", style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("Configure gesture detection behaviour", color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(4.dp))

            // Gesture toggles
            SectionHeader("Gesture Mapping", Icons.Default.TouchApp)
            SettingsCard {
                GestureToggleRow(
                    emoji = "👍", gestureName = "Thumbs Up",
                    action = "Double Tap", checked = thumbEnabled
                ) {
                    thumbEnabled = it
                    GestureSettings.thumbsUpEnabled = it
                }
                HorizontalDivider(color = NavyBorder, modifier = Modifier.padding(horizontal = 4.dp))
                GestureToggleRow(
                    emoji = "☝️", gestureName = "Pointing Up",
                    action = "Swipe Up", checked = pointEnabled
                ) {
                    pointEnabled = it
                    GestureSettings.pointingUpEnabled = it
                }
            }

            // Confidence threshold
            SectionHeader("Detection Sensitivity", Icons.Default.Tune)
            SettingsCard {
                Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Confidence Threshold", color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanDim.copy(0.3f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("${(threshold * 100).toInt()}%",
                                color = CyanBright, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it; GestureSettings.confidenceThreshold = it },
                        valueRange = 0.4f..0.95f, steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanBright, activeTrackColor = CyanMid,
                            inactiveTrackColor = NavyBorder
                        )
                    )
                    Text("Lower = more sensitive, Higher = fewer false positives",
                        color = TextMuted, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Action cooldown
            SectionHeader("Action Cooldown", Icons.Default.Timer)
            SettingsCard {
                Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Minimum delay between actions", color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurpleMid.copy(0.3f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(cooldownLabels[cooldownIndex],
                                color = PurpleBright, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Slider(
                        value = cooldownIndex.toFloat(),
                        onValueChange = {
                            cooldownIndex = it.toInt()
                            GestureSettings.actionCooldownMs = cooldownValues[it.toInt()]
                        },
                        valueRange = 0f..4f, steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = PurpleBright, activeTrackColor = PurpleMid,
                            inactiveTrackColor = NavyBorder
                        )
                    )
                }
            }

            // About
            SectionHeader("About", Icons.Default.Info)
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AboutRow("Version", "1.0.0")
                    AboutRow("ML Model", "MediaPipe Gesture Recognizer")
                    AboutRow("Min Android", "Android 9 (API 28)")
                    AboutRow("License", "Apache 2.0")
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = CyanBright, modifier = Modifier.size(18.dp))
        Text(title, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun GestureToggleRow(
    emoji: String, gestureName: String, action: String,
    checked: Boolean, onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(gestureName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text("→ $action", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = checked, onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = NavyDeep, checkedTrackColor = CyanBright)
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
    }
}

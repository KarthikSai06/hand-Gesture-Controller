package com.gesturecontrol.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gesturecontrol.GestureActionMap
import com.gesturecontrol.recognition.BuiltInGestureLibrary
import org.json.JSONArray

// ── Gesture entry model for UI ────────────────────────────────────────────────
private data class GestureEntry(
    val name: String,
    val emoji: String,
    val category: Category
) {
    enum class Category { MEDIAPIPE, LIBRARY, CUSTOM }
}

@Composable
fun GestureActionMappingScreen() {
    val context = LocalContext.current
    GestureActionMap.init(context)

    // Derive all known gestures
    val allGestures = remember { buildGestureList(context) }

    // Live map state — recompose when any mapping changes
    var mappingVersion by remember { mutableIntStateOf(0) }
    val actionMap by remember(mappingVersion) {
        derivedStateOf { GestureActionMap.getAll() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                listOf(
                    com.gesturecontrol.ui.theme.NavyDeep,
                    com.gesturecontrol.ui.theme.NavyDark
                )
            ))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Action Mapping",
                    style = MaterialTheme.typography.headlineLarge,
                    color = com.gesturecontrol.ui.theme.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Assign a system action to each gesture",
                    color = com.gesturecontrol.ui.theme.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── MediaPipe built-ins ───────────────────────────────────────────
            val mediapipeGestures = allGestures.filter { it.category == GestureEntry.Category.MEDIAPIPE }
            if (mediapipeGestures.isNotEmpty()) {
                item { CategoryHeader("MediaPipe Gestures", Icons.Default.Visibility) }
                items(mediapipeGestures, key = { it.name }) { entry ->
                    GestureMappingCard(
                        entry = entry,
                        currentAction = actionMap[entry.name] ?: GestureActionMap.ACTION_NONE
                    ) { newAction ->
                        GestureActionMap.set(entry.name, newAction)
                        mappingVersion++
                    }
                }
            }

            // ── Built-in library ──────────────────────────────────────────────
            val libraryGestures = allGestures.filter { it.category == GestureEntry.Category.LIBRARY }
            if (libraryGestures.isNotEmpty()) {
                item { CategoryHeader("Built-in Library", Icons.Default.AutoAwesome) }
                items(libraryGestures, key = { it.name }) { entry ->
                    GestureMappingCard(
                        entry = entry,
                        currentAction = actionMap[entry.name] ?: GestureActionMap.ACTION_NONE
                    ) { newAction ->
                        GestureActionMap.set(entry.name, newAction)
                        mappingVersion++
                    }
                }
            }

            // ── Custom trained ────────────────────────────────────────────────
            val customGestures = allGestures.filter { it.category == GestureEntry.Category.CUSTOM }
            if (customGestures.isNotEmpty()) {
                item { CategoryHeader("Custom Trained", Icons.Default.Psychology) }
                items(customGestures, key = { it.name }) { entry ->
                    GestureMappingCard(
                        entry = entry,
                        currentAction = actionMap[entry.name] ?: GestureActionMap.ACTION_NONE
                    ) { newAction ->
                        GestureActionMap.set(entry.name, newAction)
                        mappingVersion++
                    }
                }
            }

            // Empty custom gestures notice
            if (customGestures.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = com.gesturecontrol.ui.theme.NavyCard
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = com.gesturecontrol.ui.theme.TextMuted
                            )
                            Column {
                                Text(
                                    "No Custom Gestures Yet",
                                    color = com.gesturecontrol.ui.theme.TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Record and export a model in the Record tab",
                                    color = com.gesturecontrol.ui.theme.TextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Category header ────────────────────────────────────────────────────────────
@Composable
private fun CategoryHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, null, tint = com.gesturecontrol.ui.theme.CyanBright, modifier = Modifier.size(18.dp))
        Text(
            title,
            color = com.gesturecontrol.ui.theme.TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ── Single gesture mapping card ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureMappingCard(
    entry: GestureEntry,
    currentAction: String,
    onActionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = com.gesturecontrol.ui.theme.NavyCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji + name
            Text(entry.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name.replace("_", " "),
                    color = com.gesturecontrol.ui.theme.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "→ ${GestureActionMap.actionLabel(currentAction)}",
                    color = if (currentAction == GestureActionMap.ACTION_NONE)
                        com.gesturecontrol.ui.theme.TextMuted
                    else
                        com.gesturecontrol.ui.theme.CyanBright,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Action dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                Box(
                    modifier = Modifier
                        .menuAnchor()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (currentAction == GestureActionMap.ACTION_NONE)
                                com.gesturecontrol.ui.theme.NavyBorder
                            else
                                com.gesturecontrol.ui.theme.CyanDim.copy(alpha = 0.35f)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            GestureActionMap.actionEmoji(currentAction),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            null,
                            tint = com.gesturecontrol.ui.theme.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(com.gesturecontrol.ui.theme.NavyCard)
                ) {
                    GestureActionMap.ALL_ACTIONS.forEach { action ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(GestureActionMap.actionEmoji(action))
                                    Text(
                                        GestureActionMap.actionLabel(action),
                                        color = if (action == currentAction)
                                            com.gesturecontrol.ui.theme.CyanBright
                                        else
                                            com.gesturecontrol.ui.theme.TextPrimary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            onClick = {
                                onActionSelected(action)
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = com.gesturecontrol.ui.theme.TextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

// ── Data builder ──────────────────────────────────────────────────────────────
private fun buildGestureList(context: Context): List<GestureEntry> {
    val list = mutableListOf<GestureEntry>()

    // MediaPipe built-in gestures
    val mediapipeGestures = listOf(
        GestureEntry("Thumb_Up",     "👍", GestureEntry.Category.MEDIAPIPE),
        GestureEntry("Pointing_Up",  "☝️", GestureEntry.Category.MEDIAPIPE),
        GestureEntry("Thumb_Down",   "👎", GestureEntry.Category.MEDIAPIPE),
        GestureEntry("Closed_Fist",  "✊", GestureEntry.Category.MEDIAPIPE),
        GestureEntry("Open_Palm",    "🖐️", GestureEntry.Category.MEDIAPIPE),
        GestureEntry("Victory",      "✌️", GestureEntry.Category.MEDIAPIPE),
        GestureEntry("ILoveYou",     "🤟", GestureEntry.Category.MEDIAPIPE),
    )
    list.addAll(mediapipeGestures)

    // Built-in geometry library
    BuiltInGestureLibrary.ALL_GESTURES.forEach { info ->
        // Skip duplicates already covered by MediaPipe
        if (list.none { it.name == info.name }) {
            list.add(GestureEntry(info.name, info.emoji, GestureEntry.Category.LIBRARY))
        }
    }

    // Custom trained (from gesture_labels.json in assets or internal storage)
    val customLabels = loadCustomLabels(context)
    customLabels.forEach { label ->
        if (list.none { it.name == label }) {
            list.add(GestureEntry(label, "🧠", GestureEntry.Category.CUSTOM))
        }
    }

    return list
}

private fun loadCustomLabels(context: Context): List<String> {
    return try {
        // Try internal storage first (imported model)
        val internalFile = java.io.File(context.filesDir, "models/gesture_labels.json")
        val json = when {
            internalFile.exists() -> internalFile.readText()
            context.assets.list("")?.contains("gesture_labels.json") == true ->
                context.assets.open("gesture_labels.json").bufferedReader().readText()
            else -> return emptyList()
        }
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }
}

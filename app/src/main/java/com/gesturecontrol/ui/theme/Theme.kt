package com.gesturecontrol.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary          = CyanBright,
    onPrimary        = NavyDeep,
    primaryContainer = CyanDim,
    onPrimaryContainer = TextPrimary,
    secondary        = PurpleBright,
    onSecondary      = Color.White,
    secondaryContainer = PurpleMid,
    onSecondaryContainer = TextPrimary,
    background       = NavyDeep,
    onBackground     = TextPrimary,
    surface          = NavyDark,
    onSurface        = TextPrimary,
    surfaceVariant   = NavyCard,
    onSurfaceVariant = TextSecondary,
    error            = RedAlert,
    onError          = Color.White,
    outline          = NavyBorder,
    outlineVariant   = TextMuted
)

@Composable
fun GestureControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = GestureTypography,
        content     = content
    )
}

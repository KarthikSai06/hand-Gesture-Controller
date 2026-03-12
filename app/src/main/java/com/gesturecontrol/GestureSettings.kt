package com.gesturecontrol

// Global mutable settings — updated by SettingsScreen, read by CameraForegroundService
object GestureSettings {
    var thumbsUpEnabled: Boolean = true
    var pointingUpEnabled: Boolean = true
    var confidenceThreshold: Float = 0.55f
    var actionCooldownMs: Long = 1000L
}

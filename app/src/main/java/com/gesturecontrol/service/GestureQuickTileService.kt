package com.gesturecontrol.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class GestureQuickTileService : TileService() {

    companion object {
        /** Call this from CameraForegroundService whenever running state changes. */
        fun requestTileUpdate(service: android.content.Context) {
            requestListeningState(
                service,
                ComponentName(service, GestureQuickTileService::class.java)
            )
        }
    }

    // Called each time the tile becomes visible in the shade
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val running = CameraForegroundService.isRunning

        if (running) {
            // Stop service
            val stopIntent = Intent(this, CameraForegroundService::class.java).apply {
                action = CameraForegroundService.ACTION_STOP
            }
            startService(stopIntent)
        } else {
            // If accessibility service is not enabled, redirect user to settings
            if (GestureAccessibilityService.instance == null) {
                val a11yIntent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                launchActivityAndCollapse(a11yIntent)
                return
            }
            // Start gesture service
            ContextCompat.startForegroundService(
                this,
                Intent(this, CameraForegroundService::class.java)
            )
        }

        // Update tile after a short delay to let the service start/stop
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            updateTile()
        }, 600)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val running = CameraForegroundService.isRunning
        val a11yReady = GestureAccessibilityService.instance != null

        tile.state = when {
            running        -> Tile.STATE_ACTIVE
            !a11yReady     -> Tile.STATE_UNAVAILABLE
            else           -> Tile.STATE_INACTIVE
        }
        tile.label = "GestureControl"

        // subtitle is supported on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                running    -> "Detecting gestures"
                !a11yReady -> "Enable Accessibility first"
                else       -> "Tap to start"
            }
        }

        tile.updateTile()
    }

    // Compatibility helper: collapses shade and starts activity across API levels
    @Suppress("DEPRECATION")
    private fun launchActivityAndCollapse(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pi)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}

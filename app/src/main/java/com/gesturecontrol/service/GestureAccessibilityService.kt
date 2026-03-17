package com.gesturecontrol.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class GestureAccessibilityService : AccessibilityService() {

    companion object {
        var instance: GestureAccessibilityService? = null
        private const val TAG = "GestureA11y"

        const val ACTION_DOUBLE_TAP  = "DOUBLE_TAP"
        const val ACTION_SWIPE_UP    = "SWIPE_UP"
        const val ACTION_SWIPE_DOWN  = "SWIPE_DOWN"
        const val ACTION_VOLUME_UP   = "VOLUME_UP"
        const val ACTION_VOLUME_DOWN = "VOLUME_DOWN"
        const val ACTION_BACK        = "BACK"
        const val ACTION_HOME        = "HOME"
        const val ACTION_RECENTS     = "RECENTS"
        const val ACTION_SCROLL_UP   = "SCROLL_UP"
        const val ACTION_SCROLL_DOWN = "SCROLL_DOWN"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun performGestureAction(action: String) {
        val dm = resources.displayMetrics
        val cx = dm.widthPixels / 2f
        val cy = dm.heightPixels / 2f

        when (action) {
            ACTION_DOUBLE_TAP  -> doDoubleTap(cx, cy)
            ACTION_SWIPE_UP    -> doSwipe(cx, dm.heightPixels * 0.75f, cx, dm.heightPixels * 0.20f)
            ACTION_SWIPE_DOWN  -> doSwipe(cx, dm.heightPixels * 0.20f, cx, dm.heightPixels * 0.75f)
            ACTION_SCROLL_UP   -> doSwipe(cx, cy + 300f, cx, cy - 300f)
            ACTION_SCROLL_DOWN -> doSwipe(cx, cy - 300f, cx, cy + 300f)
            ACTION_BACK        -> performGlobalAction(GLOBAL_ACTION_BACK)
            ACTION_HOME        -> performGlobalAction(GLOBAL_ACTION_HOME)
            ACTION_RECENTS     -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            ACTION_VOLUME_UP   -> adjustVolume(AudioManager.ADJUST_RAISE)
            ACTION_VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
        }
    }

    private fun adjustVolume(direction: Int) {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun doDoubleTap(x: Float, y: Float) {
        val p = Path().apply { moveTo(x, y) }
        val tap1 = GestureDescription.StrokeDescription(p, 0L, 50L)
        val tap2 = GestureDescription.StrokeDescription(p, 150L, 50L)
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(tap1)
        gestureBuilder.addStroke(tap2)
        
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun doSwipe(sx: Float, sy: Float, ex: Float, ey: Float) {
        val p = Path().apply { moveTo(sx, sy); lineTo(ex, ey) }
        val stroke = GestureDescription.StrokeDescription(p, 0L, 400L)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }
}

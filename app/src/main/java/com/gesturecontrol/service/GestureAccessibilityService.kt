package com.gesturecontrol.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class GestureAccessibilityService : AccessibilityService() {

    companion object {
        var instance: GestureAccessibilityService? = null
        private const val TAG = "GestureA11y"

        const val ACTION_DOUBLE_TAP = "DOUBLE_TAP"
        const val ACTION_SWIPE_UP = "SWIPE_UP"
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
            ACTION_DOUBLE_TAP -> doDoubleTap(cx, cy)
            ACTION_SWIPE_UP   -> doSwipe(cx, dm.heightPixels * 0.75f, cx, dm.heightPixels * 0.20f)
        }
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

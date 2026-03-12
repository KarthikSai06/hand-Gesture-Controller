package com.gesturecontrol

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object GestureEventBus {

    private val _gestureFlow = MutableSharedFlow<GestureResult>(
        replay = 1,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val gestureFlow: SharedFlow<GestureResult> = _gestureFlow.asSharedFlow()

    fun emit(result: GestureResult) {
        _gestureFlow.tryEmit(result)
    }

    data class GestureResult(
        val gestureName: String,
        val confidence: Float,
        val actionTriggered: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
}

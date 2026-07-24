package io.kelly.modifier

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

fun Modifier.onLongPress(
    enabled: Boolean = true,
    block: () -> Unit
): Modifier = if (!enabled) this else composed {
    val currentBlock by rememberUpdatedState(block)
    pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { currentBlock() }
        )
    }
}

fun Modifier.clickableNoRipple(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBlock by rememberUpdatedState(onClick)
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = {
            val currentTime = SystemClock.elapsedRealtime()
            val isSafeState =
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (isSafeState && (currentTime - lastClickTime > 500L)) {
                lastClickTime = currentTime
                currentBlock()
            }
        }
    )
}

fun Modifier.onKeyDelEvent(
    enabled: Boolean = true,
    block: () -> Unit
): Modifier = if (!enabled) this else composed {
    val currentBlock by rememberUpdatedState(block)
    onKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && event.key == Key.Backspace) {
            currentBlock()
            true
        } else {
            false
        }
    }
}

fun Modifier.holdToTrack(
    enabled: Boolean = true,
    tolerance: Dp = 10.dp,
    onPress: () -> Unit = {},
    onInsideChanged: (isInside: Boolean) -> Unit = {},
    onRelease: (isInside: Boolean) -> Unit,
): Modifier = if (!enabled) this else composed {
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnInsideChanged by rememberUpdatedState(onInsideChanged)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val tolerancePx = with(LocalDensity.current) { tolerance.toPx() }
    pointerInput(tolerancePx) {
        awaitPointerEventScope {
            while (true) {
                val downEvent = awaitFirstDown()
                val pointerId = downEvent.id
                currentOnPress()
                var isInside = true
                currentOnInsideChanged(true)
                var finished = false
                while (!finished) {
                    val event = awaitPointerEvent()
                    val change = event.changes.find { it.id == pointerId }
                    if (change == null || !change.pressed) {
                        finished = true
                    } else {
                        val x = change.position.x
                        val y = change.position.y
                        val newIsInside = x >= -tolerancePx &&
                                x <= size.width + tolerancePx &&
                                y >= -tolerancePx &&
                                y <= size.height + tolerancePx
                        if (newIsInside != isInside) {
                            isInside = newIsInside
                            currentOnInsideChanged(isInside)
                        }
                        change.consume()
                    }
                }
                currentOnRelease(isInside)
            }
        }
    }
}
package io.kelly.modifier

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

fun Modifier.horizontalDraggable(
    enabled: Boolean = true,
    initialProgress: Float = 0f,
    onDrag: ((Float) -> Unit)? = null,
    onRelease: ((Float) -> Unit)? = null
) = draggable(
    enabled = enabled,
    initialProgress = Offset(initialProgress, 0f),
    enableTranslationX = true,
    enableTranslationY = false,
    onDrag = if (onDrag == null) null else { progress -> onDrag(progress.x) },
    onRelease = if (onRelease == null) null else { progress -> onRelease(progress.x) }
)

fun Modifier.verticalDraggable(
    enabled: Boolean = true,
    initialProgress: Float = 0f,
    onDrag: ((Float) -> Unit)? = null,
    onRelease: ((Float) -> Unit)? = null
) = draggable(
    enabled = enabled,
    initialProgress = Offset(0f, initialProgress),
    enableTranslationX = false,
    enableTranslationY = true,
    onDrag = if (onDrag == null) null else { progress -> onDrag(progress.y) },
    onRelease = if (onRelease == null) null else { progress -> onRelease(progress.y) }
)

fun Modifier.draggable(
    enabled: Boolean = true,
    initialProgress: Offset = Offset.Zero,
    enableTranslationX: Boolean = true,
    enableTranslationY: Boolean = true,
    onDrag: ((Offset) -> Unit)? = null,
    onRelease: ((Offset) -> Unit)? = null
): Modifier = if (!enabled) this else composed {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentInitialProgress by rememberUpdatedState(initialProgress)

    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    var elementSize by remember { mutableStateOf(IntSize.Zero) }
    var dragOffset by remember { mutableStateOf<Offset?>(null) }

    val maxX = (parentSize.width - elementSize.width).coerceAtLeast(0).toFloat()
    val maxY = (parentSize.height - elementSize.height).coerceAtLeast(0).toFloat()

    val currentOffset = dragOffset ?: Offset(
        x = currentInitialProgress.x * maxX,
        y = currentInitialProgress.y * maxY
    )

    this
        .onGloballyPositioned {
            parentSize = it.parentLayoutCoordinates?.size ?: IntSize.Zero
            elementSize = it.size
        }
        .graphicsLayer {
            alpha = if (parentSize == IntSize.Zero) 0f else 1f
            if (enableTranslationX) {
                translationX = if (isRtl) -currentOffset.x else currentOffset.x
            }
            if (enableTranslationY) {
                translationY = currentOffset.y
            }
        }
        .pointerInput(enabled, maxX, maxY, isRtl) {
            if (!enabled || (maxX == 0f && maxY == 0f)) return@pointerInput
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    val dragX = if (isRtl) -dragAmount.x else dragAmount.x
                    val activeOffset = dragOffset ?: Offset(
                        x = currentInitialProgress.x * maxX,
                        y = currentInitialProgress.y * maxY
                    )

                    val nextOffset = Offset(
                        x = (activeOffset.x + dragX).coerceIn(0f, maxX),
                        y = (activeOffset.y + dragAmount.y).coerceIn(0f, maxY)
                    )
                    dragOffset = nextOffset

                    currentOnDrag?.invoke(
                        Offset(
                            x = if (maxX > 0) nextOffset.x / maxX else 0f,
                            y = if (maxY > 0) nextOffset.y / maxY else 0f
                        )
                    )
                },
                onDragEnd = {
                    val activeOffset = dragOffset ?: Offset(
                        x = currentInitialProgress.x * maxX,
                        y = currentInitialProgress.y * maxY
                    )
                    currentOnRelease?.invoke(
                        Offset(
                            x = if (maxX > 0) activeOffset.x / maxX else 0f,
                            y = if (maxY > 0) activeOffset.y / maxY else 0f
                        )
                    )
                },
                onDragCancel = {
                    val activeOffset = dragOffset ?: Offset(
                        x = currentInitialProgress.x * maxX,
                        y = currentInitialProgress.y * maxY
                    )
                    currentOnRelease?.invoke(
                        Offset(
                            x = if (maxX > 0) activeOffset.x / maxX else 0f,
                            y = if (maxY > 0) activeOffset.y / maxY else 0f
                        )
                    )
                }
            )
        }
        .systemGestureExclusion()
}
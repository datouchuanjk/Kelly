package io.kelly.composable.nestedscroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import io.kelly.modifier.applyIf
import io.kelly.modifier.applyUnless

@Composable
fun NestedScrollSticky(
    modifier: Modifier = Modifier,
    state: NestedScrollStickyState = rememberNestedScrollStickyState(),
    header: @Composable BoxScope.() -> Unit,
    sticky: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.(Modifier) -> Unit
) {
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var stickyHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffset by rememberSaveable { mutableFloatStateOf(0f) }

    LaunchedEffect(headerHeightPx) {
        state.headerHeightPx = headerHeightPx
    }
    LaunchedEffect(stickyHeightPx) {
        state.stickyHeightPx = stickyHeightPx
    }
    LaunchedEffect(headerHeightPx, headerOffset) {
        state.offset = if (headerHeightPx > 0f) {
            (-headerOffset / headerHeightPx).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    LaunchedEffect(headerHeightPx) {
        if (headerHeightPx > 0f && headerOffset < -headerHeightPx) {
            headerOffset = -headerHeightPx
        }
    }
    val currentHeaderHeightPx by rememberUpdatedState(headerHeightPx)

    var isHeaderExpandingInThisGesture by remember { mutableStateOf(false) }

    fun post(delta: Float): Offset {
        if (headerOffset < 0f) {
            isHeaderExpandingInThisGesture = true
            val oldOffset = headerOffset
            val targetOffset = (oldOffset + delta).coerceAtMost(0f)
            headerOffset = targetOffset
            val headerConsumed = targetOffset - oldOffset
            return if (state.isContinuousConsumed) {
                Offset(0f, headerConsumed)
            } else {
                Offset(0f, delta)
            }
        }

        if (isHeaderExpandingInThisGesture && !state.isContinuousConsumed) {
            return Offset(0f, delta)
        }
        return Offset.Zero
    }

    val grandpaConnection = remember {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f && headerOffset > -currentHeaderHeightPx) {
                    val oldOffset = headerOffset
                    val targetOffset = (oldOffset + delta).coerceAtLeast(-currentHeaderHeightPx)
                    headerOffset = targetOffset
                    return Offset(0f, targetOffset - oldOffset)
                } else if (delta > 0f && state.isQuickExpand) {
                    return post(delta)
                }
                return Offset.Zero
            }
        }
    }
    val proxyConnection = remember(state) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0f) {
                    return post(delta)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                isHeaderExpandingInThisGesture = false
                return super.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isHeaderExpandingInThisGesture = false
                return super.onPostFling(consumed, available)
            }
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(grandpaConnection)
            .applyUnless(state.isMultipleNestedScroll) {
                nestedScroll(proxyConnection)
            }
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged {
                    val newHeight = it.height.toFloat()
                    if (headerHeightPx != newHeight) {
                        headerHeightPx = newHeight
                    }
                }
                .graphicsLayer {
                    translationY = headerOffset
                }
        ) {
            header()
        }
        if (sticky != null) {
            Box(
                modifier = Modifier
                    .onSizeChanged {
                        val newHeight = it.height.toFloat()
                        if (stickyHeightPx != newHeight) {
                            stickyHeightPx = newHeight
                        }
                    }
                    .graphicsLayer {
                        translationY = headerHeightPx + headerOffset
                    }
            ) {
                sticky()
            }
        }
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val top = (headerHeightPx + stickyHeightPx + headerOffset).toInt()
                    val height = (constraints.maxHeight - top).coerceAtLeast(0)
                    val placeable = measurable.measure(
                        constraints.copy(
                            minHeight = height,
                            maxHeight = height
                        )
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(0, top)
                    }
                }
        ) {
            content(
                Modifier.applyIf(state.isMultipleNestedScroll ) {
                    nestedScroll(proxyConnection)
                }
            )
        }
    }
}
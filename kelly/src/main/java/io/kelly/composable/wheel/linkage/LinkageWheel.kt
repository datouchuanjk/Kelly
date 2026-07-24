package io.kelly.composable.wheel.linkage

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kelly.modifier.clickableNoRipple
import io.kelly.composable.wheel.Wheel
import io.kelly.composable.wheel.WheelDefaults
import io.kelly.composable.wheel.linkage.provider.LinkageProvider
import androidx.compose.foundation.layout.ColumnScope


@Composable
fun <T> LinkageWheel(
    modifier: Modifier = Modifier,
    provider: LinkageProvider<T>,
    itemHeight: Dp = 47.dp,
    visibleCount: Int = 5,
    resetStateOnParentChange: Boolean = true,
    flingBehaviorProvider: @Composable (LazyListState) -> FlingBehavior = { state ->
        rememberSnapFlingBehavior(state)
    },
    drawBackground: ContentDrawScope.(Dp) -> Unit = remember { WheelDefaults.drawBackground },
    graphicsAnimator: GraphicsLayerScope.(Float) -> Unit = remember { WheelDefaults.graphicsAnimator },
    itemContent: @Composable (T, Int) -> Unit = { item, level ->
        Text(text = provider.map(item, level), fontSize = 12.sp, color = Color(0xff333333))
    },
    confirmButton: @Composable (ColumnScope.(onConfirm: () -> Unit) -> Unit)? = { onConfirm ->
        Icon(
            tint = Color(0xff333333),
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 15.dp, vertical = 8.dp)
                .clickableNoRipple {
                    onConfirm()
                }
        )
    },
    onScrollChanged: ((Triple<T?, T?, T?>) -> Unit)? = null,
    onConfirm: ((Triple<T?, T?, T?>) -> Unit)? = null,
) {
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnScrollChanged by rememberUpdatedState(onScrollChanged)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val primaryList = remember(provider) { provider.providePrimary() }
    if (primaryList.isEmpty()) return

    val pInitialIndex = remember(primaryList) {
        provider.initialPrimaryIndex(primaryList).coerceIn(primaryList.indices)
    }
    val primaryState = rememberLazyListState(pInitialIndex)

    val currentPrimary by remember {
        derivedStateOf {
            primaryList.getOrElse(
                primaryState.currentItemIndex(itemHeightPx, primaryList.size)
            ) { primaryList.first() }
        }
    }

    val secondaryList by remember {
        derivedStateOf {
            provider.provideSecondary(currentPrimary)
        }
    }

    val secondaryStateKey = if (resetStateOnParentChange) currentPrimary else Unit
    val secondaryState = remember(secondaryStateKey) {
        val initial = if (secondaryList.isEmpty()) 0
        else provider.initialSecondaryIndex(secondaryList).coerceIn(secondaryList.indices)
        LazyListState(initial)
    }

    val currentSecondary by remember {
        derivedStateOf {
            secondaryList.getOrElse(
                secondaryState.currentItemIndex(itemHeightPx, secondaryList.size)
            ) {
                secondaryList.firstOrNull()
            }
        }
    }

    val tertiaryList by remember {
        derivedStateOf {
            provider.provideTertiary(currentPrimary, currentSecondary)
        }
    }

    val tertiaryStateKey1 = if (resetStateOnParentChange) currentPrimary else Unit
    val tertiaryStateKey2 = if (resetStateOnParentChange) currentSecondary else Unit
    val tertiaryState = remember(tertiaryStateKey1, tertiaryStateKey2) {
        val initial = if (tertiaryList.isEmpty()) 0
        else provider.initialTertiaryIndex(tertiaryList).coerceIn(tertiaryList.indices)
        LazyListState(initial)
    }

    val currentTertiary by remember {
        derivedStateOf {
            tertiaryList.getOrElse(
                tertiaryState.currentItemIndex(itemHeightPx, tertiaryList.size)
            ) {
                tertiaryList.firstOrNull()
            }
        }
    }

    if (!resetStateOnParentChange) {
        LaunchedEffect(secondaryList.size) {
            if (secondaryState.firstVisibleItemIndex >= secondaryList.size && secondaryList.isNotEmpty()) {
                secondaryState.scrollToItem((secondaryList.size - 1).coerceAtLeast(0))
            }
        }
        LaunchedEffect(tertiaryList.size) {
            if (tertiaryState.firstVisibleItemIndex >= tertiaryList.size && tertiaryList.isNotEmpty()) {
                tertiaryState.scrollToItem((tertiaryList.size - 1).coerceAtLeast(0))
            }
        }
    }

    val currentSelection = Triple(currentPrimary, currentSecondary, currentTertiary)

    LaunchedEffect(currentPrimary, currentSecondary, currentTertiary) {
        currentOnScrollChanged?.invoke(currentSelection)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        confirmButton?.invoke(this) {
            currentOnConfirm?.invoke(currentSelection)
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Wheel(
                modifier = Modifier.weight(1f),
                state = primaryState,
                itemCount = primaryList.size,
                itemHeight = itemHeight,
                visibleCount = visibleCount,
                flingBehavior = flingBehaviorProvider(primaryState),
                drawBackground = drawBackground,
                graphicsAnimator = graphicsAnimator
            ) { index -> primaryList.getOrNull(index)?.let { itemContent(it, 0) } }

            if (secondaryList.isNotEmpty()) {
                Wheel(
                    modifier = Modifier.weight(1f),
                    state = secondaryState,
                    itemCount = secondaryList.size,
                    itemHeight = itemHeight,
                    visibleCount = visibleCount,
                    flingBehavior = flingBehaviorProvider(secondaryState),
                    drawBackground = drawBackground,
                    graphicsAnimator = graphicsAnimator
                ) { index ->
                    secondaryList.getOrNull(index)?.let { itemContent(it, 1) }
                }
            }

            if (tertiaryList.isNotEmpty()) {
                Wheel(
                    modifier = Modifier.weight(1f),
                    state = tertiaryState,
                    itemCount = tertiaryList.size,
                    itemHeight = itemHeight,
                    visibleCount = visibleCount,
                    flingBehavior = flingBehaviorProvider(tertiaryState),
                    drawBackground = drawBackground,
                    graphicsAnimator = graphicsAnimator
                ) { index ->
                    tertiaryList.getOrNull(index)?.let { itemContent(it, 2) }
                }
            }
        }
    }
}

internal fun LazyListState.currentItemIndex(itemHeightPx: Float, maxSize: Int): Int {
    if (maxSize == 0) return 0
    val index = if (firstVisibleItemScrollOffset > itemHeightPx / 2) {
        firstVisibleItemIndex + 1
    } else {
        firstVisibleItemIndex
    }
    return index.coerceIn(0, maxSize - 1)
}
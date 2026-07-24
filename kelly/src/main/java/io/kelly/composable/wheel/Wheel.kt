package io.kelly.composable.wheel


import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.dp

@Composable
internal fun RowScope.Wheel(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    itemCount: Int,
    itemHeight: Dp = 48.dp,
    visibleCount: Int = 5,
    flingBehavior: FlingBehavior = rememberSnapFlingBehavior(state),
    drawBackground: ContentDrawScope.(Dp) -> Unit = remember { WheelDefaults.drawBackground },
    graphicsAnimator: GraphicsLayerScope.(Float) -> Unit = remember { WheelDefaults.graphicsAnimator },
    content: @Composable (Int) -> Unit
) {
    require(visibleCount % 2 != 0)
    LazyColumn(
        modifier = modifier
            .height(itemHeight * visibleCount)
            .weight(1f)
            .drawWithContent {
                drawBackground(itemHeight)
                drawContent()
            },
        state = state,
        contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
        flingBehavior = flingBehavior,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(itemCount, key = { it }) { index ->
            Box(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .height(itemHeight)
                    .graphicsLayer {
                        val itemHeightPx = itemHeight.toPx()
                        val topPaddingPx = (visibleCount - 1) / 2f * itemHeightPx
                        val staticItemTop = index * itemHeightPx + topPaddingPx
                        val scrollOffset =
                            state.firstVisibleItemIndex * itemHeightPx + state.firstVisibleItemScrollOffset
                        val currentItemTop = staticItemTop - scrollOffset
                        val currentItemCenter = currentItemTop + itemHeightPx / 2f
                        val distanceFromCenter =
                            currentItemCenter - (size.height * visibleCount / 2f)
                        val ratio =
                            (distanceFromCenter / (size.height * visibleCount / 2f)).coerceIn(
                                -1f,
                                1f
                            )
                        cameraDistance = 16f * density
                        graphicsAnimator(ratio)
                    },
                contentAlignment = Alignment.Center
            ) {
                content(index)
            }
        }
    }
}

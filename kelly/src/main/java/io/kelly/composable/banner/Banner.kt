package io.kelly.composable.banner

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

@Composable
fun <T> Banner(
    data: List<T>,
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 30.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 15.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: T) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
        state,
        Orientation.Horizontal
    ),
    snapPosition: SnapPosition = SnapPosition.Start,
    animator: (GraphicsLayerScope.(Float) -> Unit)? = {
        lerp(0.85f, 1f, it).let { value ->
            scaleY = value
            alpha = value
        }
    },
    indicator: (@Composable ColumnScope.() -> Unit)? = {
        HorizontalPagerIndicator(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 5.dp)
                .wrapContentWidth(),
            pageCount = data.size,
            pagerState = state
        )
    },
    pageContent: @Composable PagerScope.(T) -> Unit
) {
    if (data.isEmpty()) return
    Column {
        HorizontalPager(
            state = state,
            modifier = modifier,
            contentPadding = contentPadding,
            pageSize = pageSize,
            beyondViewportPageCount = beyondViewportPageCount,
            pageSpacing = pageSpacing,
            verticalAlignment = verticalAlignment,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            reverseLayout = reverseLayout,
            key = if (key == null) {
                null
            } else {
                {
                    val index = it % data.size
                    val item = data[index]
                    key(item)
                }
            },
            pageNestedScrollConnection = pageNestedScrollConnection,
            snapPosition = snapPosition,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        animator?.let {
                            val value = 1 - (state.currentPage - page + state.currentPageOffsetFraction)
                                    .absoluteValue.coerceIn(0f, 1f)
                            it.invoke(this, value)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val index = page % data.size
                val item = data[index]
                pageContent(item)
            }
        }
        indicator?.invoke(this)
    }
}

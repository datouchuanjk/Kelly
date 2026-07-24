package io.kelly.composable.nestedscroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberNestedScrollStickyState(
    isMultipleNestedScroll: Boolean = false,
    isContinuousConsumed: Boolean = false,
    isQuickExpand: Boolean = false,
): NestedScrollStickyState {
    return remember(isMultipleNestedScroll, isContinuousConsumed, isQuickExpand) {
        NestedScrollStickyState(isMultipleNestedScroll, isContinuousConsumed,isQuickExpand)
    }
}

@Stable
class NestedScrollStickyState internal constructor(
    internal val isMultipleNestedScroll: Boolean = true,
    internal val isContinuousConsumed: Boolean = false,
    internal val isQuickExpand: Boolean = false,
) {
    var offset: Float by mutableFloatStateOf(0f)
        internal set
    var headerHeightPx: Float by mutableFloatStateOf(0f)
        internal set
    var stickyHeightPx: Float by mutableFloatStateOf(0f)
        internal set
    val isHeaderFullyCollapsed by derivedStateOf {
        offset >= 1f
    }
}
package io.kelly.modifier

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.yield

inline fun Modifier.applyIf(
    condition: Boolean,
    block: Modifier.() -> Modifier
) = if (condition) block() else this


inline fun Modifier.applyUnless(
    condition: Boolean,
    block: Modifier.() -> Modifier
) = if (!condition) block() else this


fun Modifier.autoFocusRequester(): Modifier = composed {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        yield()
        focusRequester.requestFocus()
    }
    focusRequester(focusRequester)
}

fun Modifier.negativePaddingTop(margin: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val marginPx = margin.roundToPx()
    val targetHeight = (placeable.height - marginPx).coerceAtLeast(0)
    layout(placeable.width, targetHeight) {
        placeable.placeRelative(0, -marginPx)
    }
}
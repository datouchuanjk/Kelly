package io.kelly.remember.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Composable
fun <T> DisplayController<T>.popup(
    properties: PopupProperties = PopupProperties(focusable = true),
    onDismissRequest: DisplayController<T>.() -> Unit = { },
    positionProvider: PopupPosition.() -> IntOffset = {
        IntOffset(
            anchorBounds.left,
            anchorBounds.bottom + 10.dp.roundToPx()
        )
    },
    content: @Composable (DisplayController<T>.(T) -> Unit)
) = content { data ->
    val density = LocalDensity.current
    val currentPositionProvider by rememberUpdatedState(positionProvider)
    Popup(
        popupPositionProvider = remember {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    return currentPositionProvider(
                        PopupPosition(
                            density,
                            anchorBounds,
                            windowSize,
                            layoutDirection,
                            popupContentSize
                        )
                    )
                }
            }
        },
        properties = properties,
        onDismissRequest = {
            onDismissRequest()
            dismiss()
        }
    ) {
        content(data)
    }
}

class PopupPosition internal constructor(
    density: Density,
    val anchorBounds: IntRect,
    val windowSize: IntSize,
    val layoutDirection: LayoutDirection,
    val popupContentSize: IntSize,
) : Density by density

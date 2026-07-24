package io.kelly.composable.wheel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object WheelDefaults {
    val drawBackground: ContentDrawScope.(Dp) -> Unit = { itemHeight ->
        val dividerYTop = size.height / 2f - itemHeight.toPx() / 2f
        val dividerYBottom = size.height / 2f + itemHeight.toPx() / 2f
        drawLine(
            color = Color.LightGray.copy(0.4f),
            strokeWidth = 1.dp.toPx(),
            start = Offset(0f, dividerYTop),
            end = Offset(size.width, dividerYTop)
        )
        drawLine(
            color = Color.LightGray.copy(0.4f),
            strokeWidth = 1.dp.toPx(),
            start = Offset(0f, dividerYBottom),
            end = Offset(size.width, dividerYBottom)
        )
    }

    val graphicsAnimator: GraphicsLayerScope.(Float) -> Unit = { ratio ->
        val absRatio = kotlin.math.abs(ratio)
        alpha = 1f - (absRatio * 0.7f)
        val scale = 1f - (absRatio * 0.25f)
        scaleX = scale
        scaleY = scale
        rotationX = -45f * ratio
    }
}


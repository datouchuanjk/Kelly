package io.kelly.modifier

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun Modifier.shimmer(
    enable: Boolean = true,
    angle: Int = 45,
    shimmerColors: List<Color> = listOf(Color.White),
    shimmerWidthRatio: Float = 0.5f,
    durationMillis: Int = 1500,
    clipToContent: Boolean = true
): Modifier = if (!enable) this else composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing)
        ),
        label = "progress"
    )
    val finalColors = remember(shimmerColors) {
        if (shimmerColors.isEmpty()) {
            val c = Color.White
            listOf(c.copy(0f), c.copy(0.4f), c.copy(0f))
        } else {
            listOf(
                shimmerColors.first().copy(alpha = 0f)
            ) + shimmerColors + listOf(shimmerColors.last().copy(alpha = 0f))
        }
    }
    graphicsLayer(
        compositingStrategy = if (clipToContent) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    ).drawWithCache {
            val width = size.width
            val height = size.height
            val rad = Math.toRadians(angle.toDouble())
            val cosValue = cos(rad).toFloat()
            val sinValue = sin(rad).toFloat()
            val maxSide = sqrt(width * width + height * height)
            val shimmerWidth = maxSide * shimmerWidthRatio
            val totalDistance = maxSide + shimmerWidth
            val center = Offset(width / 2f, height / 2f)
            onDrawWithContent {
                drawContent()
                val currentPos = -shimmerWidth + totalDistance * progress
                val startOffset = Offset(
                    x = center.x + (currentPos - maxSide / 2f) * cosValue,
                    y = center.y + (currentPos - maxSide / 2f) * sinValue
                )
                val endOffset = Offset(
                    x = startOffset.x + shimmerWidth * cosValue,
                    y = startOffset.y + shimmerWidth * sinValue
                )

                drawRect(
                    brush = Brush.linearGradient(
                        colors = finalColors,
                        start = startOffset,
                        end = endOffset,
                        tileMode = TileMode.Clamp
                    ),
                    blendMode = if (clipToContent) BlendMode.SrcAtop else BlendMode.SrcOver
                )
            }
        }
}
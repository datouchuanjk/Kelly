package io.kelly.composable.text

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    forceMarqueeScroll: Boolean = true,
    style: TextStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp),
    times: Int = Int.MAX_VALUE,
    velocity: Dp = 50.dp,
    animateEnd: (() -> Unit)? = null
) {
    val layoutDirection = LocalLayoutDirection.current
    val currentAnimateEnd by rememberUpdatedState(animateEnd)
    var containerWidth by remember { mutableIntStateOf(0) }
    var hasFinishedNormally by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()
    val textWidth = remember(text, style) {
        textMeasurer.measure(text = text, style = style).size.width
    }
    val density = LocalDensity.current
    val offsetX = remember(text) { Animatable(0f) }
    val alpha = remember(text) { Animatable(if (enable) 0f else 1f) }
    val fadeDurationMs = 600
    LaunchedEffect(text) {
        hasFinishedNormally = false
        if (text.isEmpty()) {
            hasFinishedNormally = true
            currentAnimateEnd?.invoke()
        }
    }

    DisposableEffect(text) {
        onDispose {
            if (!hasFinishedNormally && text.isNotEmpty()) {
                currentAnimateEnd?.invoke()
            }
        }
    }

    if (text.isNotEmpty()) {
        LaunchedEffect(text, containerWidth, textWidth, enable) {
            if (!enable) {
                offsetX.snapTo(0f)
                alpha.snapTo(1f)
                return@LaunchedEffect
            }

            if (containerWidth > 0 && textWidth > 0) {
                val shouldScroll = textWidth > containerWidth || forceMarqueeScroll
                if (shouldScroll) {
                    val isLtr = layoutDirection == LayoutDirection.Ltr
                    val startX = if (isLtr) containerWidth.toFloat() else -textWidth.toFloat()
                    val endX = if (isLtr) -textWidth.toFloat() else containerWidth.toFloat()
                    val distance = abs(startX) + abs(endX)
                    val velocityPxPerSec = with(density) { velocity.toPx() }
                    val durationMs = ((distance / velocityPxPerSec) * 1000).toLong()

                    alpha.snapTo(0f)
                    offsetX.snapTo(startX)

                    launch {
                        alpha.animateTo(1f, animationSpec = tween(durationMillis = fadeDurationMs))
                    }

                    repeat(times) {
                        if (offsetX.value != startX) {
                            offsetX.snapTo(startX)
                        }
                        offsetX.animateTo(
                            targetValue = endX,
                            animationSpec = tween(
                                durationMillis = durationMs.toInt(),
                                easing = LinearEasing
                            )
                        )
                    }

                    alpha.animateTo(0f, animationSpec = tween(durationMillis = fadeDurationMs))
                    hasFinishedNormally = true
                    currentAnimateEnd?.invoke()
                } else {
                    offsetX.snapTo(0f)
                    alpha.snapTo(1f)
                    hasFinishedNormally = true
                    currentAnimateEnd?.invoke()
                }
            }
        }

        Box(
            modifier = modifier
                .clipToBounds()
                .onSizeChanged { containerWidth = it.width }
        ) {
            Text(
                text = text,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
                modifier = Modifier
                    .offset { IntOffset(x = offsetX.value.toInt(), y = 0) }
                    .graphicsLayer { this.alpha = alpha.value }
            )
        }
    }
}
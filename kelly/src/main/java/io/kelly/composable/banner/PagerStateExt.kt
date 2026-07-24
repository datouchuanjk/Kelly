package io.kelly.composable.banner

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PagerState.withAutoScroll(
    interval: Duration = 3000.milliseconds,
    animationSpec: AnimationSpec<Float> = tween(500)
) = apply {
    val isDragged by interactionSource.collectIsDraggedAsState()
    if (!isDragged) {
        LaunchedEffect(settledPage) {
            delay(interval)
            val next = currentPage
                .plus(1)
                .mod(pageCount)
            animateScrollToPage(
                page = next,
                animationSpec = animationSpec
            )
        }
    }
}
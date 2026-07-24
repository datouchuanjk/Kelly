package io.kelly.composable.banner

import androidx.annotation.FloatRange
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable

@Composable
fun rememberInfinitePagerState(
    initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5)
    initialPageOffsetFraction: Float = 0f,
    realPageCount: () -> Int
): PagerState {
    val count = realPageCount()
    if (count == 0) {
        return rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = initialPageOffsetFraction,
            pageCount = { 0 }
        )
    }

    return rememberPagerState(
        initialPage = (Int.MAX_VALUE / 2).run {
            this - (this % count) + initialPage
        },
        initialPageOffsetFraction = initialPageOffsetFraction,
        pageCount = { Int.MAX_VALUE }
    )
}


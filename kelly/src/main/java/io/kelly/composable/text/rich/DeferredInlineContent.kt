package io.kelly.composable.text.rich

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

internal data class DeferredInlineContent  (
    val width: Dp,
    val height: Dp,
    val padding: PaddingValues = PaddingValues(0.dp),
    val align: PlaceholderVerticalAlign = PlaceholderVerticalAlign.Center,
    val content: @Composable BoxScope.() -> Unit
)

internal fun DeferredInlineContent.toInlineTextContent(
    density: Density,
    layoutDirection: LayoutDirection
): InlineTextContent {
    val totalWidth = with(density) {
        (width +
                padding.calculateLeftPadding(layoutDirection) +
                padding.calculateRightPadding(layoutDirection)).toSp()
    }
    val totalHeight = with(density) {
        (height +
                padding.calculateTopPadding() +
                padding.calculateBottomPadding()).toSp()
    }

    return InlineTextContent(
        placeholder = Placeholder(totalWidth, totalHeight, align),
        children = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopStart,
                content = content
            )
        }
    )
}
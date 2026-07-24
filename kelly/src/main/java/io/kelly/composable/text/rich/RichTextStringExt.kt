package io.kelly.composable.text.rich

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kelly.extension.tryResult
import io.kelly.modifier.clickableNoRipple

fun RichTextString.Builder.append(any: Any?) {
    builder.append(any.toString())
}

fun RichTextString.Builder.appendHtml(
    htmlString: String,
    linkStyles: TextLinkStyles? = null,
    linkInteractionListener: LinkInteractionListener? = null
) {
    tryResult {
        builder.append(AnnotatedString.fromHtml(htmlString, linkStyles, linkInteractionListener))
    }.onFailure {
        append(htmlString)
    }
}

inline fun RichTextString.Builder.withStyle(
    style: SpanStyle,
    block: RichTextString.Builder.() -> Unit
) = builder.withStyle(style) {
    block()
}

inline fun RichTextString.Builder.withLink(
    style: SpanStyle? = null,
    focusedStyle: SpanStyle? = null,
    hoveredStyle: SpanStyle? = null,
    pressedStyle: SpanStyle? = null,
    tag: String? = null,
    noinline onClick: (LinkAnnotation) -> Unit,
    block: RichTextString.Builder.() -> Unit
) = builder.withLink(
    LinkAnnotation.Clickable(
        tag = tag.orEmpty(),
        styles = TextLinkStyles(
            style = style,
            focusedStyle = focusedStyle,
            hoveredStyle = hoveredStyle,
            pressedStyle = pressedStyle,
        ),
        linkInteractionListener = onClick
    )
) {
    block()
}

fun RichTextString.Builder.replaceFirst(target: String, block: RichTextString.Builder.() -> Unit) {
    val currentContent = builder.toAnnotatedString()
    val text = currentContent.text
    val index = text.indexOf(target)
    if (index == -1) return
    val newBuilder = AnnotatedString.Builder()
    newBuilder.append(currentContent.subSequence(0, index))
    builder = newBuilder
    block()
    builder.append(currentContent.subSequence(index + target.length, text.length))
}

fun RichTextString.Builder.appendInlineContent(
    width: Dp,
    height: Dp,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    align: PlaceholderVerticalAlign = PlaceholderVerticalAlign.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val id = "inlineTextContent_${imageCounter++}"
    builder.appendInlineContent(id)
    deferredMap[id] = DeferredInlineContent(width, height, paddingValues, align, content)
}

fun RichTextString.Builder.appendImage(
    painter: Painter,
    width: Dp,
    height: Dp,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    align: PlaceholderVerticalAlign = PlaceholderVerticalAlign.Center,
    contentDescription: String? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    tint: Color? = null,
    onClick: (() -> Unit)? = null
) {
    appendInlineContent(
        width = width,
        height = height,
        paddingValues = paddingValues,
        align = align
    ) {
        Image(
            alignment = alignment,
            alpha = alpha,
            contentScale = contentScale,
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .then(if (onClick == null) Modifier else Modifier.clickableNoRipple { onClick() }),
            colorFilter = tint?.let { ColorFilter.tint(it) }
        )
    }
}
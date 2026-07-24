package io.kelly.composable.text.rich

import androidx.compose.ui.text.AnnotatedString


inline fun buildRichTextString(block: RichTextString.Builder.() -> Unit): RichTextString {
    return RichTextString.Builder().apply(block).build()
}

class RichTextString internal constructor(
    internal val text: AnnotatedString,
    internal val deferredMap: Map<String, DeferredInlineContent>
) {
    class Builder {
        @PublishedApi
        internal var builder = AnnotatedString.Builder()
        internal val deferredMap = mutableMapOf<String, DeferredInlineContent>()
        internal var imageCounter = 0

        fun build(): RichTextString {
            val finalAnnotatedString = builder.toAnnotatedString()
            val activeIds = finalAnnotatedString.getStringAnnotations(
                tag = "androidx.compose.foundation.text.inlineContent",
                start = 0,
                end = finalAnnotatedString.length
            ).map { it.item }.toSet()
            val cleanedMap = deferredMap.filterKeys { it in activeIds }
            return RichTextString(finalAnnotatedString, cleanedMap)
        }
    }
}
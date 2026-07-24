package io.kelly.modifier

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

fun Modifier.ninePatchBackground(
    @DrawableRes resId: Int,
    useIntrinsicSize: Boolean = true
): Modifier = composed {
    val context = LocalContext.current
    val density = LocalDensity.current
    val (drawable, padding) = remember(resId, context, density) {
        val rawDrawable = ContextCompat.getDrawable(context, resId)?.mutate()
        val rect = Rect()
        if (rawDrawable is NinePatchDrawable) {
            rawDrawable.getPadding(rect)
        }
        val paddingValues = with(density) {
            PaddingValues(
                start = rect.left.toDp(),
                top = rect.top.toDp(),
                end = rect.right.toDp(),
                bottom = rect.bottom.toDp()
            )
        }
        rawDrawable to paddingValues
    }
    ninePatchBackground(drawable, padding, density, useIntrinsicSize)
}

private fun Modifier.ninePatchBackground(
    drawable: Drawable?,
    padding: PaddingValues,
    density: Density,
    useIntrinsicSize: Boolean = true
): Modifier {
    if (drawable == null) return this
    return applyIf(useIntrinsicSize) {
        widthIn(min = with(density) { drawable.intrinsicWidth.toDp() })
            .heightIn(min = with(density) { drawable.intrinsicHeight.toDp() })
    }
        .drawBehind {
            drawable.setBounds(
                0,
                0,
                size.width.roundToInt()
                    .coerceAtLeast(if (useIntrinsicSize) drawable.intrinsicWidth else 0),
                size.height.roundToInt()
                    .coerceAtLeast(if (useIntrinsicSize) drawable.intrinsicHeight else 0),
            )
            drawIntoCanvas { canvas ->
                drawable.draw(canvas.nativeCanvas)
            }
        }
        .padding(padding)
}


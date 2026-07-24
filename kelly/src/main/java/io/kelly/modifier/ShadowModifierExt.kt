package io.kelly.modifier

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.outerShadow(
    elevation: Dp,
    color: Color = Color.Black.copy(0.2f),
    shape: Shape = RectangleShape,
    offsetY: Dp = elevation / 2f,
    offsetX: Dp = 0.dp
) = drawWithCache {
    val blurRadius = elevation.toPx()
    val paint = Paint().apply {
        val frameworkPaint = asFrameworkPaint()
        frameworkPaint.color = android.graphics.Color.TRANSPARENT
        if (blurRadius > 0f) {
            frameworkPaint.setShadowLayer(
                blurRadius,
                offsetX.toPx(),
                offsetY.toPx(),
                color.toArgb()
            )
        }
    }
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.drawOutline(outline, paint)
        }
    }
}


fun Modifier.innerShadow(
    elevation: Dp,
    color: Color = Color.Black.copy(0.2f),
    shape: Shape = RectangleShape,
    offsetX: Dp = 0.dp,
    offsetY: Dp = elevation / 2f,
) = drawWithCache {
    val blurRadius = elevation.toPx()
    val paint = Paint().apply {
        asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            if (blurRadius > 0f) {
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
        }
    }

    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }

    val shadowPath = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(size.toRect().inflate(blurRadius * 2f))
        addPath(path)
    }

    val dx = offsetX.toPx()
    val dy = offsetY.toPx()

    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipPath(path)
            canvas.translate(dx, dy)
            canvas.drawPath(shadowPath, paint)
            canvas.restore()
        }
    }
}
package com.pandorika.irbruteforcer.ui.pixel

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Notched 4-side pixel border (the mockup's four box-shadows): the corners stay empty.
 * The border is drawn OUTSIDE the bounds, so the caller leaves [px] of room around the element.
 * [pressed] shifts the whole element (fill + border) down by 2.dp.
 */
fun Modifier.pixelFrame(edge: Color, px: Dp = 4.dp, pressed: Boolean = false): Modifier =
    graphicsLayer { translationY = if (pressed) 2.dp.toPx() else 0f }
        .drawBehind {
            val p = px.toPx()
            drawRectSafe(edge, 0f, -p, size.width, p)
            drawRectSafe(edge, 0f, size.height, size.width, p)
            drawRectSafe(edge, -p, 0f, p, size.height)
            drawRectSafe(edge, size.width, 0f, p, size.height)
        }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRectSafe(
    color: Color,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
) = drawRect(
    color = color,
    topLeft = androidx.compose.ui.geometry.Offset(x, y),
    size = androidx.compose.ui.geometry.Size(w, h),
)

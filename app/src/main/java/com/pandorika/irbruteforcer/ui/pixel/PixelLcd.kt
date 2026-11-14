package com.pandorika.irbruteforcer.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandorika.irbruteforcer.ui.theme.PixelLcdBg
import com.pandorika.irbruteforcer.ui.theme.PixelLcdInk
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.TextUnit
import kotlin.math.floor

/** Amber LCD panel with a faint 3.dp dot-grid. */
@Composable
fun PixelLcd(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp)
            .background(PixelLcdBg)
            .drawBehind {
                val step = 3.dp.toPx()
                val line = Color.Black.copy(alpha = 0.06f)
                var x = 0f
                while (x < size.width) {
                    drawRect(line, Offset(x, 0f), Size(1f, size.height))
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawRect(line, Offset(0f, y), Size(size.width, 1f))
                    y += step
                }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun LcdRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
fun LcdText(text: String, size: TextUnit = 8.sp, modifier: Modifier = Modifier, maxLines: Int = 1) {
    PixelText(text, size, PixelLcdInk, modifier, maxLines)
}

/** Inverted tag, e.g. "НАЙДЕН". */
@Composable
fun LcdBadge(text: String) {
    PixelText(text, 8.sp, PixelLcdBg,Modifier.background(PixelLcdInk).padding(horizontal = 5.dp, vertical = 2.dp))
}

@Composable
fun LcdBlinkText(text: String, size: TextUnit = 8.sp) {
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            on = !on
        }
    }
    PixelText(text, size, PixelLcdInk, Modifier.alpha(if (on) 1f else 0f))
}

/** Dotted progress bar: 6.dp dashes with 2.dp gaps inside a 2.dp outline. [fraction] in 0..1. */
@Composable
fun LcdProgress(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val stroke = 2.dp.toPx()
        drawRect(PixelLcdInk, Offset.Zero, Size(size.width, stroke))
        drawRect(PixelLcdInk, Offset(0f, size.height - stroke), Size(size.width, stroke))
        drawRect(PixelLcdInk, Offset.Zero, Size(stroke, size.height))
        drawRect(PixelLcdInk, Offset(size.width - stroke, 0f), Size(stroke, size.height))
        val dash = 6.dp.toPx()
        val period = 8.dp.toPx()
        val filled = (size.width - 2 * stroke) * fraction.coerceIn(0f, 1f)
        val count = floor(filled / period).toInt()
        for (i in 0..count) {
            val x = stroke + i * period
            val w = minOf(dash, stroke + filled - x)
            if (w > 0f) drawRect(PixelLcdInk, Offset(x, stroke * 2), Size(w, size.height - stroke * 4))
        }
    }
}

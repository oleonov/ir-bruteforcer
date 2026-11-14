package com.pandorika.irbruteforcer.ui.pixel

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.pandorika.irbruteforcer.ui.theme.PixelFont

/** Single-line-by-default text in Press Start 2P. Glyphs are 1em wide: pick [size] so the label fits. */
@Composable
fun PixelText(
    text: String,
    size: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = PixelFont,
            fontWeight = FontWeight.Normal,
            fontSize = size,
            lineHeight = size * 1.5f,
            color = color,
            textAlign = textAlign ?: TextAlign.Unspecified,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

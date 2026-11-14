package com.pandorika.irbruteforcer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pandorika.irbruteforcer.R

/** Press Start 2P (OFL, full Cyrillic). The font has a single weight. */
val PixelFont = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))

private fun TextStyle.pixel() = copy(fontFamily = PixelFont, fontWeight = FontWeight.Normal)

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.pixel(),
    displayMedium = base.displayMedium.pixel(),
    displaySmall = base.displaySmall.pixel(),
    headlineLarge = base.headlineLarge.pixel(),
    headlineMedium = base.headlineMedium.pixel(),
    headlineSmall = base.headlineSmall.pixel(),
    titleLarge = base.titleLarge.pixel(),
    titleMedium = base.titleMedium.pixel(),
    titleSmall = base.titleSmall.pixel(),
    bodyLarge = base.bodyLarge.pixel(),
    bodyMedium = base.bodyMedium.pixel(),
    bodySmall = base.bodySmall.pixel(),
    labelLarge = base.labelLarge.pixel(),
    labelMedium = base.labelMedium.pixel(),
    labelSmall = base.labelSmall.pixel(),
)

package com.pandorika.irbruteforcer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PixelColorScheme = darkColorScheme(
    primary = PixelHot,
    onPrimary = PixelHotInk,
    secondary = PixelButtonInk,
    onSecondary = PixelHotInk,
    background = PixelBackground,
    onBackground = PixelButtonInk,
    surface = PixelShell,
    onSurface = PixelButtonInk,
    surfaceVariant = PixelButtonBg,
    onSurfaceVariant = PixelButtonInk,
)

/** Dark-only amber theme; no dynamic color. */
@Composable
fun IRBruteForcerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PixelColorScheme,
        typography = Typography,
        content = content,
    )
}

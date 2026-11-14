package com.pandorika.irbruteforcer.ui.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandorika.irbruteforcer.ui.theme.PixelButtonBg
import com.pandorika.irbruteforcer.ui.theme.PixelButtonEdge
import com.pandorika.irbruteforcer.ui.theme.PixelButtonInk
import com.pandorika.irbruteforcer.ui.theme.PixelHot
import com.pandorika.irbruteforcer.ui.theme.PixelHotInk

/** Corner badge: the scan size N, or a check mark + "1" when a code is saved. */
@Composable
fun PixelBadge(text: String, found: Boolean, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.alpha(0.6f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (found) PixelIcon(PixelBitmaps.Check, 1.dp, color)
        PixelText(text, 8.sp, color)
    }
}

/**
 * Pixel button. The frame is drawn 4.dp OUTSIDE the bounds; the caller leaves room.
 * [hot] = highlighted (active scan), [big] = larger icon/label.
 */
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    icon: List<String>? = null,
    badge: String? = null,
    badgeFound: Boolean = false,
    big: Boolean = false,
    hot: Boolean = false,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg = if (hot) PixelHot else PixelButtonBg
    val ink = if (hot) PixelHotInk else PixelButtonInk
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.35f)
            .pixelFrame(PixelButtonEdge, pressed = pressed && enabled)
            .background(bg)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            if (icon != null) PixelIcon(icon, if (big) 4.dp else 3.dp, ink)
            if (label.isNotEmpty()) {
                PixelText(label, if (big) 12.sp else 10.sp, ink, textAlign = TextAlign.Center)
            }
        }
        if (badge != null) {
            PixelBadge(
                text = badge,
                found = badgeFound,
                color = ink,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp),
            )
        }
    }
}

/** Compact footer button: icon and label side by side. */
@Composable
fun PixelFooterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    icon: List<String>? = null,
    hot: Boolean = false,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val ink = if (hot) PixelHotInk else PixelButtonInk
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.35f)
            .pixelFrame(PixelButtonEdge, pressed = pressed && enabled)
            .background(if (hot) PixelHot else PixelButtonBg)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            if (icon != null) PixelIcon(icon, 2.dp, ink)
            if (label.isNotEmpty()) PixelText(label, 10.sp, ink)
        }
    }
}

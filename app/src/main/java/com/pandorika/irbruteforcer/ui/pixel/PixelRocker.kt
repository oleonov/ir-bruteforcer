package com.pandorika.irbruteforcer.ui.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandorika.irbruteforcer.ui.theme.PixelButtonBg
import com.pandorika.irbruteforcer.ui.theme.PixelButtonEdge
import com.pandorika.irbruteforcer.ui.theme.PixelButtonInk
import com.pandorika.irbruteforcer.ui.theme.PixelHot
import com.pandorika.irbruteforcer.ui.theme.PixelHotInk

/** One half of a rocker (own click target, own badge). */
data class RockerHalf(
    val icon: List<String>,
    val badge: String?,
    val badgeFound: Boolean,
    val hot: Boolean,
    val onClick: () -> Unit,
)

/** Vertical rocker: upper half, middle label, lower half. Frame is drawn 4.dp outside the bounds. */
@Composable
fun PixelRocker(
    label: String,
    up: RockerHalf,
    down: RockerHalf,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.35f)
            .pixelFrame(PixelButtonEdge)
            .background(PixelButtonBg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RockerHalfBox(up, enabled, Modifier.weight(1f))
        PixelText(label, 9.sp, PixelButtonInk.copy(alpha = 0.8f), Modifier.padding(vertical = 3.dp))
        RockerHalfBox(down, enabled, Modifier.weight(1f))
    }
}

@Composable
private fun RockerHalfBox(half: RockerHalf, enabled: Boolean, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val ink: Color = if (half.hot) PixelHotInk else PixelButtonInk
    val bg = when {
        half.hot -> PixelHot
        pressed && enabled -> PixelButtonInk.copy(alpha = 0.25f)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = half.onClick),
        contentAlignment = Alignment.Center,
    ) {
        PixelIcon(half.icon, 3.dp, ink)
        if (half.badge != null) {
            PixelBadge(
                text = half.badge,
                found = half.badgeFound,
                color = ink,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp),
            )
        }
    }
}

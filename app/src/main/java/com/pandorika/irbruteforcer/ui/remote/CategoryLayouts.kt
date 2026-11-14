package com.pandorika.irbruteforcer.ui.remote

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandorika.irbruteforcer.R
import com.pandorika.irbruteforcer.remote.RemoteCategory
import com.pandorika.irbruteforcer.remote.RemoteMode
import com.pandorika.irbruteforcer.remote.RemoteUiState
import com.pandorika.irbruteforcer.ui.pixel.PixelBadge
import com.pandorika.irbruteforcer.ui.pixel.PixelBitmaps
import com.pandorika.irbruteforcer.ui.pixel.PixelButton
import com.pandorika.irbruteforcer.ui.pixel.PixelRocker
import com.pandorika.irbruteforcer.ui.pixel.RockerHalf
import com.pandorika.irbruteforcer.ui.pixel.bitmap
import com.pandorika.irbruteforcer.ui.pixel.pixelFrame
import com.pandorika.irbruteforcer.ui.theme.PixelButtonEdge
import com.pandorika.irbruteforcer.ui.theme.PixelHot

private val BigRow = 84.dp
private val NormalRow = 64.dp
private val RockerRow = 136.dp
private val Gap = 14.dp

/**
 * Everything a layout needs: current state + press callback. Presses carry the category this pad draws,
 * so a tap on a stale (not yet recomposed) pad never scans the newly selected category.
 */
internal class PadScope(val state: RemoteUiState, private val onPress: (RemoteCategory, String) -> Unit) {
    val enabled: Boolean get() = state.hasEmitter && !state.isLoading && state.error == null

    fun press(id: String) = onPress(state.category, id)

    fun isHot(id: String): Boolean = when (val mode = state.mode) {
        is RemoteMode.Scanning -> mode.buttonId == id
        RemoteMode.Idle -> false
    }

    fun badge(id: String): String? = state.buttonCounts[id]?.toString()
}

@Composable
fun CategoryPad(state: RemoteUiState, onPress: (RemoteCategory, String) -> Unit, modifier: Modifier = Modifier) {
    val scope = PadScope(state, onPress)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Gap)) {
        with(scope) {
            when (state.category) {
                RemoteCategory.Ac -> AcPad()
                RemoteCategory.Tv -> TvPad()
                RemoteCategory.Projector -> ProjectorPad()
                RemoteCategory.Audio -> AudioPad()
                RemoteCategory.Fan -> FanPad()
                RemoteCategory.Led -> LedPad()
                RemoteCategory.Disc -> DiscPad()
                RemoteCategory.Monitor -> MonitorPad()
            }
        }
    }
}

@Composable
private fun PadScope.AcPad() {
    PadRow(BigRow) { Btn("off", Modifier.weight(1f), big = true) }
    PadRow(NormalRow) { Btn("cool_hi", Modifier.weight(1f)); Btn("heat_hi", Modifier.weight(1f)) }
    PadRow(NormalRow) { Btn("cool_lo", Modifier.weight(1f)); Btn("heat_lo", Modifier.weight(1f)) }
    PadRow(NormalRow) { Btn("dry", Modifier.weight(1f)) }
}

@Composable
private fun PadScope.TvPad() {
    PadRow(BigRow) { Btn("power", Modifier.weight(1f), big = true) }
    PadRow(RockerRow) {
        Rocker(R.string.rocker_volume, "vol_up", "vol_dn", Modifier.weight(1f))
        Col { Btn("mute", Modifier.weight(1f)) }
        Rocker(R.string.rocker_channel, "ch_next", "ch_prev", Modifier.weight(1f))
    }
}

@Composable
private fun PadScope.ProjectorPad() {
    PadRow(BigRow) { Btn("power", Modifier.weight(1f), big = true) }
    PadRow(RockerRow) {
        Rocker(R.string.rocker_volume, "vol_up", "vol_dn", Modifier.weight(1f))
        Col { Btn("mute", Modifier.weight(1f)) }
        Col { Btn("play", Modifier.weight(1f)); Btn("pause", Modifier.weight(1f)) }
    }
}

@Composable
private fun PadScope.AudioPad() {
    PadRow(BigRow) { Btn("power", Modifier.weight(1f), big = true); Btn("mute", Modifier.weight(1f), big = true) }
    PadRow(RockerRow) {
        Rocker(R.string.rocker_volume, "vol_up", "vol_dn", Modifier.weight(1f))
        Col { Btn("play", Modifier.weight(1f)); Btn("pause", Modifier.weight(1f)) }
    }
    PadRow(NormalRow) { Btn("prev", Modifier.weight(1f)); Btn("next", Modifier.weight(1f)) }
}

@Composable
private fun PadScope.FanPad() {
    PadRow(BigRow) { Btn("power", Modifier.weight(1f), big = true) }
    PadRow(RockerRow) {
        Rocker(R.string.rocker_speed, "speed_up", "speed_dn", Modifier.weight(1f))
        Col { Btn("rotate", Modifier.weight(1f)); Btn("mode", Modifier.weight(1f)) }
    }
    PadRow(NormalRow) { Btn("timer", Modifier.weight(1f)) }
}

@Composable
private fun PadScope.LedPad() {
    PadRow(BigRow) { Btn("power_on", Modifier.weight(1f), big = true); Btn("power_off", Modifier.weight(1f), big = true) }
    PadRow(RockerRow) {
        Rocker(R.string.rocker_brightness, "bright_up", "bright_dn", Modifier.weight(1f))
        Col {
            Swatch("red", Color(0xFFEE3333), Color.Black, Modifier.weight(1f))
            Swatch("green", Color(0xFF33CC33), Color.Black, Modifier.weight(1f))
        }
        Col {
            Swatch("blue", Color(0xFF3366FF), Color.White, Modifier.weight(1f))
            Swatch("white", Color(0xFFFFFFFF), Color.Black, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PadScope.DiscPad() {
    PadRow(BigRow) { Btn("power", Modifier.weight(1f), big = true); Btn("eject", Modifier.weight(1f), big = true) }
    PadRow(NormalRow) {
        Btn("rewind", Modifier.weight(1f)); Btn("play", Modifier.weight(1f))
        Btn("pause", Modifier.weight(1f)); Btn("forward", Modifier.weight(1f))
    }
    PadRow(NormalRow) { Btn("ok", Modifier.weight(1f)); Btn("subtitle", Modifier.weight(1f)) }
}

@Composable
private fun PadScope.MonitorPad() {
    PadRow(BigRow) { Btn("power", Modifier.weight(1f), big = true) }
    PadRow(NormalRow) { Btn("source", Modifier.weight(1f)); Btn("menu", Modifier.weight(1f)); Btn("exit", Modifier.weight(1f)) }
}

@Composable
private fun PadRow(height: Dp, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(Gap), content = content)
}

@Composable
private fun RowScope.Col(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Gap), content = content)
}

@Composable
private fun PadScope.Btn(id: String, modifier: Modifier, big: Boolean = false) {
    val button = state.category.button(id) ?: return
    PixelButton(
        onClick = { press(id) },
        modifier = modifier.fillMaxSize(),
        label = button.labelRes?.let { stringResource(it) }.orEmpty(),
        icon = button.icon.bitmap(),
        badge = badge(id),
        big = big,
        hot = isHot(id),
        enabled = enabled,
    )
}

@Composable
private fun PadScope.Rocker(@StringRes label: Int, upId: String, downId: String, modifier: Modifier) {
    fun half(id: String) = RockerHalf(
        icon = state.category.button(id)!!.icon.bitmap(),
        badge = badge(id),
        badgeFound = false,
        hot = isHot(id),
        onClick = { press(id) },
    )
    PixelRocker(stringResource(label), half(upId), half(downId), enabled, modifier.fillMaxSize())
}

@Composable
private fun PadScope.Swatch(id: String, color: Color, badgeColor: Color, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.35f)
            .pixelFrame(if (isHot(id)) PixelHot else PixelButtonEdge, pressed = pressed && enabled)
            .background(color)
            .clickable(interactionSource = source, indication = null, enabled = enabled) { press(id) },
    ) {
        badge(id)?.let {
            PixelBadge(it, false, badgeColor, Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp))
        }
    }
}

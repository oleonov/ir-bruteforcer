package com.pandorika.irbruteforcer.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandorika.irbruteforcer.remote.RemoteCategory
import com.pandorika.irbruteforcer.remote.RemoteMessage
import com.pandorika.irbruteforcer.remote.RemoteMode
import com.pandorika.irbruteforcer.remote.RemoteUiState
import com.pandorika.irbruteforcer.R
import com.pandorika.irbruteforcer.ui.pixel.DevicePickerDialog
import com.pandorika.irbruteforcer.ui.pixel.LcdBlinkText
import com.pandorika.irbruteforcer.ui.pixel.LcdProgress
import com.pandorika.irbruteforcer.ui.pixel.LcdRow
import com.pandorika.irbruteforcer.ui.pixel.LcdText
import com.pandorika.irbruteforcer.ui.pixel.PixelBitmaps
import com.pandorika.irbruteforcer.ui.pixel.PixelFooterButton
import com.pandorika.irbruteforcer.ui.pixel.PixelIcon
import com.pandorika.irbruteforcer.ui.pixel.PixelLcd
import com.pandorika.irbruteforcer.ui.pixel.PixelText
import com.pandorika.irbruteforcer.ui.pixel.bitmap
import com.pandorika.irbruteforcer.ui.pixel.pixelFrame
import com.pandorika.irbruteforcer.ui.theme.IRBruteForcerTheme
import com.pandorika.irbruteforcer.ui.theme.PixelBackground
import com.pandorika.irbruteforcer.ui.theme.PixelButtonEdge
import com.pandorika.irbruteforcer.ui.theme.PixelButtonInk
import com.pandorika.irbruteforcer.ui.theme.PixelLcdDim
import com.pandorika.irbruteforcer.ui.theme.PixelLcdInk
import com.pandorika.irbruteforcer.ui.theme.PixelShell
import java.util.Locale

/** All user intents of the screen; defaults are no-ops (previews). */
class RemoteActions(
    val selectCategory: (RemoteCategory) -> Unit = {},
    val press: (RemoteCategory, String) -> Unit = { _, _ -> },
    val stop: () -> Unit = {},
    val confirmWorked: () -> Unit = {},
    val reset: (RemoteCategory) -> Unit = {},
    val cycleDelay: (Int) -> Unit = {},
)

@Composable
fun RemoteScreen(
    state: RemoteUiState,
    actions: RemoteActions,
    modifier: Modifier = Modifier,
    menuInitiallyOpen: Boolean = false,
) {
    var menuOpen by rememberSaveable { mutableStateOf(menuInitiallyOpen) }
    val counts = state.libraryTotals

    Column(
        modifier
            .fillMaxSize()
            .background(PixelBackground)
            .safeDrawingPadding(),
    ) {
        DeviceBar(
            selected = state.category,
            count = counts[state.category],
            onClick = { menuOpen = true },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
        PixelLcd(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) { LcdContent(state) }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            CategoryPad(state, actions.press)
        }
        Footer(state, actions)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelIcon(PixelBitmaps.Sun, 1.dp, PixelLcdDim)
            PixelText("  " + stringResource(R.string.footer_screen_on), 8.sp, PixelLcdDim)
        }
    }
    if (menuOpen) {
        DevicePickerDialog(
            selected = state.category,
            counts = counts,
            onSelect = actions.selectCategory,
            onDismiss = { menuOpen = false },
        )
    }
}

/** Trigger of the device picker: current device icon, title, code count and a boxed chevron. */
@Composable
private fun DeviceBar(selected: RemoteCategory, count: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pixelFrame(PixelButtonEdge)
            .background(PixelShell)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PixelIcon(selected.icon.bitmap(), 2.dp, PixelButtonInk)
        PixelText(stringResource(selected.titleRes), 10.sp, PixelButtonInk, Modifier.weight(1f))
        count?.let { PixelText(it.toString(), 8.sp, PixelButtonInk.copy(alpha = 0.7f)) }
        Box(Modifier.pixelFrame(PixelButtonEdge, 2.dp).padding(5.dp)) {
            PixelIcon(PixelBitmaps.Chevron, 2.dp, PixelButtonInk)
        }
    }
}

@Composable
private fun ColumnScope.LcdContent(state: RemoteUiState) {
    val category = state.category
    when {
        state.error != null -> {
            LcdText(stringResource(R.string.lcd_error), 14.sp)
            LcdText(state.error, 8.sp, maxLines = 3)
        }
        state.isLoading -> LcdText(stringResource(R.string.lcd_loading), 14.sp)
        !state.hasEmitter -> {
            LcdText(stringResource(R.string.lcd_no_emitter), 12.sp, maxLines = 2)
            LcdText(stringResource(R.string.lcd_no_emitter_hint), 8.sp, maxLines = 2)
        }
        else -> when (val mode = state.mode) {
            RemoteMode.Idle -> {
                LcdRow {
                    LcdText(stringResource(category.shortTitleRes), 14.sp)
                    LcdText(stringResource(R.string.lcd_ready))
                }
                val total = state.libraryTotals[category]
                when {
                    state.cursor > 0 -> LcdText(stringResource(R.string.lcd_cursor, state.cursor + 1, total ?: 0), 8.sp)
                    // Feedback for СБРОС: the button is gone, so the LCD says it loudly.
                    state.message == RemoteMessage.CursorReset ->
                        LcdText(stringResource(R.string.lcd_reset_from_start), 10.sp)
                    else -> LcdText(stringResource(R.string.lcd_from_start), 8.sp)
                }
                LcdText(stringResource(R.string.lcd_hint), 8.sp, maxLines = 2)
                (total ?: state.buttonCounts.values.sum()).let {
                    LcdText(pluralStringResource(R.plurals.codes_in_library, it, it), 8.sp)
                }
            }
            is RemoteMode.Scanning -> {
                val button = category.button(mode.buttonId)
                val nameRes = button?.labelRes ?: button?.nameRes
                val name = if (nameRes != null) stringResource(nameRes) else mode.buttonId.uppercase()
                LcdRow {
                    LcdText(name, 14.sp)
                    LcdText("${mode.index + 1}/${mode.total}")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PixelIcon(PixelBitmaps.Play, 1.dp, PixelLcdInk)
                    LcdText(mode.label ?: "-", 8.sp)
                }
                LcdProgress((mode.index + 1).toFloat() / mode.total.coerceAtLeast(1))
                LcdRow {
                    LcdBlinkText(stringResource(R.string.lcd_scanning))
                    LcdText("~" + formatRemaining(mode.remainingMillis))
                }
            }
        }
    }
    state.message?.let { LcdText(messageText(it), 8.sp, maxLines = 2) }
}

@Composable
private fun messageText(message: RemoteMessage): String = when (message) {
    RemoteMessage.Stopped -> stringResource(R.string.message_stopped)
    RemoteMessage.NoCodes -> stringResource(R.string.message_no_codes)
    is RemoteMessage.CursorSet -> stringResource(R.string.message_cursor_set, message.position)
    RemoteMessage.CursorReset -> stringResource(R.string.message_cursor_reset)
    is RemoteMessage.ScanDone -> stringResource(R.string.message_scan_done) +
        if (message.failed > 0) " · " + pluralStringResource(R.plurals.message_not_sent, message.failed, message.failed) else ""
    is RemoteMessage.SendFailed -> pluralStringResource(R.plurals.message_not_sent, message.count, message.count)
    is RemoteMessage.Error -> message.reason
}

private fun formatRemaining(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

@Composable
private fun Footer(state: RemoteUiState, actions: RemoteActions) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (state.mode) {
            RemoteMode.Idle -> Row(
                Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // СБРОС only exists while there is a cursor to reset; otherwise the speed takes the row.
                val canReset = state.cursor > 0
                SpeedControl(state.delayMillis, actions.cycleDelay, Modifier.weight(if (canReset) 1.3f else 1f))
                if (canReset) {
                    PixelFooterButton(
                        onClick = { actions.reset(state.category) },
                        modifier = Modifier.weight(0.8f).fillMaxSize(),
                        label = stringResource(R.string.footer_reset),
                        icon = PixelBitmaps.X,
                    )
                }
            }
            is RemoteMode.Scanning -> Row(
                Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PixelFooterButton(actions.stop, Modifier.weight(0.8f).fillMaxSize(), stringResource(R.string.footer_stop), PixelBitmaps.Stop)
                PixelFooterButton(
                    actions.confirmWorked,
                    Modifier.weight(1.6f).fillMaxSize(),
                    stringResource(R.string.footer_worked),
                    PixelBitmaps.Check,
                    hot = true,
                )
            }
        }
    }
}

@Composable
private fun SpeedControl(delayMillis: Long, onCycle: (Int) -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxSize()
            .pixelFrame(PixelButtonEdge)
            .background(PixelShell),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.clickable { onCycle(-1) }.padding(horizontal = 8.dp, vertical = 12.dp)) {
            PixelIcon(PixelBitmaps.Left, 1.dp, PixelButtonInk)
        }
        PixelText(stringResource(R.string.footer_speed, String.format(Locale.US, "%.2f", delayMillis / 1000.0)), 8.sp, PixelButtonInk)
        Box(Modifier.clickable { onCycle(+1) }.padding(horizontal = 8.dp, vertical = 12.dp)) {
            PixelIcon(PixelBitmaps.Play, 1.dp, PixelButtonInk)
        }
    }
}

// ---- Previews ----

private val acCounts = mapOf("off" to 139, "cool_hi" to 37, "heat_hi" to 33, "cool_lo" to 38, "heat_lo" to 32, "dry" to 43)
private val tvCounts = mapOf(
    "power" to 324, "vol_up" to 99, "vol_dn" to 104, "mute" to 93, "ch_next" to 94, "ch_prev" to 101,
)

private fun previewState(category: RemoteCategory, counts: Map<String, Int>) = RemoteUiState(
    category = category,
    isLoading = false,
    hasEmitter = true,
    buttonCounts = counts,
    libraryTotals = mapOf(
        RemoteCategory.Ac to 322, RemoteCategory.Tv to 815, RemoteCategory.Projector to 303,
        RemoteCategory.Audio to 1020, RemoteCategory.Fan to 421, RemoteCategory.Led to 611,
        RemoteCategory.Disc to 359, RemoteCategory.Monitor to 24,
    ),
)

@Preview(widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun AcIdlePreview() {
    IRBruteForcerTheme { RemoteScreen(previewState(RemoteCategory.Ac, acCounts), RemoteActions()) }
}

@Preview(widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun AcScanningPreview() {
    val state = previewState(RemoteCategory.Ac, acCounts).copy(
        mode = RemoteMode.Scanning("off", 36, 139, "Daikin FTXS25", 67_000),
    )
    IRBruteForcerTheme { RemoteScreen(state, RemoteActions()) }
}

@Preview(widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun TvCursorPreview() {
    val state = previewState(RemoteCategory.Tv, tvCounts).copy(cursor = 120)
    IRBruteForcerTheme { RemoteScreen(state, RemoteActions()) }
}

@Preview(widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun AcResetDonePreview() {
    val state = previewState(RemoteCategory.Ac, acCounts).copy(message = RemoteMessage.CursorReset)
    IRBruteForcerTheme { RemoteScreen(state, RemoteActions()) }
}

@Preview(widthDp = 360, heightDp = 760, showBackground = true, locale = "ru")
@Composable
private fun AcResetDoneRuPreview() {
    val state = previewState(RemoteCategory.Ac, acCounts).copy(message = RemoteMessage.CursorReset)
    IRBruteForcerTheme { RemoteScreen(state, RemoteActions()) }
}

@Preview(widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun PickerOpenPreview() {
    IRBruteForcerTheme {
        RemoteScreen(previewState(RemoteCategory.Tv, tvCounts), RemoteActions(), menuInitiallyOpen = true)
    }
}

@Preview(widthDp = 360, heightDp = 760, showBackground = true, locale = "ru")
@Composable
private fun AcIdleRuPreview() {
    IRBruteForcerTheme { RemoteScreen(previewState(RemoteCategory.Ac, acCounts), RemoteActions()) }
}

@Preview(widthDp = 360, heightDp = 760, showBackground = true, locale = "ru")
@Composable
private fun FanScanningRuPreview() {
    val state = previewState(RemoteCategory.Fan, emptyMap()).copy(
        mode = RemoteMode.Scanning("rotate", 12, 400, "Dyson", 120_000),
        message = RemoteMessage.SendFailed(3),
    )
    IRBruteForcerTheme { RemoteScreen(state, RemoteActions()) }
}

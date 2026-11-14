package com.pandorika.irbruteforcer.ui.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pandorika.irbruteforcer.R
import com.pandorika.irbruteforcer.remote.RemoteCategory
import com.pandorika.irbruteforcer.ui.theme.IRBruteForcerTheme
import com.pandorika.irbruteforcer.ui.theme.PixelButtonEdge
import com.pandorika.irbruteforcer.ui.theme.PixelButtonInk
import com.pandorika.irbruteforcer.ui.theme.PixelHot
import com.pandorika.irbruteforcer.ui.theme.PixelHotInk
import com.pandorika.irbruteforcer.ui.theme.PixelShell

/**
 * Modal device picker: a centered pixel-framed panel over the platform scrim. Pinned categories first,
 * a dotted divider, then the rest. [counts] holds the code count per category when known.
 * Tapping a row selects and dismisses; back press, tapping outside and the ✕ control dismiss.
 */
@Composable
fun DevicePickerDialog(
    selected: RemoteCategory,
    counts: Map<RemoteCategory, Int>,
    onSelect: (RemoteCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        DevicePickerContent(selected, counts, onSelect, onDismiss)
    }
}

@Composable
internal fun DevicePickerContent(
    selected: RemoteCategory,
    counts: Map<RemoteCategory, Int>,
    onSelect: (RemoteCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    // Outer padding leaves room for the 4.dp frame, which is drawn outside its bounds.
    Box(Modifier.padding(4.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .pixelFrame(PixelButtonInk)
                .background(PixelShell),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PixelText(stringResource(R.string.picker_title), 9.sp, PixelButtonInk, Modifier.weight(1f))
                Box(Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 12.dp)) {
                    PixelIcon(PixelBitmaps.X, 2.dp, PixelButtonInk)
                }
            }
            DottedDivider(Modifier.padding(horizontal = 12.dp))
            Column(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
            ) {
                val pinned = RemoteCategory.entries.filter { it.pinned }
                val rest = RemoteCategory.entries.filterNot { it.pinned }
                pinned.forEach { PickerRow(it, it == selected, counts[it]) { onSelect(it); onDismiss() } }
                DottedDivider(Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                rest.forEach { PickerRow(it, it == selected, counts[it]) { onSelect(it); onDismiss() } }
            }
        }
    }
}

@Composable
private fun PickerRow(category: RemoteCategory, selected: Boolean, count: Int?, onClick: () -> Unit) {
    val ink = if (selected) PixelHotInk else PixelButtonInk
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) PixelHot else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PixelIcon(category.icon.bitmap(), 2.dp, ink)
        PixelText(stringResource(category.titleRes), 9.sp, ink, Modifier.weight(1f))
        if (count != null) PixelText(count.toString(), 8.sp, ink.copy(alpha = 0.6f))
    }
}

@Composable
private fun DottedDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .drawBehind {
                val dash = 4.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawRect(PixelButtonInk.copy(alpha = 0.5f), Offset(x, 0f), Size(minOf(dash, size.width - x), size.height))
                    x += dash * 2
                }
            },
    )
}

private val previewCounts = mapOf(
    RemoteCategory.Ac to 322, RemoteCategory.Tv to 815, RemoteCategory.Projector to 303,
    RemoteCategory.Audio to 1020, RemoteCategory.Fan to 421, RemoteCategory.Led to 611,
    RemoteCategory.Disc to 359, RemoteCategory.Monitor to 24,
)

@Preview(widthDp = 312, showBackground = true)
@Composable
private fun DevicePickerEnPreview() {
    IRBruteForcerTheme { DevicePickerContent(RemoteCategory.Tv, previewCounts, {}, {}) }
}

@Preview(widthDp = 312, showBackground = true, locale = "ru")
@Composable
private fun DevicePickerRuPreview() {
    IRBruteForcerTheme { DevicePickerContent(RemoteCategory.Audio, previewCounts, {}, {}) }
}

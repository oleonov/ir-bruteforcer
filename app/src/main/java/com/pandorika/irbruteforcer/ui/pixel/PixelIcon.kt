package com.pandorika.irbruteforcer.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.pandorika.irbruteforcer.remote.ButtonIcon
import kotlin.math.roundToInt

/** 9x9 bitmaps ported from the approved mockup. '#' = lit pixel. */
object PixelBitmaps {
    val Power = listOf("....#....", ".#..#..#.", "#...#...#", "#...#...#", "#.......#", "#.......#", ".#.....#.", "..#####..", ".........")
    val Snow = listOf("....#....", ".#..#..#.", "..#.#.#..", "...###...", "#########", "...###...", "..#.#.#..", ".#..#..#.", "....#....")
    val Sun = listOf("....#....", ".#.....#.", "...###...", "..#####..", "#.#####.#", "..#####..", "...###...", ".#.....#.", "....#....")
    val Drop = listOf("....#....", "....#....", "...###...", "..#####..", ".###.###.", ".##.####.", ".#######.", "..#####..", "...###...")
    val Fan = listOf("..###....", ".####....", "..###.##.", "....####.", ".##.#.##.", ".####....", "....###..", "....####.", "....###..")
    val Mute = listOf("...#.....", "..##.....", "####.#..#", "####..##.", "####..##.", "####.#..#", "..##.....", "...#.....", ".........")
    val Up = listOf("....#....", "...###...", "..#####..", ".#######.", "#########", "...###...", "...###...", "...###...", ".........")
    val Plus = listOf(".........", "...###...", "...###...", "#########", "#########", "#########", "...###...", "...###...", ".........")
    val Minus = listOf(".........", ".........", ".........", "#########", "#########", "#########", ".........", ".........", ".........")
    val Play = listOf("#........", "###......", "#####....", "#######..", "#########", "#######..", "#####....", "###......", "#........")
    val Pause = listOf(".##...##.", ".##...##.", ".##...##.", ".##...##.", ".##...##.", ".##...##.", ".##...##.", ".##...##.", ".........")
    val Next = listOf("#...#..##", "##..##.##", "###.#####", "#########", "###.#####", "##..##.##", "#...#..##", ".........", ".........")
    val FastForward = listOf("#...#....", "##..##...", "###.###..", "########.", "#########", "########.", "###.###..", "##..##...", "#...#....")
    val Bulb = listOf("..#####..", ".#.....#.", "#...#...#", "#..#....#", ".#.....#.", "..#...#..", "..#####..", "..#####..", "...###...")
    val Eject = listOf("....#....", "...###...", "..#####..", ".#######.", "#########", ".........", "#########", "#########", ".........")
    val Timer = listOf("..#####..", ".#..#..#.", "#...#...#", "#...#...#", "#...###.#", "#.......#", ".#.....#.", "..#####..", ".........")
    val Rotate = listOf(".........", ".........", "..#...#..", ".##...##.", "#########", ".##...##.", "..#...#..", ".........", ".........")
    val Mode = listOf("#########", "#.......#", "#.##.##.#", "#.......#", "#.##.##.#", "#.......#", "#########", ".........", ".........")
    val Tv = listOf("#########", "#.......#", "#.......#", "#.......#", "#.......#", "#########", "...#.#...", "..#####..", ".........")
    val Ac = listOf("#########", "#.......#", "#.......#", "#########", ".#.#.#.#.", "..#.#.#..", ".#.#.#.#.", ".........", ".........")
    val Projector = listOf(".........", "######...", "#....#.##", "#.##.####", "#.##.####", "#....#.##", "######...", ".#....#..", ".........")
    val Speaker = listOf(".#######.", ".#..#..#.", ".#.....#.", ".#.###.#.", ".#.#.#.#.", ".#.###.#.", ".#.....#.", ".#######.", ".........")
    val Monitor = listOf("#########", "#.......#", "#.......#", "#.......#", "#########", "....#....", "....#....", "..#####..", ".........")
    val Disc = listOf("..#####..", ".#.....#.", "#..###..#", "#..#.#..#", "#..###..#", ".#.....#.", "..#####..", ".........", ".........")
    val Check = listOf("........#", ".......##", "......##.", "#....##..", "##..##...", ".####....", "..##.....", ".........", ".........")
    val X = listOf("#.......#", ".#.....#.", "..#...#..", "...#.#...", "....#....", "...#.#...", "..#...#..", ".#.....#.", "#.......#")
    val Menu = listOf("#########", ".........", ".........", "#########", ".........", ".........", "#########", ".........", ".........")
    val Source = listOf("....#....", "....##...", "########.", "#########", "########.", "....##...", "....#....", ".........", ".........")
    val Subtitle = Mode
    val Ok = listOf("..#####..", ".#.....#.", "#..###..#", "#.#...#.#", "#.#...#.#", "#..###..#", ".#.....#.", "..#####..", ".........")
    val Stop = List(9) { "#########" }
    /** Open "V": the device picker trigger. */
    val Chevron = listOf(".........", ".........", "#.......#", "##.....##", ".##...##.", "..##.##..", "...###...", "....#....", ".........")
    val Heat = listOf("....#....", "...##....", "...###...", "..####...", "..#####..", ".###.##..", ".##..###.", ".##...##.", "..#####..")

    private fun mirror(a: List<String>) = a.map { it.reversed() }

    val Prev = mirror(Next)
    val Rewind = mirror(FastForward)
    val Down = Up.reversed()
    val Left = mirror(Play)
}

fun ButtonIcon.bitmap(): List<String> = when (this) {
    ButtonIcon.Power -> PixelBitmaps.Power
    ButtonIcon.Snow -> PixelBitmaps.Snow
    ButtonIcon.Heat -> PixelBitmaps.Heat
    ButtonIcon.Drop -> PixelBitmaps.Drop
    ButtonIcon.Fan -> PixelBitmaps.Fan
    ButtonIcon.Plus -> PixelBitmaps.Plus
    ButtonIcon.Minus -> PixelBitmaps.Minus
    ButtonIcon.Up -> PixelBitmaps.Up
    ButtonIcon.Down -> PixelBitmaps.Down
    ButtonIcon.Mute -> PixelBitmaps.Mute
    ButtonIcon.Play -> PixelBitmaps.Play
    ButtonIcon.Pause -> PixelBitmaps.Pause
    ButtonIcon.Next -> PixelBitmaps.Next
    ButtonIcon.Prev -> PixelBitmaps.Prev
    ButtonIcon.FastForward -> PixelBitmaps.FastForward
    ButtonIcon.Rewind -> PixelBitmaps.Rewind
    ButtonIcon.Bulb -> PixelBitmaps.Bulb
    ButtonIcon.Eject -> PixelBitmaps.Eject
    ButtonIcon.Timer -> PixelBitmaps.Timer
    ButtonIcon.Rotate -> PixelBitmaps.Rotate
    ButtonIcon.Mode -> PixelBitmaps.Mode
    ButtonIcon.Ok -> PixelBitmaps.Ok
    ButtonIcon.Subtitle -> PixelBitmaps.Subtitle
    ButtonIcon.Source -> PixelBitmaps.Source
    ButtonIcon.Menu -> PixelBitmaps.Menu
    ButtonIcon.Exit -> PixelBitmaps.X
    ButtonIcon.Ac -> PixelBitmaps.Ac
    ButtonIcon.Tv -> PixelBitmaps.Tv
    ButtonIcon.Projector -> PixelBitmaps.Projector
    ButtonIcon.Speaker -> PixelBitmaps.Speaker
    ButtonIcon.Monitor -> PixelBitmaps.Monitor
    ButtonIcon.Disc -> PixelBitmaps.Disc
}

/** Draws [bitmap] with every pixel as a whole-device-pixel square (no anti-aliasing). [scale] = dp per bitmap pixel. */
@Composable
fun PixelIcon(bitmap: List<String>, scale: Dp, color: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val cell = with(density) { scale.toPx() }.roundToInt().coerceAtLeast(1)
    val side = with(density) { (cell * 9).toDp() }
    Canvas(modifier.size(side)) {
        val cellSize = Size(cell.toFloat(), cell.toFloat())
        bitmap.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch == '#') {
                    drawRect(color, Offset((x * cell).toFloat(), (y * cell).toFloat()), cellSize)
                }
            }
        }
    }
}

package com.pandorika.irbruteforcer

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** The launcher vectors are generated from [MAP]; this keeps them pixel-exact and inside the adaptive safe zone. */
class LauncherIconTest {

    private val map = listOf(
        "............",
        ".....AA.....",
        "..A......A..",
        "...A....A...",
        ".....RR.....",
        "....RRRR....",
        "....HRRR....",
        "....HRRR....",
        "...rrrrrr...",
        "....W..W....",
        "....W..W....",
        "............",
    )
    private val colors = mapOf(
        "#FFF0A020" to 'A', "#FFE8452C" to 'R', "#FFFFD27A" to 'H', "#FF8A2A1A" to 'r', "#FF9A6512" to 'W',
    )
    private val res = File("src/main/res")
    private val android = "http://schemas.android.com/apk/res/android"

    private fun paths(name: String): List<Pair<String, String>> {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File(res, "drawable/$name.xml"))
        val root = doc.documentElement
        assertEquals("108", root.getAttributeNS(android, "viewportWidth"))
        assertEquals("108", root.getAttributeNS(android, "viewportHeight"))
        val nodes = root.getElementsByTagName("path")
        return (0 until nodes.length).map {
            val e = nodes.item(it) as Element
            e.getAttributeNS(android, "fillColor") to e.getAttributeNS(android, "pathData")
        }
    }

    /** Every subpath must be one 5x5 square on the 5-unit grid that starts at (24,24); returns grid cells. */
    private fun cells(pathData: String): List<Pair<Int, Int>> {
        val squares = Regex("""M(\d+),(\d+)h5v5h-5z""").findAll(pathData).toList()
        assertEquals("only grid squares in $pathData", pathData, squares.joinToString("") { it.value })
        return squares.map {
            val x = it.groupValues[1].toInt() - ORIGIN
            val y = it.groupValues[2].toInt() - ORIGIN
            assertTrue(x % CELL == 0 && y % CELL == 0)
            x / CELL to y / CELL
        }
    }

    private fun render(paths: List<Pair<String, String>>, color: (String) -> Char): List<String> {
        val grid = Array(12) { CharArray(12) { '.' } }
        for ((fill, data) in paths) {
            for ((x, y) in cells(data)) {
                assertEquals("cell $x,$y painted twice", '.', grid[y][x])
                grid[y][x] = color(fill)
            }
        }
        return grid.map { String(it) }
    }

    @Test fun foregroundMatchesThePixelMapAndColours() {
        val fg = paths("ic_launcher_foreground")
        assertEquals(colors.keys, fg.map { it.first }.toSet())
        assertEquals(map, render(fg) { colors.getValue(it) })
    }

    @Test fun monochromeIsEveryLitCellExceptTheHighlight() {
        val mono = paths("ic_launcher_monochrome")
        assertEquals(listOf("#FFFFFFFF"), mono.map { it.first })
        val expected = map.map { row -> row.map { if (it == '.' || it == 'H') '.' else '#' }.joinToString("") }
        assertEquals(expected, render(mono) { '#' })
    }

    @Test fun backgroundIsSolidDark() {
        assertEquals(listOf("#FF17120B" to "M0,0h108v108h-108z"), paths("ic_launcher_background"))
    }

    @Test fun litPixelsStayInsideTheSafeZone() {
        for (name in listOf("ic_launcher_foreground", "ic_launcher_monochrome")) {
            for ((_, data) in paths(name)) {
                for ((x, y) in cells(data)) {
                    for ((cx, cy) in listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1)) {
                        val px = ORIGIN + (x + cx) * CELL
                        val py = ORIGIN + (y + cy) * CELL
                        assertTrue("$name cell $x,$y", hypot(px - 54.0, py - 54.0) <= SAFE_RADIUS)
                    }
                }
            }
        }
    }

    @Test fun adaptiveIconsUseAllThreeLayers() {
        for (name in listOf("ic_launcher", "ic_launcher_round")) {
            val text = File(res, "mipmap-anydpi/$name.xml").readText()
            assertTrue(text.contains("""<background android:drawable="@drawable/ic_launcher_background" />"""))
            assertTrue(text.contains("""<foreground android:drawable="@drawable/ic_launcher_foreground" />"""))
            assertTrue(text.contains("""<monochrome android:drawable="@drawable/ic_launcher_monochrome" />"""))
        }
    }

    private companion object {
        const val ORIGIN = 24
        const val CELL = 5
        const val SAFE_RADIUS = 33.0
    }
}

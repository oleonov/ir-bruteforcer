package com.pandorika.irbruteforcer.remote

import com.pandorika.irbruteforcer.ir.FlipperIrLibraryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RemoteCatalogTest {

    private val parser = FlipperIrLibraryParser()

    private fun library(category: RemoteCategory) =
        parser.parse(File("src/main/assets/${category.assetPath}").readText())

    @Test
    fun everyButtonHasSignals() {
        RemoteCategory.entries.forEach { category ->
            val library = library(category)
            assertEquals("${category.name} skipped", 0, library.skippedSignals)
            category.buttons.forEach { button ->
                val count = button.signalNames.sumOf { library.signalsNamed(it).size }
                assertTrue("${category.name}/${button.id} has no signals", count > 0)
            }
        }
    }

    @Test
    fun buttonCountsMatchBundledMomentumLibraries() {
        // Measured on Momentum-Firmware d3f89df (2026-08-18). A change means the asset was updated:
        // re-check signal names before updating these numbers.
        val expected = mapOf(
            RemoteCategory.Ac to mapOf("off" to 139, "dry" to 43, "cool_lo" to 38, "cool_hi" to 37, "heat_hi" to 33, "heat_lo" to 32),
            RemoteCategory.Tv to mapOf("power" to 324, "vol_dn" to 104, "ch_prev" to 101, "vol_up" to 99, "ch_next" to 94, "mute" to 93),
            RemoteCategory.Projector to mapOf("power" to 139, "vol_up" to 51, "vol_dn" to 48, "mute" to 45, "play" to 10, "pause" to 10),
            RemoteCategory.Audio to mapOf("power" to 168, "vol_up" to 143, "vol_dn" to 143, "pause" to 141, "mute" to 124, "play" to 106, "next" to 99, "prev" to 96),
            RemoteCategory.Fan to mapOf("power" to 116, "timer" to 80, "speed_up" to 78, "rotate" to 78, "mode" to 48, "speed_dn" to 21),
            RemoteCategory.Led to mapOf("bright_up" to 98, "bright_dn" to 82, "power_on" to 77, "power_off" to 76, "red" to 73, "green" to 70, "blue" to 69, "white" to 66),
            RemoteCategory.Disc to mapOf("power" to 56, "play" to 52, "rewind" to 47, "pause" to 46, "ok" to 46, "forward" to 42, "eject" to 40, "subtitle" to 30),
            RemoteCategory.Monitor to mapOf("power" to 8, "source" to 6, "menu" to 6, "exit" to 4),
        )
        expected.forEach { (category, counts) ->
            val library = library(category)
            assertEquals("${category.name} button set", counts.keys, category.buttons.map { it.id }.toSet())
            counts.forEach { (id, count) ->
                val button = category.button(id)!!
                assertEquals("${category.name}/$id", count, button.signalNames.sumOf { library.signalsNamed(it).size })
            }
        }
    }

    @Test
    fun pinnedCategoriesComeFirst() {
        val entries = RemoteCategory.entries
        assertEquals(listOf(RemoteCategory.Ac, RemoteCategory.Tv), entries.filter { it.pinned })
        assertEquals(listOf(RemoteCategory.Ac, RemoteCategory.Tv), entries.take(2))
    }
}

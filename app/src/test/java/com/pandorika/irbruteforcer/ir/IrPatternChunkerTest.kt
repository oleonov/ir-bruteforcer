package com.pandorika.irbruteforcer.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IrPatternChunkerTest {
    private val limit = IrPatternChunker.MAX_PATTERN_MICROS

    private fun total(durations: IntArray) = durations.fold(0L) { sum, d -> sum + d }

    /** Sum preserved, every chunk within limit, multi-chunk pieces start and end with a mark. */
    private fun assertValidSplit(durations: IntArray, chunks: List<IrPatternChunk>) {
        assertEquals(total(durations), chunks.sumOf { it.totalDurationMicros + it.gapAfterMicros })
        chunks.forEach { chunk ->
            assertTrue("chunk ${chunk.totalDurationMicros} > limit", chunk.totalDurationMicros <= limit)
            assertTrue("chunk must hold a mark", chunk.durationsMicros.isNotEmpty())
            // Odd length => first and last element are both marks (mark, space, ..., mark).
            assertEquals("chunk must start and end with a mark", 1, chunk.durationsMicros.size % 2)
        }
        // Concatenating chunks with their gaps restores the original sequence.
        val restored = chunks.flatMap { chunk ->
            chunk.durationsMicros.toList() + if (chunk.gapAfterMicros > 0) listOf(chunk.gapAfterMicros) else emptyList()
        }
        assertEquals(durations.toList(), restored)
    }

    @Test fun patternWithinLimitIsOneUnchangedChunk() {
        val short = intArrayOf(9_000, 4_500, 560, 560, 560)
        val chunks = IrPatternChunker.split(short)
        assertEquals(1, chunks.size)
        assertSame(short, chunks.single().durationsMicros)
        assertEquals(0, chunks.single().gapAfterMicros)

        val exactlyLimit = intArrayOf(1_000_000, 500_000, 500_000)
        assertEquals(1, IrPatternChunker.split(exactlyLimit).size)
    }

    @Test fun longTrailingSilenceIsCutIntoGap() {
        val durations = intArrayOf(9_000, 4_500, 560, 560, 560, 1_990_000)
        val chunks = IrPatternChunker.split(durations)
        assertEquals(1, chunks.size)
        assertEquals(1_990_000, chunks.single().gapAfterMicros)
        assertValidSplit(durations, chunks)
    }

    @Test fun longPatternSplitsAtSpaces() {
        val durations = intArrayOf(1_500_000, 400_000, 1_500_000, 400_000, 1_500_000)
        val chunks = IrPatternChunker.split(durations)
        assertEquals(3, chunks.size)
        assertEquals(listOf(400_000, 400_000, 0), chunks.map { it.gapAfterMicros })
        assertValidSplit(durations, chunks)
    }

    @Test fun trailingSpaceBecomesGapOfLastChunk() {
        val durations = intArrayOf(1_000_000, 100, 1_000_000, 100, 900_000, 50_000)
        val chunks = IrPatternChunker.split(durations)
        assertEquals(50_000, chunks.last().gapAfterMicros)
        assertValidSplit(durations, chunks)
    }

    @Test fun singleMarkOverLimitIsItsOwnChunk() {
        val durations = intArrayOf(2_500_000, 100, 500)
        val chunks = IrPatternChunker.split(durations)
        assertEquals(listOf(listOf(2_500_000), listOf(500)), chunks.map { it.durationsMicros.toList() })
        assertEquals(total(durations), chunks.sumOf { it.totalDurationMicros + it.gapAfterMicros })
    }

    @Test fun realLongSignalsOfBundledAcLibrarySplitValidly() {
        val library = FlipperIrLibraryParser().parse(File("src/main/assets/infrared/ac.ir").readText())
        val long = library.signals.filter { it.pattern.totalDurationMicros > limit }
        assertTrue("expected the ~5.59 s raw Off signal", long.any { it.name == "Off" && it.type == IrSignalType.Raw })
        long.forEach { signal ->
            val chunks = IrPatternChunker.split(signal.pattern.durationsMicros)
            assertTrue(chunks.size >= 3)
            assertValidSplit(signal.pattern.durationsMicros, chunks)
        }
        library.signals.filter { it.pattern.totalDurationMicros <= limit }.forEach { signal ->
            assertEquals(1, IrPatternChunker.split(signal.pattern.durationsMicros).size)
        }
    }
}

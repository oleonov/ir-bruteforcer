package com.pandorika.irbruteforcer.ir.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test 4: the ConsumerIrManager-ready pattern produced by IrProtocolEncoders.encode. */
class NormalizationTest {
    private fun sample(p: IrProtocol) = IrMessage(p, address = 1, command = 2)

    private fun frames(durations: IntArray): Int =
        1 + durations.indices.count { it % 2 == 1 && durations[it] >= 10_000 }

    @Test fun everyProtocolStartsAndEndsWithMark() {
        for (p in IrProtocol.entries) {
            val pattern = assertNotNull_(IrProtocolEncoders.encode(sample(p)), p)
            assertEquals(p.name, p.carrierFrequency, pattern.carrierFrequency)
            val d = pattern.durationsMicros
            assertTrue("$p odd length (mark first, mark last)", d.size % 2 == 1)
            assertTrue("$p positive", d.all { it > 0 })
        }
    }

    private fun assertNotNull_(pattern: com.pandorika.irbruteforcer.ir.IrPattern?, p: IrProtocol) =
        pattern.also { assertNotNull("$p", it) }!!

    @Test fun noAdjacentSameLevelLeftovers() {
        // total on-air time equals the raw level stream minus leading/trailing spaces: nothing lost, nothing merged twice
        for (p in IrProtocol.entries) {
            val m = sample(p)
            val levels = IrProtocolEncoders.encodeLevels(m, maxOf(p.minRepeatCount, 1))
            val trimmed = levels.dropWhile { !it.mark }.dropLastWhile { !it.mark }
            val pattern = IrProtocolEncoders.encode(m)!!
            assertEquals("$p", trimmed.sumOf { it.micros.toLong() }, pattern.totalDurationMicros)
            // merged runs alternate by construction; verify the run count against the raw stream
            var runs = 0
            var last: Boolean? = null
            for (l in trimmed) if (l.mark != last) { runs++; last = l.mark }
            assertEquals("$p", runs, pattern.durationsMicros.size)
        }
    }

    @Test fun frameCounts() {
        assertEquals(3, frames(IrProtocolEncoders.encode(sample(IrProtocol.SIRC))!!.durationsMicros))
        assertEquals(3, frames(IrProtocolEncoders.encode(sample(IrProtocol.SIRC15))!!.durationsMicros))
        assertEquals(3, frames(IrProtocolEncoders.encode(sample(IrProtocol.SIRC20))!!.durationsMicros))
        assertEquals(1, frames(IrProtocolEncoders.encode(sample(IrProtocol.NEC))!!.durationsMicros)) // min repeat 1: frame, 0 repeat frames
        assertEquals(2, frames(IrProtocolEncoders.encode(sample(IrProtocol.PIONEER))!!.durationsMicros))
    }

    @Test fun necPatternIsExactlyOneFrame() {
        // preamble 2 + 32 bits * 2 + stop mark
        assertEquals(2 + 64 + 1, IrProtocolEncoders.encode(sample(IrProtocol.NEC))!!.durationsMicros.size)
    }

    @Test fun outOfRangeMessagesAreRejected() {
        assertNull(IrProtocolEncoders.encode(IrMessage(IrProtocol.NEC, 0x100, 1)))
        assertNull(IrProtocolEncoders.encode(IrMessage(IrProtocol.NEC, 1, 0x100)))
        assertNull(IrProtocolEncoders.encode(IrMessage(IrProtocol.RC5, 0x20, 1)))
        assertNull(IrProtocolEncoders.encode(IrMessage(IrProtocol.SIRC, 1, 0x80)))
        assertNull(IrProtocolEncoders.encode(IrMessage(IrProtocol.RCA, 0x10, 1)))
        assertNull(IrProtocolEncoders.encode(IrMessage(IrProtocol.NEC, -1, 1)))
        assertFalse(IrProtocolEncoders.isMessageValid(IrMessage(IrProtocol.KASEIKYO, 1L shl 26, 0)))
        assertTrue(IrProtocolEncoders.isMessageValid(IrMessage(IrProtocol.KASEIKYO, (1L shl 26) - 1, 0x3FF)))
        assertTrue(IrProtocolEncoders.isMessageValid(IrMessage(IrProtocol.NEC42_EXT, (1L shl 26) - 1, 0xFFFF)))
    }
}

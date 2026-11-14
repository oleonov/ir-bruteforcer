package com.pandorika.irbruteforcer.ir.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Test 3: encode every message of encoder_decoder_input1, read the payload bits back with a small
 * test-only PDWM / Manchester bit reader driven by the encoder's own timing constants, unpack
 * address/command by the protocol's documented bit layout and compare with the input message.
 */
class RoundTripTest {
    private data class Run(val mark: Boolean, val micros: Int)

    private fun runs(levels: List<IrLevel>): List<Run> {
        val out = ArrayList<Run>()
        for (l in levels) {
            if (out.isNotEmpty() && out.last().mark == l.mark) {
                out[out.lastIndex] = Run(l.mark, out.last().micros + l.micros)
            } else {
                out += Run(l.mark, l.micros)
            }
        }
        return out
    }

    private fun readPdwm(encoder: CommonEncoder, levels: List<IrLevel>, bits: Int): Long {
        val t = encoder.timings
        val r = runs(levels)
        assertEquals("silence", t.silenceTime, r[0].micros)
        assertEquals(Run(true, t.preambleMark), r[1])
        assertEquals(Run(false, t.preambleSpace), r[2])
        var value = 0L
        for (i in 0 until bits) {
            val mark = r[3 + 2 * i]
            // PWM frames (SIRC) end on the last mark: the final space is not part of the stream
            val space = r.getOrNull(4 + 2 * i) ?: Run(false, t.bit1Space).also { assertEquals(t.bit0Space, t.bit1Space) }
            assertTrue(mark.mark && !space.mark)
            val bit = when {
                mark.micros == t.bit1Mark && space.micros == t.bit1Space -> 1L
                mark.micros == t.bit0Mark && space.micros == t.bit0Space -> 0L
                else -> error("bad bit $i: $mark $space")
            }
            value = value or (bit shl i)
        }
        return value
    }

    /** Returns the transmitted bits in order (index 0 = first bit on the air). */
    private fun readManchester(
        encoder: CommonEncoder,
        levels: List<IrLevel>,
        bits: Int,
        markFirstIsOne: Boolean,
        preambleSlots: Int,
        toggleIndex: Int,
    ): List<Int> {
        val t = encoder.timings
        val unit = t.bit1Mark
        val slots = ArrayList<Boolean>()
        runs(levels).forEachIndexed { i, run ->
            val micros = if (i == 0) run.micros - t.silenceTime else run.micros
            repeat((micros.toDouble() / unit).roundToInt()) { slots += run.mark }
        }
        var pos = preambleSlots
        fun slot(i: Int) = if (i < slots.size) slots[i] else false // last space is not transmitted
        return (0 until bits).map { index ->
            val width = if (index == toggleIndex) 2 else 1
            val first = slot(pos)
            val second = slot(pos + width)
            assertEquals("bit $index first half", first, slot(pos + width - 1))
            pos += 2 * width
            assertTrue("bit $index not a transition", first != second)
            if (first == markFirstIsOne) 1 else 0
        }
    }

    private fun msb(bits: List<Int>): Long = bits.fold(0L) { acc, b -> (acc shl 1) or b.toLong() }

    private fun roundTrip(message: IrMessage): Pair<Long, Long> {
        val encoder = IrProtocolEncoders.newEncoder(message.protocol)
        encoder.reset(message)
        val levels = IrProtocolEncoders.encodeLevels(message, 1)
        fun bit(b: Long, from: Int, len: Int) = (b shr from) and ((1L shl len) - 1)
        fun inv(v: Long, len: Int) = v.inv() and ((1L shl len) - 1)
        return when (message.protocol) {
            IrProtocol.NEC -> {
                val b = readPdwm(encoder, levels, 32)
                assertEquals(inv(bit(b, 0, 8), 8), bit(b, 8, 8))
                assertEquals(inv(bit(b, 16, 8), 8), bit(b, 24, 8))
                bit(b, 0, 8) to bit(b, 16, 8)
            }

            IrProtocol.NEC_EXT -> readPdwm(encoder, levels, 32).let { bit(it, 0, 16) to bit(it, 16, 16) }
            IrProtocol.NEC42 -> {
                val b = readPdwm(encoder, levels, 42)
                assertEquals(inv(bit(b, 0, 13), 13), bit(b, 13, 13))
                val command = bit(b, 26, 6) or (bit(b, 32, 2) shl 6)
                assertEquals(inv(command, 8), bit(b, 34, 8))
                bit(b, 0, 13) to command
            }

            IrProtocol.NEC42_EXT -> readPdwm(encoder, levels, 42).let { bit(it, 0, 26) to (bit(it, 26, 6) or (bit(it, 32, 10) shl 6)) }
            IrProtocol.SAMSUNG32 -> {
                val b = readPdwm(encoder, levels, 32)
                assertEquals(bit(b, 0, 8), bit(b, 8, 8))
                assertEquals(inv(bit(b, 16, 8), 8), bit(b, 24, 8))
                bit(b, 0, 8) to bit(b, 16, 8)
            }

            IrProtocol.SIRC -> readPdwm(encoder, levels, 12).let { bit(it, 7, 5) to bit(it, 0, 7) }
            IrProtocol.SIRC15 -> readPdwm(encoder, levels, 15).let { bit(it, 7, 8) to bit(it, 0, 7) }
            IrProtocol.SIRC20 -> readPdwm(encoder, levels, 20).let { bit(it, 7, 13) to bit(it, 0, 7) }
            IrProtocol.RCA -> {
                val b = readPdwm(encoder, levels, 24)
                assertEquals(inv(bit(b, 0, 4), 4), bit(b, 12, 4))
                assertEquals(inv(bit(b, 4, 8), 8), bit(b, 16, 8))
                bit(b, 0, 4) to bit(b, 4, 8)
            }

            IrProtocol.PIONEER -> {
                val b = readPdwm(encoder, levels, 33)
                assertEquals(inv(bit(b, 0, 8), 8), bit(b, 8, 8))
                assertEquals(inv(bit(b, 16, 8), 8), bit(b, 24, 8))
                assertEquals(0L, bit(b, 32, 1))
                bit(b, 0, 8) to bit(b, 16, 8)
            }

            IrProtocol.KASEIKYO -> {
                val b = readPdwm(encoder, levels, 48)
                val d = (0 until 6).map { bit(b, 8 * it, 8) }
                val vendor = d[0] or (d[1] shl 8)
                val parity = (d[0] xor d[1]).let { (it and 0xF) xor (it shr 4) }
                assertEquals(parity, d[2] and 0xF)
                assertEquals(d[2] xor d[3] xor d[4], d[5])
                val address = ((d[4] shr 6) shl 24) or (vendor shl 8) or ((d[2] shr 4) shl 4) or (d[3] and 0xF)
                address to ((d[3] shr 4) or ((d[4] and 0x3F) shl 4))
            }

            IrProtocol.RC5, IrProtocol.RC5X -> {
                // 14 bits: start, field, toggle, 5 address, 6 command (MSB first); space-then-mark = 1
                val b = readManchester(encoder, levels, 14, markFirstIsOne = false, preambleSlots = 0, toggleIndex = -1)
                assertEquals(1, b[0])
                msb(b.subList(3, 8)) to msb(b.subList(8, 14))
            }

            IrProtocol.RC6 -> {
                // 21 bits: start, 3 mode, toggle (double width), 8 address, 8 command (MSB first); mark-then-space = 1
                val b = readManchester(encoder, levels, 21, markFirstIsOne = true, preambleSlots = 8, toggleIndex = 4)
                assertEquals(1, b[0])
                assertEquals(0L, msb(b.subList(1, 4)))
                msb(b.subList(5, 13)) to msb(b.subList(13, 21))
            }
        }
    }

    @Test fun payloadSurvivesEncodeAndReadBack() {
        var checked = 0
        val protocolsSeen = HashSet<IrProtocol>()
        for (file in IrTestFixtures.ALL) {
            val block = IrTestFixtures.load(file)["encoder_decoder_input1"] ?: continue
            for (m in block.messages.filter { !it.repeat }) {
                assertTrue("$file $m", IrProtocolEncoders.isMessageValid(m))
                val (address, command) = roundTrip(m)
                assertEquals("$file address $m", m.address, address)
                assertEquals("$file command $m", m.command, command)
                protocolsSeen += m.protocol
                checked++
            }
        }
        assertTrue("checked only $checked", checked >= 24)
        assertEquals(
            setOf("NEC", "NECext", "NEC42", "NEC42ext", "Samsung32", "RC5", "RC5X", "RC6", "SIRC", "SIRC15", "SIRC20", "Kaseikyo", "RCA", "Pioneer"),
            protocolsSeen.map { it.protocolName }.toSet(),
        )
    }
}

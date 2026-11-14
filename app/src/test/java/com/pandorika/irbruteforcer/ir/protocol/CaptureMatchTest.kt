package com.pandorika.irbruteforcer.ir.protocol

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/**
 * Test 2: our encoding of the first non-repeat expected message must match the first frame of the
 * real-world capture (decoder_inputN) within max(25 %, 150 us) per duration, with equal length.
 *
 * Capture layout: timings[0] is the leading silence space, then mark/space alternate. A frame is the run
 * of durations up to the first space >= 10 ms; a trailing space (Manchester captures) is trimmed because
 * the encoder never emits it. Frame alignment: frame #0 is compared with the first expected message when
 * that message is non-repeat; otherwise (first expected message is a repeat) any frame may match.
 */
class CaptureMatchTest {
    private fun frames(capture: List<Int>): List<List<Int>> {
        val rest = capture.drop(1) // leading silence
        val result = ArrayList<List<Int>>()
        var current = ArrayList<Int>()
        rest.forEachIndexed { i, d ->
            val isSpace = i % 2 == 1
            if (isSpace && d >= 10_000) {
                if (current.isNotEmpty()) result += current
                current = ArrayList()
            } else {
                current += d
            }
        }
        if (current.isNotEmpty()) result += current
        return result.map { if (it.size % 2 == 0) it.dropLast(1) else it }
    }

    private fun matches(actual: IntArray, expected: List<Int>): Boolean =
        actual.size == expected.size && actual.indices.all {
            abs(actual[it] - expected[it]) <= max(0.25 * expected[it], 150.0)
        }

    @Test fun encodedFirstMessageMatchesCapturedFrame() {
        val problems = ArrayList<String>()
        var checked = 0
        for (file in IrTestFixtures.ALL) {
            val blocks = IrTestFixtures.load(file)
            for (n in 1..20) {
                val capture = blocks["decoder_input$n"]?.data ?: continue
                val messages = blocks["decoder_expected$n"]!!.messages
                // Fixture alignment: nec42ext/decoder_input2 is the decoder's "long packet with NEC start"
                // case: the capture holds 44 data bit pairs while the expected message is a 42-bit one.
                if (file == "nec42ext" && n == 2) continue
                val first = messages.first { !it.repeat }
                assertTrue("$file/$n invalid", IrProtocolEncoders.isMessageValid(first))
                val frames = frames(capture)
                // one transmission only (SIRC/Pioneer patterns hold several frames). Manchester captures
                // alternate the toggle bit between fixtures (even inputs carry toggle=1), so try both.
                val candidates = listOf(false, true).map { toggle ->
                    val encoder = IrProtocolEncoders.newEncoder(first.protocol)
                    when (encoder) {
                        is Rc5Encoder -> encoder.toggleBit = toggle
                        is Rc6Encoder -> encoder.toggleBit = toggle
                    }
                    encoder.reset(first)
                    val levels = ArrayList<IrLevel>()
                    do {
                        val status = encoder.encode()
                        levels += IrLevel(encoder.level, encoder.duration)
                    } while (status != EncodeStatus.Done)
                    IrProtocolEncoders.normalize(levels)
                }
                checked++
                val ok = candidates.any { one ->
                    if (!messages.first().repeat) matches(one, frames.first()) else frames.any { matches(one, it) }
                }
                if (!ok) problems += "$file decoder_input$n: encoded=${candidates.map { it.toList() }} captured=${frames.first()}"
            }
        }
        assertTrue("no captures checked", checked > 20)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}

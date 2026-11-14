package com.pandorika.irbruteforcer.ir.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test 1: replay of Flipper's infrared_test_run_encoder (infrared_test.c) against encoder_input/encoder_expected.
 * Per message: reset the (persistent) encoder unless it is a repeat message, encode until Done, merge adjacent
 * same-level steps inside that message, append. The expected data starts with the silence space.
 */
class EncoderGoldenTest {
    private fun replay(encoder: CommonEncoder, messages: List<IrMessage>): List<Int> {
        val out = ArrayList<Int>()
        for (message in messages) {
            if (!message.repeat) encoder.reset(message)
            val merged = ArrayList<Int>()
            var lastLevel: Boolean? = null
            while (true) {
                val status = encoder.encode()
                if (lastLevel == null || lastLevel != encoder.level) merged += 0
                lastLevel = encoder.level
                merged[merged.lastIndex] = merged.last() + encoder.duration
                if (status == EncodeStatus.Done) break
            }
            out += merged
        }
        return out
    }

    private fun golden(file: String, index: Int, toggleStart: Boolean = false) {
        val blocks = IrTestFixtures.load(file)
        val input = checkNotNull(blocks["encoder_input$index"]).messages
        val expected = checkNotNull(blocks["encoder_expected$index"]).data
        val encoder = IrProtocolEncoders.newEncoder(input.first().protocol)
        when (encoder) {
            is Rc5Encoder -> encoder.toggleBit = toggleStart
            is Rc6Encoder -> encoder.toggleBit = toggleStart
        }
        assertArrayEquals("$file encoder_expected$index", expected.toIntArray(), replay(encoder, input).toIntArray())
    }

    @Test fun sirc1() = golden("sirc", 1)

    @Test fun sirc2() = golden("sirc", 2)

    @Test fun rc5x1() = golden("rc5x", 1)

    // Fixture alignment: the RC5 fixture matches exactly with a fresh encoder (toggle bit starts at 0);
    // starting at 1 (the state after another RC5/RC5X reset on the same encoder) does not match.
    @Test fun rc51() = golden("rc5", 1, toggleStart = false)

    @Test fun rc61() = golden("rc6", 1)

    @Test fun fixturesPresent() {
        IrTestFixtures.ALL.forEach { assertTrue(it, IrTestFixtures.load(it).isNotEmpty()) }
    }
}

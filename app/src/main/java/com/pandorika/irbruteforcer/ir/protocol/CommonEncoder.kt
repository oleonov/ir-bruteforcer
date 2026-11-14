// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

/** Mirror of InfraredTimings (lib/infrared/encoder_decoder/infrared.h), encoder-relevant fields only. */
internal class IrTimings(
    val preambleMark: Int = 0,
    val preambleSpace: Int = 0,
    val bit1Mark: Int,
    val bit1Space: Int = 0,
    val bit0Mark: Int = 0,
    val bit0Space: Int = 0,
    val silenceTime: Int,
)

internal enum class EncodeStatus { Ok, Done }

private enum class EncoderState { Silence, Preamble, Encode, EncodeRepeat }

/**
 * Port of InfraredCommonEncoder + infrared_common_encode (common/infrared_common_encoder.c).
 * The payload `data` is the little-endian byte buffer of the C struct packed into a Long
 * (bit i of the buffer == bit i of [data]); the longest payload is 48 bits.
 * Subclasses port the per-protocol reset()/encode_repeat()/encode() wrappers.
 */
internal abstract class CommonEncoder(val timings: IrTimings) {
    private var state = EncoderState.Silence
    protected var switchDetect = false
    protected var bitsToEncode = 0
    protected var bitsEncoded = 0
    protected var timingsSum = 0L
    protected var timingsEncoded = 0
    protected var data = 0L

    /** Output of the last encode() call. */
    var duration = 0
        protected set
    var level = false
        protected set

    /** Protocol-specific message packing; every override must start with commonReset(). */
    abstract fun reset(message: IrMessage)

    /** infrared_common_encoder_reset */
    protected fun commonReset() {
        timingsEncoded = 0
        timingsSum = 0
        bitsEncoded = 0
        state = EncoderState.Silence
        switchDetect = false
        data = 0
    }

    protected open val hasEncodeRepeat: Boolean get() = false

    protected open fun encodeRepeat(): EncodeStatus = error("no encode_repeat")

    /** Per-protocol bit encoder: pdwm by default, manchester for RC5/RC6. */
    protected open fun encodeBitStep(): EncodeStatus = commonEncodePdwm()

    /** Protocol entry point (infrared_encoder_<p>_encode); SIRC/Pioneer override it. */
    open fun encode(): EncodeStatus = commonEncode()

    protected fun forceRepeatState() {
        state = EncoderState.EncodeRepeat
    }

    protected fun forcePreambleState() {
        state = EncoderState.Preamble
    }

    private fun bit(index: Int): Boolean = (data shr index) and 1L == 1L

    /** infrared_common_encode_manchester */
    protected fun commonEncodeManchester(): EncodeStatus {
        val logicValue = bit(bitsEncoded)
        val evenTiming = timingsEncoded % 2 == 0

        level = evenTiming xor logicValue
        duration = timings.bit1Mark
        if (evenTiming) {
            ++bitsEncoded
        } else if (level && (bitsEncoded + 1 == bitsToEncode)) {
            ++bitsEncoded // don't encode last space
        }
        return EncodeStatus.Ok
    }

    /** infrared_common_encode_pdwm */
    protected fun commonEncodePdwm(): EncodeStatus {
        val logicValue = bit(bitsEncoded)
        val pwm = timings.bit1Space == timings.bit0Space

        if (timingsEncoded % 2 == 1) {
            duration = if (logicValue) timings.bit1Mark else timings.bit0Mark
            level = true
            if (pwm) ++bitsEncoded
        } else {
            duration = if (logicValue) timings.bit1Space else timings.bit0Space
            level = false
            if (!pwm) ++bitsEncoded
        }
        return EncodeStatus.Ok
    }

    private fun commonEncodeBits(): EncodeStatus {
        var status = encodeBitStep()
        ++timingsEncoded
        timingsSum += duration
        if (bitsEncoded == bitsToEncode && level) {
            status = EncodeStatus.Done
        }
        return status
    }

    /** infrared_common_encode */
    protected fun commonEncode(): EncodeStatus {
        var status = EncodeStatus.Ok
        var runEncodeBits = false
        when (state) {
            EncoderState.Silence -> {
                duration = timings.silenceTime
                level = false
                state = EncoderState.Preamble
                ++timingsEncoded
                timingsSum = 0
            }

            EncoderState.Preamble -> {
                if (timings.preambleMark != 0) {
                    if (timingsEncoded == 1) {
                        duration = timings.preambleMark
                        level = true
                    } else {
                        duration = timings.preambleSpace
                        level = false
                        state = EncoderState.Encode
                    }
                    ++timingsEncoded
                    timingsSum += duration
                } else {
                    state = EncoderState.Encode
                    runEncodeBits = true
                }
            }

            EncoderState.Encode -> runEncodeBits = true
            EncoderState.EncodeRepeat -> status = encodeRepeat()
        }
        if (runEncodeBits) {
            status = commonEncodeBits()
            if (status == EncodeStatus.Done) {
                if (hasEncodeRepeat) {
                    state = EncoderState.EncodeRepeat
                } else {
                    timingsEncoded = 0
                    timingsSum = 0
                    bitsEncoded = 0
                    switchDetect = false
                    state = EncoderState.Silence
                }
            }
        }
        return status
    }
}

internal fun reverse8(value: Long): Long {
    val v = (value and 0xFF).toInt()
    var result = 0
    for (i in 0 until 8) {
        if (v and (1 shl i) != 0) result = result or (1 shl (7 - i))
    }
    return result.toLong()
}

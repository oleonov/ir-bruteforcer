// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

private const val NEC_PREAMBLE_MARK = 9000
private const val NEC_PREAMBLE_SPACE = 4500
private const val NEC_BIT1_MARK = 560
private const val NEC_BIT1_SPACE = 1690
private const val NEC_BIT0_MARK = 560
private const val NEC_BIT0_SPACE = 560
private const val NEC_REPEAT_PERIOD = 110_000
private const val NEC_REPEAT_MARK = 9000
private const val NEC_REPEAT_SPACE = 2250

private val NEC_REPEAT_TIMINGS = intArrayOf(
    NEC_REPEAT_PERIOD - NEC_REPEAT_MARK - NEC_REPEAT_SPACE - NEC_BIT1_MARK,
    NEC_REPEAT_MARK,
    NEC_REPEAT_SPACE,
    NEC_BIT1_MARK,
)

/** Port of nec/infrared_encoder_nec.c (NEC, NECext, NEC42, NEC42ext). */
internal class NecEncoder : CommonEncoder(
    IrTimings(
        preambleMark = NEC_PREAMBLE_MARK,
        preambleSpace = NEC_PREAMBLE_SPACE,
        bit1Mark = NEC_BIT1_MARK,
        bit1Space = NEC_BIT1_SPACE,
        bit0Mark = NEC_BIT0_MARK,
        bit0Space = NEC_BIT0_SPACE,
        silenceTime = NEC_REPEAT_PERIOD,
    ),
) {
    override val hasEncodeRepeat: Boolean get() = true

    override fun reset(message: IrMessage) {
        commonReset()
        val a = message.address
        val c = message.command
        when (message.protocol) {
            IrProtocol.NEC -> {
                val address = a and 0xFF
                val command = c and 0xFF
                data = address or
                    ((address.inv() and 0xFF) shl 8) or
                    (command shl 16) or
                    ((command.inv() and 0xFF) shl 24)
                bitsToEncode = 32
            }

            IrProtocol.NEC_EXT -> {
                data = (a and 0xFFFF) or ((c and 0xFFFF) shl 16)
                bitsToEncode = 32
            }

            IrProtocol.NEC42 -> {
                // 13 address + 13 inverse address + 8 command + 8 inv command
                var data1 = a and 0x1FFF
                data1 = data1 or ((a.inv() and 0x1FFF) shl 13)
                data1 = data1 or ((c and 0x3F) shl 26)
                var data2 = (c and 0xC0) shr 6
                data2 = data2 or ((c.inv() and 0xFF) shl 2)
                data = (data1 and 0xFFFFFFFFL) or (data2 shl 32)
                bitsToEncode = 42
            }

            IrProtocol.NEC42_EXT -> {
                val data1 = ((a and 0x3FFFFFF) or ((c and 0x3F) shl 26)) and 0xFFFFFFFFL
                val data2 = (c and 0xFFC0) shr 6
                data = data1 or (data2 shl 32)
                bitsToEncode = 42
            }

            else -> error("not a NEC variant: ${message.protocol}")
        }
    }

    override fun encodeRepeat(): EncodeStatus {
        // space + 2 timings preamble + payload + stop bit
        val timingsEncodedUpToRepeat = 1 + 2 + bitsToEncode * 2 + 1
        val repeatCnt = timingsEncoded - timingsEncodedUpToRepeat
        check(repeatCnt >= 0)

        duration = if (repeatCnt > 0) {
            NEC_REPEAT_TIMINGS[repeatCnt % NEC_REPEAT_TIMINGS.size]
        } else {
            (NEC_REPEAT_PERIOD - timingsSum).toInt()
        }
        level = repeatCnt % 2 == 1
        ++timingsEncoded
        val done = (repeatCnt + 1) % NEC_REPEAT_TIMINGS.size == 0
        return if (done) EncodeStatus.Done else EncodeStatus.Ok
    }
}

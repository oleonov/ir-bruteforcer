// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

private const val SIRC_REPEAT_PERIOD = 45_000

/** Port of sirc/infrared_encoder_sirc.c (SIRC, SIRC15, SIRC20). */
internal class SircEncoder : CommonEncoder(
    IrTimings(
        preambleMark = 2400,
        preambleSpace = 600,
        bit1Mark = 1200,
        bit1Space = 600,
        bit0Mark = 600,
        bit0Space = 600,
        silenceTime = 10_000,
    ),
) {
    override val hasEncodeRepeat: Boolean get() = true

    override fun reset(message: IrMessage) {
        commonReset()
        val command = message.command and 0x7F
        when (message.protocol) {
            IrProtocol.SIRC -> {
                data = command or ((message.address and 0x1F) shl 7)
                bitsToEncode = 12
            }

            IrProtocol.SIRC15 -> {
                data = command or ((message.address and 0xFF) shl 7)
                bitsToEncode = 15
            }

            IrProtocol.SIRC20 -> {
                data = command or ((message.address and 0x1FFF) shl 7)
                bitsToEncode = 20
            }

            else -> error("not a SIRC variant: ${message.protocol}")
        }
    }

    override fun encodeRepeat(): EncodeStatus {
        check(timingsEncoded == 1 + 2 + bitsToEncode * 2 - 1)
        check(timingsSum < SIRC_REPEAT_PERIOD)
        duration = (SIRC_REPEAT_PERIOD - timingsSum).toInt()
        level = false

        timingsSum = 0
        timingsEncoded = 1
        bitsEncoded = 0
        forcePreambleState()
        return EncodeStatus.Ok
    }

    override fun encode(): EncodeStatus {
        var status = commonEncode()
        if (status == EncodeStatus.Ok && bitsEncoded == bitsToEncode) {
            check(!level)
            status = EncodeStatus.Done
            forceRepeatState()
        }
        return status
    }
}

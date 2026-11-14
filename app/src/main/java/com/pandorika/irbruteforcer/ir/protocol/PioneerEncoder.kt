// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

private const val PIONEER_SILENCE = 26_000

/** Port of pioneer/infrared_encoder_pioneer.c. */
internal class PioneerEncoder : CommonEncoder(
    IrTimings(
        preambleMark = 8500,
        preambleSpace = 4225,
        bit1Mark = 500,
        bit1Space = 1500,
        bit0Mark = 500,
        bit0Space = 500,
        silenceTime = PIONEER_SILENCE,
    ),
) {
    override val hasEncodeRepeat: Boolean get() = true

    override fun reset(message: IrMessage) {
        commonReset()
        val address = message.address and 0xFF
        val command = message.command and 0xFF
        // data[4] = 0 -> the 33rd bit is a zero bit
        data = address or ((address.inv() and 0xFF) shl 8) or
            (command shl 16) or ((command.inv() and 0xFF) shl 24)
        bitsToEncode = 33
    }

    override fun encodeRepeat(): EncodeStatus {
        duration = PIONEER_SILENCE
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

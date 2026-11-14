// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

/** Port of rca/infrared_encoder_rca.c. */
internal class RcaEncoder : CommonEncoder(
    IrTimings(
        preambleMark = 4000,
        preambleSpace = 4000,
        bit1Mark = 500,
        bit1Space = 2000,
        bit0Mark = 500,
        bit0Space = 1000,
        silenceTime = 8000,
    ),
) {
    override fun reset(message: IrMessage) {
        commonReset()
        val address = message.address and 0xFF
        val addressInverse = address.inv() and 0xFF
        val command = message.command and 0xFF
        val commandInverse = command.inv() and 0xFF

        data = (address and 0xF) or
            (command shl 4) or
            ((addressInverse and 0xF) shl 12) or
            (commandInverse shl 16)
        bitsToEncode = 24
    }
}

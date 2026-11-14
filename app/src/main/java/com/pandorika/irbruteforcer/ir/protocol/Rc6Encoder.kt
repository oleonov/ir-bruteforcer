// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

private const val RC6_BIT = 444

/** Port of rc6/infrared_encoder_rc6.c. Toggle bit semantics as in [Rc5Encoder]. */
internal class Rc6Encoder : CommonEncoder(
    IrTimings(
        preambleMark = 2666,
        preambleSpace = 889,
        bit1Mark = RC6_BIT,
        silenceTime = 2700 * 10,
    ),
) {
    var toggleBit = false

    override fun reset(message: IrMessage) {
        commonReset()
        var d = 0x01L // start bit; 3 bits for mode == 0
        if (toggleBit) d = d or 0x10
        d = d or (reverse8(message.address) shl 5)
        d = d or (reverse8(message.command) shl 13)
        data = d
        bitsToEncode = 1 + 3 + 1 + 8 + 8
        toggleBit = !toggleBit
    }

    /** infrared_encoder_rc6_encode_manchester: the toggle bit (index 4) has double length. */
    override fun encodeBitStep(): EncodeStatus {
        val toggle = bitsEncoded == 4
        val status = commonEncodeManchester()
        if (toggle) duration *= 2
        return status
    }
}

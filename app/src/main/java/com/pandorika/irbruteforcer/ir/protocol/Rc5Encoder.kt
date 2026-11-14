// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

private const val RC5_BIT = 888

/**
 * Port of rc5/infrared_encoder_rc5.c (RC5, RC5X). [toggleBit] lives in the encoder (not the message)
 * and flips on every reset(), exactly like Flipper's InfraredEncoderRC5; a fresh encoder starts at false.
 */
internal class Rc5Encoder : CommonEncoder(
    IrTimings(bit1Mark = RC5_BIT, silenceTime = 2700 * 10),
) {
    var toggleBit = false

    override fun reset(message: IrMessage) {
        commonReset()
        var d = 0x01L // start bit
        if (message.protocol == IrProtocol.RC5) {
            d = d or 0x02 // start bit
        }
        if (toggleBit) d = d or 0x04
        d = d or ((reverse8(message.address) shr 3) shl 3) // address 5 bit
        d = d or ((reverse8(message.command) shr 2) shl 8) // command 6 bit

        // data[0] = ~data[0]; data[1] = ~data[1]
        data = d xor 0xFFFFL
        bitsToEncode = 1 + 1 + 1 + 5 + 6
        toggleBit = !toggleBit
    }

    override fun encodeBitStep(): EncodeStatus = commonEncodeManchester()
}

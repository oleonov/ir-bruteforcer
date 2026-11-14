// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

private const val KASEIKYO_UNIT = 432

/** Port of kaseikyo/infrared_encoder_kaseikyo.c. */
internal class KaseikyoEncoder : CommonEncoder(
    IrTimings(
        preambleMark = 8 * KASEIKYO_UNIT,
        preambleSpace = 4 * KASEIKYO_UNIT,
        bit1Mark = KASEIKYO_UNIT,
        bit1Space = 3 * KASEIKYO_UNIT,
        bit0Mark = KASEIKYO_UNIT,
        bit0Space = KASEIKYO_UNIT,
        silenceTime = 130_000,
    ),
) {
    override fun reset(message: IrMessage) {
        commonReset()
        val address = message.address and 0xFFFFFFFFL
        val command = message.command and 0xFFFF

        val id = (address shr 24) and 3
        val vendorId = (address shr 8) and 0xFFFF
        val genre1 = (address shr 4) and 0xF
        val genre2 = address and 0xF

        val d0 = vendorId and 0xFF
        val d1 = vendorId shr 8
        var vendorParity = d0 xor d1
        vendorParity = (vendorParity and 0xF) xor (vendorParity shr 4)
        val d2 = (vendorParity and 0xF) or (genre1 shl 4)
        val d3 = (genre2 and 0xF) or ((command and 0xF) shl 4)
        val d4 = ((id shl 6) or (command shr 4)) and 0xFF
        val d5 = d2 xor d3 xor d4

        data = d0 or (d1 shl 8) or (d2 shl 16) or (d3 shl 24) or (d4 shl 32) or (d5 shl 40)
        bitsToEncode = 48
    }
}

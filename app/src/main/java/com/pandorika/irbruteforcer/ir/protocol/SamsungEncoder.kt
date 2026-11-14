// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

private const val SAMSUNG_BIT1_MARK = 550
private const val SAMSUNG_REPEAT_PAUSE1 = 46_000
private const val SAMSUNG_REPEAT_PAUSE2 = 97_000
private const val SAMSUNG_REPEAT_MARK = 4500
private const val SAMSUNG_REPEAT_SPACE = 4500
private const val SAMSUNG_BIT1_SPACE = 1650

private val SAMSUNG_REPEAT_TIMINGS = intArrayOf(
    SAMSUNG_REPEAT_PAUSE2,
    SAMSUNG_REPEAT_MARK,
    SAMSUNG_REPEAT_SPACE,
    SAMSUNG_BIT1_MARK,
    SAMSUNG_BIT1_SPACE,
    SAMSUNG_BIT1_MARK,
)

/** Port of samsung/infrared_encoder_samsung.c. */
internal class SamsungEncoder : CommonEncoder(
    IrTimings(
        preambleMark = 4500,
        preambleSpace = 4500,
        bit1Mark = SAMSUNG_BIT1_MARK,
        bit1Space = SAMSUNG_BIT1_SPACE,
        bit0Mark = 550,
        bit0Space = 550,
        silenceTime = 145_000,
    ),
) {
    override val hasEncodeRepeat: Boolean get() = true

    override fun reset(message: IrMessage) {
        commonReset()
        val address = message.address and 0xFF
        val command = message.command and 0xFF
        val commandInverse = command.inv() and 0xFF
        data = address or (address shl 8) or (command shl 16) or (commandInverse shl 24)
        bitsToEncode = 32
    }

    override fun encodeRepeat(): EncodeStatus {
        // space + 2 timings preamble + payload + stop bit
        val timingsEncodedUpToRepeat = 1 + 2 + bitsEncoded * 2 + 1
        val repeatCnt = timingsEncoded - timingsEncodedUpToRepeat
        check(repeatCnt >= 0)

        duration = if (repeatCnt > 0) {
            SAMSUNG_REPEAT_TIMINGS[repeatCnt % SAMSUNG_REPEAT_TIMINGS.size]
        } else {
            SAMSUNG_REPEAT_PAUSE1
        }
        level = repeatCnt % 2 == 1
        ++timingsEncoded
        val done = (repeatCnt + 1) % SAMSUNG_REPEAT_TIMINGS.size == 0
        return if (done) EncodeStatus.Done else EncodeStatus.Ok
    }
}

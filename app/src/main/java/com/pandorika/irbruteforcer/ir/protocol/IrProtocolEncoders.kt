// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

import com.pandorika.irbruteforcer.ir.IrPattern

object IrProtocolEncoders {
    /**
     * Port of infrared_signal_is_message_valid (signal/infrared_signal.c): address and command must
     * fit the variant's address_length / command_length bits.
     */
    fun isMessageValid(message: IrMessage): Boolean {
        val p = message.protocol
        val addressMask = (1L shl p.addressLength) - 1
        val commandMask = (1L shl p.commandLength) - 1
        return message.address in 0..addressMask && message.command in 0..commandMask
    }

    internal fun newEncoder(protocol: IrProtocol): CommonEncoder = when (protocol) {
        IrProtocol.NEC, IrProtocol.NEC_EXT, IrProtocol.NEC42, IrProtocol.NEC42_EXT -> NecEncoder()
        IrProtocol.SAMSUNG32 -> SamsungEncoder()
        IrProtocol.RC5, IrProtocol.RC5X -> Rc5Encoder()
        IrProtocol.RC6 -> Rc6Encoder()
        IrProtocol.SIRC, IrProtocol.SIRC15, IrProtocol.SIRC20 -> SircEncoder()
        IrProtocol.KASEIKYO -> KaseikyoEncoder()
        IrProtocol.RCA -> RcaEncoder()
        IrProtocol.PIONEER -> PioneerEncoder()
    }

    /**
     * Port of infrared_send / infrared_get_data_callback (worker/infrared_transmit.c): fresh encoder,
     * reset once, then every Done ends one transmission and the encoder is NOT reset in between, so
     * later transmissions are repeat frames for protocols with encode_repeat. Raw steps, not merged.
     */
    fun encodeLevels(message: IrMessage, transmissions: Int): List<IrLevel> {
        require(transmissions > 0)
        val encoder = newEncoder(message.protocol)
        encoder.reset(message)
        val levels = ArrayList<IrLevel>()
        var remaining = transmissions
        while (remaining > 0) {
            val status = encoder.encode()
            levels += IrLevel(encoder.level, encoder.duration)
            if (status == EncodeStatus.Done) remaining--
        }
        return levels
    }

    /** infrared_send(message, 1): max(min_repeat_count, 1) transmissions, normalized for ConsumerIrManager. */
    fun encode(message: IrMessage): IrPattern? {
        if (!isMessageValid(message)) return null
        val levels = encodeLevels(message, maxOf(message.protocol.minRepeatCount, 1))
        val durations = normalize(levels)
        if (durations.isEmpty()) return null
        return IrPattern(message.protocol.carrierFrequency, durations)
    }

    /** Drops leading spaces, merges adjacent same-level steps, drops the trailing space. */
    internal fun normalize(levels: List<IrLevel>): IntArray {
        val out = ArrayList<Int>()
        var currentMark = true // the pattern starts with a mark
        for (step in levels) {
            if (out.isEmpty()) {
                if (!step.mark) continue
                out += step.micros
                currentMark = true
            } else if (step.mark == currentMark) {
                out[out.lastIndex] = out.last() + step.micros
            } else {
                out += step.micros
                currentMark = step.mark
            }
        }
        if (out.isNotEmpty() && !currentMark) out.removeAt(out.lastIndex)
        return out.toIntArray()
    }
}

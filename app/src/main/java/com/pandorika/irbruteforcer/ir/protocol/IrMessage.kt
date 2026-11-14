// SPDX-License-Identifier: GPL-3.0-only
// Kotlin port of the Flipper Zero infrared encoders (lib/infrared/encoder_decoder),
// Copyright (C) Flipper Devices Inc. Modified by the IR Brute contributors. See NOTICE.

package com.pandorika.irbruteforcer.ir.protocol

/**
 * Supported Flipper protocol variants. Constants mirror the InfraredProtocolVariant tables in
 * lib/infrared/encoder_decoder/<p>/infrared_protocol_<p>.c.
 */
enum class IrProtocol(
    val protocolName: String,
    val carrierFrequency: Int,
    val minRepeatCount: Int,
    val addressLength: Int,
    val commandLength: Int,
) {
    NEC("NEC", 38_000, 1, 8, 8),
    NEC_EXT("NECext", 38_000, 1, 16, 16),
    NEC42("NEC42", 38_000, 1, 13, 8),
    NEC42_EXT("NEC42ext", 38_000, 1, 26, 16),
    SAMSUNG32("Samsung32", 38_000, 1, 8, 8),
    RC5("RC5", 36_000, 1, 5, 6),
    RC5X("RC5X", 36_000, 1, 5, 7),
    RC6("RC6", 36_000, 1, 8, 8),
    SIRC("SIRC", 40_000, 3, 5, 7),
    SIRC15("SIRC15", 40_000, 3, 8, 7),
    SIRC20("SIRC20", 40_000, 3, 13, 7),
    KASEIKYO("Kaseikyo", 38_000, 1, 26, 10),
    RCA("RCA", 38_000, 1, 4, 8),
    PIONEER("Pioneer", 40_000, 2, 8, 8),
    ;

    companion object {
        fun fromName(name: String): IrProtocol? = entries.firstOrNull { it.protocolName == name }
    }
}

/** Mirror of InfraredMessage: address/command are unsigned values (little-endian hex in .ir files). */
data class IrMessage(
    val protocol: IrProtocol,
    val address: Long,
    val command: Long,
    val repeat: Boolean = false,
)

/** One encoder output step: a mark (level = true) or space of [micros] microseconds. */
data class IrLevel(val mark: Boolean, val micros: Int)

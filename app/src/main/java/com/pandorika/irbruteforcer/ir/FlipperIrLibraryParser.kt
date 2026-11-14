package com.pandorika.irbruteforcer.ir

import com.pandorika.irbruteforcer.ir.protocol.IrMessage
import com.pandorika.irbruteforcer.ir.protocol.IrProtocol
import com.pandorika.irbruteforcer.ir.protocol.IrProtocolEncoders

class FlipperIrLibraryParser {
    fun parse(text: String): IrLibrary {
        val signals = mutableListOf<IrSignal>()
        var skippedSignals = 0
        var currentModel: String? = null
        var currentName: String? = null
        val fields = linkedMapOf<String, String>()

        fun flushSignal() {
            val name = currentName ?: return
            val signal = buildSignal(name, currentModel, fields)
            if (signal != null) {
                signals += signal
            } else {
                skippedSignals++
            }
            currentName = null
            fields.clear()
        }

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("# Model:") -> {
                    flushSignal()
                    currentModel = line.removePrefix("# Model:").trim().takeIf { it.isNotBlank() }
                }

                line.startsWith("#") -> Unit // other comments

                line.startsWith("name:") -> {
                    flushSignal()
                    currentName = line.valueAfterColon()
                }

                currentName != null && line.contains(":") -> {
                    val separatorIndex = line.indexOf(':')
                    val key = line.substring(0, separatorIndex).trim()
                    val value = line.substring(separatorIndex + 1).trim()
                    fields[key] = value
                }
            }
        }
        flushSignal()

        return IrLibrary(signals = signals, skippedSignals = skippedSignals)
    }

    private fun buildSignal(
        name: String,
        model: String?,
        fields: Map<String, String>,
    ): IrSignal? {
        return when (fields["type"]) {
            "raw" -> buildRawSignal(name, model, fields)
            "parsed" -> buildParsedSignal(name, model, fields)
            else -> null
        }
    }

    private fun buildRawSignal(
        name: String,
        model: String?,
        fields: Map<String, String>,
    ): IrSignal? {
        val frequency = fields["frequency"]?.toIntOrNull() ?: return null
        val durations = fields["data"]
            ?.splitToSequence(' ')
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { it.toIntOrNull() }
            ?.toList()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return IrSignal(
            name = name,
            model = model,
            type = IrSignalType.Raw,
            pattern = IrPattern(
                carrierFrequency = frequency,
                durationsMicros = durations.toIntArray(),
            ),
        )
    }

    private fun buildParsedSignal(
        name: String,
        model: String?,
        fields: Map<String, String>,
    ): IrSignal? {
        val protocol = IrProtocol.fromName(fields["protocol"] ?: return null) ?: return null
        val address = fields["address"]?.parseLittleEndianHexBytes() ?: return null
        val command = fields["command"]?.parseLittleEndianHexBytes() ?: return null
        val message = IrMessage(protocol, address, command)
        // null for an out-of-range address/command (Flipper rejects such signals as well)
        val pattern = IrProtocolEncoders.encode(message) ?: return null

        return IrSignal(
            name = name,
            model = model,
            type = IrSignalType.Parsed,
            pattern = pattern,
            protocol = protocol.protocolName,
            address = address,
            command = command,
        )
    }

    private fun String.valueAfterColon(): String = substringAfter(':').trim()

    /** "13 00 00 00" -> 0x13 (little-endian bytes, at most 4 like Flipper's uint32). */
    private fun String.parseLittleEndianHexBytes(): Long? {
        val tokens = split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty() || tokens.size > 4) return null
        var value = 0L
        tokens.forEachIndexed { index, token ->
            val byte = token.toIntOrNull(16)?.takeIf { it in 0..0xFF } ?: return null
            value = value or (byte.toLong() shl (index * 8))
        }
        return value
    }
}

package com.pandorika.irbruteforcer.ir

import java.security.MessageDigest

enum class IrSignalType {
    Raw,
    Parsed,
}

data class IrPattern(
    val carrierFrequency: Int,
    val durationsMicros: IntArray,
) {
    val totalDurationMicros: Long
        get() = durationsMicros.fold(0L) { total, duration -> total + duration }
}

data class IrSignal(
    val name: String,
    val model: String?,
    val type: IrSignalType,
    val pattern: IrPattern,
    /** Flipper protocol name for parsed signals, null for raw. */
    val protocol: String? = null,
    val address: Long? = null,
    val command: Long? = null,
) {
    /**
     * Stable identity of the code, independent of the signal name:
     * raw -> SHA-1 of "raw|<freq>|<durations>", parsed -> SHA-1 of "p|<protocol>|<address hex>|<command hex>".
     */
    val fingerprint: String by lazy(LazyThreadSafetyMode.NONE) {
        val canonical = if (protocol != null && address != null && command != null) {
            "p|$protocol|${address.toString(16)}|${command.toString(16)}"
        } else {
            "raw|${pattern.carrierFrequency}|${pattern.durationsMicros.joinToString(" ")}"
        }
        MessageDigest.getInstance("SHA-1")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

data class IrLibrary(
    val signals: List<IrSignal>,
    val skippedSignals: Int,
) {
    val rawCount: Int = signals.count { it.type == IrSignalType.Raw }
    val parsedCount: Int = signals.count { it.type == IrSignalType.Parsed }

    fun signalsNamed(name: String): List<IrSignal> = signals.filter { it.name == name }
}


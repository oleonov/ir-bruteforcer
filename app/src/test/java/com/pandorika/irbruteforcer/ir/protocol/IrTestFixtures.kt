package com.pandorika.irbruteforcer.ir.protocol

/** Reader for Flipper `.irtest` files (applications/debug/unit_tests/resources/unit_tests/infrared). */
internal class IrTestBlock(val name: String, val type: String) {
    var data: List<Int> = emptyList()
    val messages = ArrayList<IrMessage>()
}

internal object IrTestFixtures {
    /** Fixture file basenames, e.g. "nec" -> irtest/test_nec.irtest */
    val ALL = listOf(
        "kaseikyo", "nec", "nec42", "nec42ext", "necext", "pioneer",
        "rc5", "rc5x", "rc6", "rca", "samsung32", "sirc",
    )

    fun load(file: String): Map<String, IrTestBlock> {
        val text = checkNotNull(javaClass.getResourceAsStream("/irtest/test_$file.irtest")) {
            "missing fixture $file"
        }.bufferedReader().readText()

        val blocks = LinkedHashMap<String, IrTestBlock>()
        var block: IrTestBlock? = null
        var pendingName: String? = null
        var protocol: IrProtocol? = null
        var address = 0L
        var command = 0L
        var lastKey: String? = null
        val dataTokens = ArrayList<Int>()

        fun flushData() {
            block?.takeIf { dataTokens.isNotEmpty() }?.data = dataTokens.toList()
            dataTokens.clear()
        }

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val colon = line.indexOf(':')
            if (colon < 0) {
                // continuation of a multi-line value (long "data:" lists)
                if (lastKey == "data") line.split(' ').filter { it.isNotBlank() }.forEach { dataTokens += it.toInt() }
                continue
            }
            val key = line.substring(0, colon).trim()
            val value = line.substring(colon + 1).trim()
            lastKey = key
            when (key) {
                "name" -> {
                    flushData()
                    pendingName = value
                }

                "type" -> {
                    block = IrTestBlock(checkNotNull(pendingName), value).also { blocks[it.name] = it }
                }

                "data" -> value.split(' ').filter { it.isNotBlank() }.forEach { dataTokens += it.toInt() }
                "protocol" -> protocol = checkNotNull(IrProtocol.fromName(value)) { "unknown protocol $value" }
                "address" -> address = littleEndian(value)
                "command" -> command = littleEndian(value)
                "repeat" -> block!!.messages += IrMessage(protocol!!, address, command, value == "true")
            }
        }
        flushData()
        return blocks
    }

    private fun littleEndian(value: String): Long =
        value.split(' ').filter { it.isNotBlank() }
            .foldIndexed(0L) { i, acc, t -> acc or (t.toLong(16) shl (8 * i)) }
}

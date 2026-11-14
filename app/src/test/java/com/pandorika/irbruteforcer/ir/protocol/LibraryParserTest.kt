package com.pandorika.irbruteforcer.ir.protocol

import com.pandorika.irbruteforcer.ir.FlipperIrLibraryParser
import com.pandorika.irbruteforcer.ir.IrSignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Tests 5 (library smoke) plus parser behaviour for parsed signals. */
class LibraryParserTest {
    private val parser = FlipperIrLibraryParser()

    @Test fun bundledLibrariesParseWithoutSkips() {
        val names = listOf("ac", "tv", "projectors", "audio", "fans", "leds", "bluray_dvd", "monitor")
        var checked = 0
        for (name in names) {
            val file = File("src/main/assets/infrared/$name.ir")
            if (!file.exists()) continue
            checked++
            val library = parser.parse(file.readText())
            assertTrue("$name is empty", library.signals.isNotEmpty())
            assertEquals("$name skippedSignals", 0, library.skippedSignals)
        }
        assumeTrue("no bundled libraries present yet", checked > 0)
    }

    private fun nec(address: String, command: String, name: String = "Power") = """
        |Filetype: IR signals file
        |Version: 1
        |# Model: X1
        |#
        |name: $name
        |type: parsed
        |protocol: NEC
        |address: $address
        |command: $command
        |#
    """.trimMargin()

    @Test fun outOfRangeNecAddressIsSkipped() {
        val library = parser.parse(nec("00 01 00 00", "02 00 00 00"))
        assertEquals(0, library.signals.size)
        assertEquals(1, library.skippedSignals)
    }

    @Test fun validNecIsParsedWithProtocolFields() {
        val library = parser.parse(nec("04 00 00 00", "08 00 00 00"))
        assertEquals(0, library.skippedSignals)
        val s = library.signals.single()
        assertEquals(IrSignalType.Parsed, s.type)
        assertEquals("NEC", s.protocol)
        assertEquals(4L, s.address)
        assertEquals(8L, s.command)
        assertEquals("X1", s.model)
        assertEquals(38_000, s.pattern.carrierFrequency)
        assertEquals(2 + 64 + 1, s.pattern.durationsMicros.size)
    }

    @Test fun littleEndianMultiByteAddressAndCrlf() {
        val text = nec("34 12 00 00", "78 56 00 00").replace("\n", "\r\n").replace("NEC", "NECext")
        val s = parser.parse(text).signals.single()
        assertEquals(0x1234L, s.address)
        assertEquals(0x5678L, s.command)
    }

    @Test fun unknownProtocolIsSkipped() {
        val library = parser.parse(nec("00 00 00 00", "00 00 00 00").replace("protocol: NEC", "protocol: Foo"))
        assertEquals(1, library.skippedSignals)
    }

    @Test fun fingerprintIsStableAndNameIndependent() {
        val a = parser.parse(nec("04 00 00 00", "08 00 00 00", "Power")).signals.single()
        val b = parser.parse(nec("04 00 00 00", "08 00 00 00", "Standby")).signals.single()
        val c = parser.parse(nec("04 00 00 00", "09 00 00 00", "Power")).signals.single()
        assertEquals(a.fingerprint, b.fingerprint)
        assertEquals(a.fingerprint, parser.parse(nec("04 00 00 00", "08 00 00 00")).signals.single().fingerprint)
        assertNotEquals(a.fingerprint, c.fingerprint)
        assertEquals(40, a.fingerprint.length)
    }

    @Test fun rawSignalKeepsFieldsAndIgnoresDutyCycleAndComments() {
        val text = """
            |# some comment: with colon
            |name: R1
            |type: raw
            |frequency: 38000
            |duty_cycle: 0.330000
            |data: 9000 4500 560 560 560
            |#
            |name: R1b
            |type: raw
            |frequency: 38000
            |duty_cycle: 0.5
            |data: 9000 4500 560 560 560
        """.trimMargin()
        val library = parser.parse(text)
        assertEquals(0, library.skippedSignals)
        val (r1, r1b) = library.signals
        assertNull(r1.protocol)
        assertNull(r1.address)
        assertEquals(listOf(9000, 4500, 560, 560, 560), r1.pattern.durationsMicros.toList())
        assertEquals("raw|38000|9000 4500 560 560 560".let { sha1(it) }, r1.fingerprint)
        assertEquals(r1.fingerprint, r1b.fingerprint)
        assertNotNull(r1.fingerprint)
    }

    private fun sha1(s: String) = java.security.MessageDigest.getInstance("SHA-1")
        .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}

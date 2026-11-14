package com.pandorika.irbruteforcer.remote

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pandorika.irbruteforcer.ir.IrLibrary
import com.pandorika.irbruteforcer.ir.IrPattern
import com.pandorika.irbruteforcer.ir.IrSignal
import com.pandorika.irbruteforcer.ir.IrSignalType
import com.pandorika.irbruteforcer.ir.IrTransmitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteViewModelTest {

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    private var nextFrequency = 30_000

    /** 50 ms single-chunk code. Unique carrier keeps the fingerprint and pattern identity unique. */
    private fun short(name: String = "Off", model: String? = null) =
        IrSignal(name, model, IrSignalType.Raw, IrPattern(nextFrequency++, intArrayOf(40_000, 10_000)))

    /** 5.3 s code: 3 chunks of 1500 ms separated by 400 ms gaps. */
    private fun long(name: String = "Off") = IrSignal(
        name, null, IrSignalType.Raw,
        IrPattern(nextFrequency++, intArrayOf(1_500_000, 400_000, 1_500_000, 400_000, 1_500_000)),
    )

    private val ac = RemoteCategory.Ac

    private inner class Harness(
        scope: TestScope,
        private val libraries: Map<RemoteCategory, List<IrSignal>>,
        val store: CursorStore = InMemoryCursorStore(),
    ) {
        val tx = FakeIrTransmitter(scope.testScheduler)
        val vm = RemoteViewModel(
            loader = { category -> IrLibrary(libraries[category].orEmpty(), skippedSignals = 0) },
            transmitter = tx,
            store = store,
            parseDispatcher = StandardTestDispatcher(scope.testScheduler),
            clock = { scope.testScheduler.currentTime },
        )
        val state get() = vm.state.value
        val mode get() = vm.state.value.mode

        /** Patterns of the first chunk of every logical send, in order. */
        fun sent(): List<IrPattern> = tx.emissions.filter { it.chunk == 0 }.map { it.pattern }

        init {
            scope.runCurrent()
        }
    }

    private fun TestScope.harness(signals: List<IrSignal>, store: CursorStore = InMemoryCursorStore()) =
        Harness(this, mapOf(ac to signals), store)

    private fun storeWithCursor(cursor: Int, delayMillis: Long = CursorStore.DEFAULT_DELAY_MILLIS) =
        InMemoryCursorStore(delayMillis).apply { setCursor(RemoteCategory.Ac, cursor) }

    private fun assertSequential(emissions: List<FakeIrTransmitter.Emission>) {
        emissions.zipWithNext { a, b ->
            assertTrue("overlap: $a then $b", b.startMillis >= a.endMillis)
        }
    }

    // Period of one short code in a scan with the default 650 ms delay.
    private val period = 50L + 650L

    // ---- Scan basics ----

    @Test fun scanSendsAllCodesInOrderWithDelays() = runTest {
        val signals = List(4) { short() }
        val h = harness(signals)
        assertEquals(4, h.state.buttonCounts["off"])
        assertEquals(0, h.state.cursor)
        assertEquals(false, h.state.isLoading)

        h.vm.press(ac, "off")
        runCurrent()
        assertEquals(RemoteMode.Scanning("off", 0, 4, null, remainingMillis = 4 * period), h.mode)
        advanceTimeBy(period + 1)
        assertEquals(RemoteMode.Scanning("off", 1, 4, null, remainingMillis = 3 * period), h.mode)

        advanceUntilIdle()
        assertEquals(signals.map { it.pattern }, h.sent())
        assertEquals(listOf(0L, period, 2 * period, 3 * period), h.tx.emissions.map { it.startMillis })
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(RemoteMessage.ScanDone(failed = 0), h.state.message)
        assertEquals("a finished scan leaves the cursor alone", 0, h.state.cursor)
    }

    @Test fun pressingScanningButtonAgainStops() = runTest {
        val h = harness(List(5) { short() })
        h.vm.press(ac, "off")
        advanceTimeBy(period + 100)
        h.vm.press(ac, "off")
        advanceUntilIdle()
        assertEquals(2, h.sent().size)
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(RemoteMessage.Stopped, h.state.message)
    }

    @Test fun stopReturnsToIdle() = runTest {
        val h = harness(List(5) { short() })
        h.vm.press(ac, "off")
        advanceTimeBy(2 * period + 100)
        h.vm.stop()
        advanceUntilIdle()
        assertEquals(3, h.sent().size)
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(RemoteMessage.Stopped, h.state.message)
    }

    @Test fun pressWithoutCodesReportsNoCodes() = runTest {
        val h = harness(listOf(short("Off")))
        h.vm.press(ac, "dry")
        advanceUntilIdle()
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(RemoteMessage.NoCodes, h.state.message)
        assertTrue(h.tx.emissions.isEmpty())
    }

    // ---- Shared cursor: what a press scans ----

    @Test fun multiNameButtonCandidatesKeepFileOrderAndFileIndices() {
        val a1 = short("A"); val b1 = short("B"); val c = short("C"); val a2 = short("A"); val b2 = short("B")
        val library = IrLibrary(listOf(a1, b1, c, a2, b2), skippedSignals = 0)
        // Names listed B first: the scan must still follow the file, not group by name.
        val button = RemoteButton("x", null, ButtonIcon.Power, listOf("B", "A"))

        val candidates = scanCandidates(library, button)
        assertEquals(listOf(0, 1, 3, 4), candidates.map { it.index })
        assertEquals(listOf(a1, b1, a2, b2), candidates.map { it.value })

        assertEquals(listOf(3, 4), scanFrom(candidates, 2).map { it.index })
        assertEquals(listOf(3, 4), scanFrom(candidates, 3).map { it.index })
        assertEquals("nothing at/after the cursor -> full list", listOf(0, 1, 3, 4), scanFrom(candidates, 5).map { it.index })
    }

    @Test fun scanStartsAtCursorInFileOrder() = runTest {
        // File: 0 Off, 1 Cool_hi, 2 Off, 3 Cool_hi, 4 Off, 5 Off
        val signals = listOf(short("Off"), short("Cool_hi"), short("Off"), short("Cool_hi"), short("Off"), short("Off"))
        val h = harness(signals, storeWithCursor(2))
        assertEquals(2, h.state.cursor)
        assertEquals(3, h.state.buttonCounts["off"])
        assertEquals(1, h.state.buttonCounts["cool_hi"])

        h.vm.press(ac, "off")
        runCurrent()
        assertEquals(3, (h.mode as RemoteMode.Scanning).total)
        advanceUntilIdle()
        assertEquals(listOf(signals[2], signals[4], signals[5]).map { it.pattern }, h.sent())

        val before = h.sent().size
        h.vm.press(ac, "cool_hi")
        advanceUntilIdle()
        assertEquals(listOf(signals[3].pattern), h.sent().drop(before))
    }

    @Test fun scanFallsBackToFullListWhenNothingAtOrAfterCursor() = runTest {
        // File: 0 Off, 1 Off, 2 Cool_hi, 3 Off, 4 Cool_hi, 5 Cool_hi
        val signals = listOf(short("Off"), short("Off"), short("Cool_hi"), short("Off"), short("Cool_hi"), short("Cool_hi"))
        val h = harness(signals, storeWithCursor(4))
        assertEquals("fallback size is the full list", 3, h.state.buttonCounts["off"])
        assertEquals(2, h.state.buttonCounts["cool_hi"])

        h.vm.press(ac, "off")
        runCurrent()
        assertEquals(3, (h.mode as RemoteMode.Scanning).total)
        advanceUntilIdle()
        assertEquals("no wrap: from the start, in file order", listOf(signals[0], signals[1], signals[3]).map { it.pattern }, h.sent())
        assertEquals(4, h.state.cursor)
    }

    @Test fun buttonCountsFollowTheCursor() = runTest {
        // File: 0 Off, 1 Cool_hi, 2 Off, 3 Cool_hi, 4 Off, 5 Heat_hi, 6 Off
        val signals = listOf(
            short("Off"), short("Cool_hi"), short("Off"), short("Cool_hi"), short("Off"), short("Heat_hi"), short("Off"),
        )
        val expected = mapOf(
            0 to mapOf("off" to 4, "cool_hi" to 2, "heat_hi" to 1),
            3 to mapOf("off" to 2, "cool_hi" to 1, "heat_hi" to 1),
            6 to mapOf("off" to 1, "cool_hi" to 2, "heat_hi" to 1), // cool_hi falls back to all
        )
        for ((cursor, counts) in expected) {
            val h = harness(signals, storeWithCursor(cursor))
            assertEquals(cursor, h.state.cursor)
            for ((id, count) in counts) assertEquals("cursor=$cursor $id", count, h.state.buttonCounts[id])
            assertEquals(0, h.state.buttonCounts["dry"])
        }
    }

    // ---- СРАБОТАЛО ----

    @Test fun confirmSetsCursorToCodeSentFiveSecondsBeforeTap() {
        val tapAt = 9_010L
        for (delayMillis in listOf(250L, 650L, 1_000L)) {
            runTest {
                val codePeriod = 50L + delayMillis
                val signals = List(40) { short() }
                val store = InMemoryCursorStore(delayMillis)
                val h = harness(signals, store)
                h.vm.press(ac, "off")
                advanceTimeBy(tapAt)
                h.vm.confirmWorked()
                advanceUntilIdle()

                val expected = ((tapAt - RemoteViewModel.CURSOR_REWIND_MILLIS) / codePeriod).toInt()
                assertEquals("delay=$delayMillis", expected, h.state.cursor)
                assertEquals(expected, store.cursor(ac))
                assertEquals(RemoteMode.Idle, h.mode)
                assertEquals(RemoteMessage.CursorSet(position = expected + 1), h.state.message)
                assertEquals(40 - expected, h.state.buttonCounts["off"])

                val count = h.tx.emissions.size
                advanceTimeBy(60_000)
                assertEquals("СРАБОТАЛО sends nothing", count, h.tx.emissions.size)
            }
        }
    }

    @Test fun confirmClampsToFirstCodeOfScanWhenScanIsYoungerThanRewind() = runTest {
        // File: 0..2 Off, 3 Cool_hi, 4..9 Off; cursor 3 -> the off scan starts at file index 4.
        val signals = List(3) { short("Off") } + short("Cool_hi") + List(6) { short("Off") }
        val h = harness(signals, storeWithCursor(3))
        h.vm.press(ac, "off")
        advanceTimeBy(2_000)
        h.vm.confirmWorked()
        advanceUntilIdle()
        assertEquals(4, h.state.cursor)
        assertEquals(RemoteMessage.CursorSet(position = 5), h.state.message)
        assertEquals(RemoteMode.Idle, h.mode)
    }

    @Test fun confirmWorksDuringScanTail() = runTest {
        val signals = List(10) { short() }
        val h = harness(signals)
        h.vm.press(ac, "off")
        advanceTimeBy(9_000) // last code ended at 6_350 + 650 ms delay; tail runs until 10_000
        assertTrue(h.mode is RemoteMode.Scanning)
        assertEquals(10, h.sent().size)
        h.vm.confirmWorked()
        advanceUntilIdle()
        assertEquals(5, h.state.cursor) // code 5 started at 3_500 <= 9_000 - 5_000
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(RemoteMessage.CursorSet(position = 6), h.state.message)

        val idle = harness(signals)
        idle.vm.press(ac, "off")
        advanceTimeBy(10 * period + RemoteViewModel.SCAN_TAIL_MILLIS + 1)
        assertEquals(RemoteMode.Idle, idle.mode)
        assertEquals(RemoteMessage.ScanDone(failed = 0), idle.state.message)
        idle.vm.confirmWorked()
        advanceUntilIdle()
        assertEquals("after the tail СРАБОТАЛО is a no-op", 0, idle.state.cursor)
    }

    @Test fun confirmDuringLongInFlightCodeUsesTapTimeNotJoinTime() = runTest {
        val signals = List(4) { long() }
        val h = harness(signals)
        val longPeriod = 5_300L + 650L
        val starts = List(4) { it * longPeriod } // 0, 5_950, 11_900, 17_850
        h.vm.press(ac, "off")
        advanceTimeBy(15_800) // code 2, chunk 2 on air (15_700..17_200)
        val tappedAt = testScheduler.currentTime
        h.vm.confirmWorked()
        advanceTimeBy(1_300) // t = 17_100: still waiting for the in-flight chunk
        assertTrue("cursor waits for the join", h.mode is RemoteMode.Scanning)

        advanceUntilIdle()
        val joinedAt = h.tx.emissions.last().endMillis
        assertEquals(17_200L, joinedAt)
        val byTap = starts.indexOfLast { it <= tappedAt - RemoteViewModel.CURSOR_REWIND_MILLIS }
        val byJoin = starts.indexOfLast { it <= joinedAt - RemoteViewModel.CURSOR_REWIND_MILLIS }
        assertEquals(1, byTap)
        assertNotEquals(byTap, byJoin)
        assertEquals(byTap, h.state.cursor)
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(1, h.tx.maxInFlight)
    }

    @Test fun confirmQueuedBehindAnotherPressUsesTheScanVisibleAtTap() = runTest {
        // Off: file indices 0..2 (starts 0, 5_950, 11_900); Cool_hi: 3..4.
        val signals = listOf(long("Off"), long("Off"), long("Off"), long("Cool_hi"), long("Cool_hi"))
        val h = harness(signals)
        h.vm.press(ac, "off")
        advanceTimeBy(12_500) // Off code 2, chunk 0 on air (11_900..13_400)
        h.vm.press(ac, "cool_hi") // queued behind the in-flight chunk
        h.vm.confirmWorked() // tapped while the Off scan is still on screen
        advanceUntilIdle()

        // 12_500 - 5_000 = 7_500 -> Off code 1 (started 5_950), not Cool's first code (index 3).
        assertEquals(1, h.state.cursor)
        assertEquals(1, h.store.cursor(ac))
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(1, h.tx.maxInFlight)
    }

    // ---- Persistence, СТОП, СБРОС ----

    @Test fun cursorPersistsAndIsRestoredOnFreshViewModel() = runTest {
        val signals = List(20) { short() }
        val store = InMemoryCursorStore()
        val h = harness(signals, store)
        h.vm.press(ac, "off")
        advanceTimeBy(9_010)
        h.vm.confirmWorked()
        advanceUntilIdle()
        assertEquals(5, store.cursor(ac))

        val relaunched = harness(signals, store)
        assertEquals(5, relaunched.state.cursor)
        assertEquals(15, relaunched.state.buttonCounts["off"])
        relaunched.vm.press(ac, "off")
        advanceUntilIdle()
        assertEquals(signals.drop(5).map { it.pattern }, relaunched.sent())
    }

    @Test fun storedCursorBeyondLibraryIsClamped() = runTest {
        val signals = List(5) { short() }
        val h = harness(signals, storeWithCursor(100))
        assertEquals(4, h.state.cursor)
        assertEquals(1, h.state.buttonCounts["off"])

        val empty = harness(emptyList(), storeWithCursor(100))
        assertEquals(0, empty.state.cursor)
        assertEquals(0, empty.state.buttonCounts["off"])
    }

    @Test fun stopKeepsCursor() = runTest {
        val store = storeWithCursor(3)
        val h = harness(List(10) { short() }, store)
        h.vm.press(ac, "off")
        advanceTimeBy(6 * period + 100)
        h.vm.stop()
        advanceUntilIdle()
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(3, h.state.cursor)
        assertEquals(3, store.cursor(ac))
        assertEquals(7, h.state.buttonCounts["off"])
    }

    @Test fun resetReturnsCursorToZeroAndFullCounts() = runTest {
        val store = storeWithCursor(4)
        store.setCursor(RemoteCategory.Tv, 7)
        val h = harness(List(10) { short() }, store)
        assertEquals(6, h.state.buttonCounts["off"])
        h.vm.press(ac, "off")
        advanceTimeBy(period + 100)

        h.vm.reset(ac)
        advanceUntilIdle()
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(0, h.state.cursor)
        assertEquals(0, store.cursor(ac))
        assertEquals(10, h.state.buttonCounts["off"])
        assertEquals(RemoteMessage.CursorReset, h.state.message)
        assertEquals("other categories untouched", 7, store.cursor(RemoteCategory.Tv))
        val count = h.tx.emissions.size
        advanceTimeBy(60_000)
        assertEquals(count, h.tx.emissions.size)

        h.vm.reset(RemoteCategory.Tv)
        advanceUntilIdle()
        assertEquals(0, store.cursor(RemoteCategory.Tv))
        assertEquals(ac, h.state.category)
    }

    // ---- R4: press bound to the tapped category ----

    @Test fun r4_pressQueuedBehindCategorySwitchSendsNothing() = runTest {
        val h = Harness(
            this,
            mapOf(RemoteCategory.Tv to List(3) { short("Power") }, RemoteCategory.Projector to List(2) { short("Power") }),
        )
        advanceUntilIdle() // preload: every switch below is instant
        h.vm.selectCategory(RemoteCategory.Tv)
        runCurrent()
        assertEquals(RemoteCategory.Tv, h.state.category)

        h.vm.selectCategory(RemoteCategory.Projector)
        h.vm.press(RemoteCategory.Tv, "power") // tapped on the TV pad, runs after the switch
        advanceUntilIdle()
        assertEquals(RemoteCategory.Projector, h.state.category)
        assertEquals(RemoteMode.Idle, h.mode)
        assertTrue(h.tx.emissions.isEmpty())

        h.vm.press(RemoteCategory.Projector, "power") // control: the visible pad still works
        advanceUntilIdle()
        assertEquals(2, h.sent().size)
    }

    @Test fun r4_stalePressAfterSwitchPublishedSendsNothing() = runTest {
        val h = Harness(
            this,
            mapOf(RemoteCategory.Tv to List(3) { short("Power") }, RemoteCategory.Projector to List(2) { short("Power") }),
        )
        advanceUntilIdle()
        h.vm.selectCategory(RemoteCategory.Tv)
        runCurrent()
        h.vm.press(RemoteCategory.Tv, "power")
        advanceTimeBy(100)
        h.vm.selectCategory(RemoteCategory.Projector)
        advanceUntilIdle()
        assertEquals(RemoteCategory.Projector, h.state.category)
        val count = h.tx.emissions.size
        assertEquals(1, count)

        h.vm.press(RemoteCategory.Tv, "power") // a not-yet-recomposed TV pad
        advanceUntilIdle()
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(count, h.tx.emissions.size)
    }

    // ---- Failures ----

    @Test fun nonFatalFailureSkipsAndContinues() = runTest {
        val signals = List(4) { short() }
        val h = harness(signals)
        h.tx.failure = { if (it === signals[1].pattern) IrTransmitResult.Failed("IR pattern too long") else null }
        h.vm.press(ac, "off")
        advanceTimeBy(period + 100) // index 1 failed
        assertEquals(RemoteMessage.SendFailed(count = 1), h.state.message)
        advanceUntilIdle()
        assertEquals(listOf(signals[0], signals[2], signals[3]).map { it.pattern }, h.sent())
        assertEquals(1, h.state.failedCount)
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(RemoteMessage.ScanDone(failed = 1), h.state.message)
    }

    @Test fun fatalFailureAbortsScan() = runTest {
        val signals = List(4) { short() }
        val h = harness(signals)
        h.tx.failure = {
            if (it === signals[1].pattern) IrTransmitResult.Failed("IR emitter unavailable", fatal = true) else null
        }
        h.vm.press(ac, "off")
        advanceUntilIdle()
        assertEquals(listOf(signals[0].pattern), h.sent())
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(RemoteMessage.Error("IR emitter unavailable"), h.state.message)
    }

    // ---- Ownership / cancellation ----

    @Test fun categorySwitchCancelsAndJoinsScan() = runTest {
        val h = Harness(
            this,
            mapOf(ac to listOf(long(), long()), RemoteCategory.Tv to listOf(short("Power"))),
        )
        h.vm.press(ac, "off")
        advanceTimeBy(500)
        h.vm.selectCategory(RemoteCategory.Tv)
        advanceTimeBy(900)
        // The cancelled chunk is still on air: no state change yet.
        assertEquals(ac, h.state.category)
        assertTrue(h.mode is RemoteMode.Scanning)

        advanceUntilIdle()
        assertEquals(RemoteCategory.Tv, h.state.category)
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(1, h.state.buttonCounts["power"])
        assertEquals(listOf(0), h.tx.emissions.map { it.chunk })
        assertEquals(1, h.tx.maxInFlight)
    }

    @Test fun clearingViewModelCancelsTransmission() = runTest {
        val tx = FakeIrTransmitter(testScheduler)
        val owner = ViewModelStore()
        val vm = ViewModelProvider(
            owner,
            viewModelFactory {
                initializer {
                    RemoteViewModel(
                        loader = { IrLibrary(listOf(long(), long()), 0) },
                        transmitter = tx,
                        store = InMemoryCursorStore(),
                        parseDispatcher = StandardTestDispatcher(testScheduler),
                        clock = { testScheduler.currentTime },
                    )
                }
            },
        )[RemoteViewModel::class.java]
        runCurrent()
        vm.press(ac, "off")
        advanceTimeBy(500)
        owner.clear()
        advanceUntilIdle()
        assertEquals(listOf(0), tx.emissions.map { it.chunk })
        assertEquals(0, tx.inFlight)
    }

    @Test fun a_stopDuringFirstChunkNeverEmitsLaterChunks() = runTest {
        val h = harness(listOf(long()))
        h.vm.press(ac, "off")
        advanceTimeBy(500)
        h.vm.stop()
        advanceTimeBy(999) // t = 1499: chunk 0 still on air
        assertTrue("Idle must wait for the join", h.mode is RemoteMode.Scanning)

        advanceUntilIdle()
        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(listOf(0), h.tx.emissions.map { it.chunk })
        assertEquals(1, h.tx.maxInFlight)
        assertEquals(0, h.tx.inFlight)
    }

    @Test fun b_pressQueuedBehindConfirmStartsOnlyAfterCancelledSendFinished() = runTest {
        val signals = listOf(long(), long(), long())
        val h = harness(signals)
        h.vm.press(ac, "off")
        advanceTimeBy(500)
        h.vm.confirmWorked()
        h.vm.press(ac, "off") // queued behind the confirmation: scans again from the new cursor
        advanceTimeBy(999)
        assertTrue("cursor must wait for the join", h.mode is RemoteMode.Scanning)
        assertEquals(1, h.tx.emissions.size)

        advanceTimeBy(100) // t = 1_599: confirmation applied, the new scan is on air
        assertEquals(0, h.state.cursor)
        val p = signals[0].pattern
        assertEquals(
            listOf(
                FakeIrTransmitter.Emission(p, 0, 0, 1_500), // cancelled send: chunk 0 only
                FakeIrTransmitter.Emission(p, 0, 1_500, 3_000), // new scan starts after it finished
            ),
            h.tx.emissions,
        )
        h.vm.stop()
        advanceUntilIdle()
        assertSequential(h.tx.emissions)
        assertEquals(1, h.tx.maxInFlight)
    }

    /** Off, Cool_hi and Heat_hi with two long codes each. */
    private fun longButtons() = listOf(long("Off"), long("Off"), long("Cool_hi"), long("Cool_hi"), long("Heat_hi"), long("Heat_hi"))

    @Test fun c_rapidPressesOfOtherButtonsEmitStrictlySequentially() = runTest {
        val signals = longButtons()
        val h = harness(signals)
        h.vm.press(ac, "off")
        advanceTimeBy(500)
        h.vm.press(ac, "cool_hi")
        h.vm.press(ac, "heat_hi")
        advanceUntilIdle()

        assertSequential(h.tx.emissions)
        assertEquals(listOf(0), h.tx.emissions.filter { it.pattern === signals[0].pattern }.map { it.chunk })
        assertTrue(h.tx.emissions.none { it.pattern === signals[1].pattern })
        // cool_hi's scan is cancelled before or at its first chunk boundary.
        assertTrue(h.tx.emissions.filter { it.pattern === signals[2].pattern }.all { it.chunk == 0 })
        assertEquals(listOf(0, 1, 2), h.tx.emissions.filter { it.pattern === signals[4].pattern }.map { it.chunk })
        assertEquals(listOf(0, 1, 2), h.tx.emissions.filter { it.pattern === signals[5].pattern }.map { it.chunk })
        assertEquals(RemoteMessage.ScanDone(failed = 0), h.state.message)
        assertEquals(1, h.tx.maxInFlight)
    }

    @Test fun d_concurrentPressesThenStopLeaveNoSurvivingJob() = runTest {
        val signals = longButtons()
        val h = harness(signals)
        h.vm.press(ac, "off")
        advanceTimeBy(500)
        h.vm.press(ac, "cool_hi")
        h.vm.press(ac, "heat_hi")
        h.vm.stop()
        advanceUntilIdle()

        assertEquals(RemoteMode.Idle, h.mode)
        assertEquals(0, h.tx.inFlight)
        assertSequential(h.tx.emissions)
        // Every owner was cancelled at its first chunk boundary; order follows the queued intents.
        assertTrue(h.tx.emissions.all { it.chunk == 0 })
        val order = listOf(signals[0], signals[2], signals[4]).map { it.pattern }
        val sentOrder = h.tx.emissions.map { emission -> order.indexOfFirst { it === emission.pattern } }
        assertTrue("unexpected code sent: $sentOrder", sentOrder.all { it >= 0 })
        assertEquals(sentOrder.sorted().distinct(), sentOrder)
        assertEquals(1, h.tx.maxInFlight)

        val count = h.tx.emissions.size
        advanceTimeBy(60_000)
        assertEquals("zero emissions after stop", count, h.tx.emissions.size)
    }

    // ---- Loading ----

    @Test fun preloadFillsTotalsForEveryCategory() = runTest {
        val libraries = RemoteCategory.entries.associateWith { category ->
            List(category.ordinal + 1) { short("Power") }
        }
        val h = Harness(this, libraries)
        advanceUntilIdle()
        assertEquals(libraries.mapValues { it.value.size }, h.state.libraryTotals)
    }

    @Test fun pressWhileLoadingIsIgnoredAndLateLoadKeepsScan() = runTest {
        val tvSignals = List(3) { short("Power") }
        val tx = FakeIrTransmitter(testScheduler)
        val vm = RemoteViewModel(
            loader = { category ->
                delay(100)
                IrLibrary(if (category == RemoteCategory.Tv) tvSignals else emptyList(), skippedSignals = 0)
            },
            transmitter = tx,
            store = InMemoryCursorStore(),
            parseDispatcher = StandardTestDispatcher(testScheduler),
            clock = { testScheduler.currentTime },
        )
        // t=100: AC loaded, preload starts parsing TV (done at t=200).
        advanceTimeBy(150)
        vm.selectCategory(RemoteCategory.Tv) // own load finishes at t=250
        advanceTimeBy(60) // t=210: preload already stored TV, own load still running
        assertTrue(vm.state.value.isLoading)
        vm.press(RemoteCategory.Tv, "power")
        runCurrent()
        assertEquals(RemoteMode.Idle, vm.state.value.mode)
        assertTrue(tx.emissions.isEmpty())

        advanceTimeBy(50) // t=260: own load applied
        assertEquals(false, vm.state.value.isLoading)
        assertEquals(3, vm.state.value.buttonCounts["power"])
        vm.press(RemoteCategory.Tv, "power")
        runCurrent()
        assertTrue(vm.state.value.mode is RemoteMode.Scanning)
        advanceUntilIdle()
        assertEquals(tvSignals.map { it.pattern }, tx.emissions.filter { it.chunk == 0 }.map { it.pattern })
    }
}

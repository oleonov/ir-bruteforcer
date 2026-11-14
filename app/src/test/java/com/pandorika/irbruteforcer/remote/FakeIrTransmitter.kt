package com.pandorika.irbruteforcer.remote

import com.pandorika.irbruteforcer.ir.IrCarrierRange
import com.pandorika.irbruteforcer.ir.IrPattern
import com.pandorika.irbruteforcer.ir.IrPatternChunker
import com.pandorika.irbruteforcer.ir.IrTransmitResult
import com.pandorika.irbruteforcer.ir.IrTransmitter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.withContext

/**
 * Emulates [com.pandorika.irbruteforcer.ir.AndroidIrTransmitter] in virtual time: the pattern is split by
 * [IrPatternChunker]; each chunk is non-abortable (like the blocking platform call) and lasts its duration;
 * gaps are cancellable delays; `ensureActive()` runs before every chunk.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeIrTransmitter(
    private val scheduler: TestCoroutineScheduler,
    override val hasEmitter: Boolean = true,
) : IrTransmitter {
    data class Emission(val pattern: IrPattern, val chunk: Int, val startMillis: Long, val endMillis: Long)

    val emissions = mutableListOf<Emission>()

    /** Returning non-null fails that pattern without emitting anything. */
    var failure: (IrPattern) -> IrTransmitResult.Failed? = { null }

    var inFlight = 0
        private set
    var maxInFlight = 0
        private set

    override val carrierRanges: List<IrCarrierRange> = emptyList()

    override suspend fun transmit(pattern: IrPattern): IrTransmitResult {
        failure(pattern)?.let { return it }
        inFlight++
        maxInFlight = maxOf(maxInFlight, inFlight)
        try {
            for ((index, chunk) in IrPatternChunker.split(pattern.durationsMicros).withIndex()) {
                currentCoroutineContext().ensureActive()
                val duration = (chunk.totalDurationMicros + 999) / 1_000
                val start = scheduler.currentTime
                emissions += Emission(pattern, index, start, start + duration)
                withContext(NonCancellable) { delay(duration) }
                if (chunk.gapAfterMicros > 0) delay((chunk.gapAfterMicros + 500L) / 1_000L)
            }
            return IrTransmitResult.Sent
        } finally {
            inFlight--
        }
    }
}

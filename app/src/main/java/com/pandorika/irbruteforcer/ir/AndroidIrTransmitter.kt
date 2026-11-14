package com.pandorika.irbruteforcer.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible

data class IrCarrierRange(
    val minFrequency: Int,
    val maxFrequency: Int,
)

sealed interface IrTransmitResult {
    data object Sent : IrTransmitResult

    /** [fatal] = the emitter itself is unavailable, so every further send will fail too. */
    data class Failed(val reason: String, val fatal: Boolean = false) : IrTransmitResult
}

interface IrTransmitter {
    val hasEmitter: Boolean
    val carrierRanges: List<IrCarrierRange>

    /**
     * Sends one logical signal (all chunks of a split pattern). Cancellation takes effect at the next
     * chunk boundary: a cancelled call never starts another chunk.
     */
    suspend fun transmit(pattern: IrPattern): IrTransmitResult
}

class AndroidIrTransmitter(
    context: Context,
) : IrTransmitter {
    private val manager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    override val hasEmitter: Boolean
        get() = manager?.hasIrEmitter() == true

    override val carrierRanges: List<IrCarrierRange>
        get() = manager?.carrierFrequencies
            ?.map { range ->
                IrCarrierRange(
                    minFrequency = range.minFrequency,
                    maxFrequency = range.maxFrequency,
                )
            }
            .orEmpty()

    override suspend fun transmit(pattern: IrPattern): IrTransmitResult {
        val consumerIrManager = manager
            ?: return IrTransmitResult.Failed("ConsumerIrManager unavailable", fatal = true)
        if (!consumerIrManager.hasIrEmitter()) {
            return IrTransmitResult.Failed("IR emitter unavailable", fatal = true)
        }
        if (pattern.durationsMicros.isEmpty()) {
            return IrTransmitResult.Failed("Empty IR pattern")
        }

        val chunks = IrPatternChunker.split(pattern.durationsMicros)
        for (chunk in chunks) {
            currentCoroutineContext().ensureActive()
            try {
                // A chunk (<= 2 s) cannot be aborted by the platform; cancellation lands after it.
                runInterruptible(Dispatchers.IO) {
                    consumerIrManager.transmit(pattern.carrierFrequency, chunk.durationsMicros)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: RuntimeException) {
                return IrTransmitResult.Failed(exception.message ?: exception::class.java.simpleName)
            }
            if (chunk.gapAfterMicros > 0) {
                delay((chunk.gapAfterMicros + 500L) / 1_000L)
            }
        }
        return IrTransmitResult.Sent
    }
}

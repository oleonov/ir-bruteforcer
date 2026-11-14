package com.pandorika.irbruteforcer.ir

/**
 * One piece of a split pattern. For a split pattern [durationsMicros] starts and ends with a mark;
 * [gapAfterMicros] is the space that was cut after it (0 when nothing follows).
 */
class IrPatternChunk(
    val durationsMicros: IntArray,
    val gapAfterMicros: Int,
) {
    val totalDurationMicros: Long
        get() = durationsMicros.fold(0L) { total, duration -> total + duration }
}

object IrPatternChunker {
    /** Android `ConsumerIrService` rejects patterns whose total duration exceeds 2 s. */
    const val MAX_PATTERN_MICROS: Long = 2_000_000L

    /**
     * Patterns up to [limitMicros] come back unchanged as one chunk. Longer patterns are split greedily
     * at space boundaries (even indices are marks, odd are spaces): every chunk is the longest run of
     * mark..mark that fits in [limitMicros], and the space after it becomes [IrPatternChunk.gapAfterMicros].
     * Sum of all chunks and gaps equals the original total. A single mark longer than the limit becomes
     * its own (oversized) chunk; the platform rejects it and the caller skips the signal.
     */
    fun split(durations: IntArray, limitMicros: Long = MAX_PATTERN_MICROS): List<IrPatternChunk> {
        val total = durations.fold(0L) { sum, duration -> sum + duration }
        if (total <= limitMicros) return listOf(IrPatternChunk(durations, gapAfterMicros = 0))

        val chunks = mutableListOf<IrPatternChunk>()
        var start = 0
        while (start < durations.size) {
            var end = start
            var accumulated = 0L
            var index = start
            while (index < durations.size) {
                accumulated += durations[index]
                if (accumulated > limitMicros) break
                if (index % 2 == 0) end = index
                index++
            }
            val gap = if (end + 1 < durations.size) durations[end + 1] else 0
            chunks += IrPatternChunk(durations.copyOfRange(start, end + 1), gap)
            start = end + 2
        }
        return chunks
    }
}

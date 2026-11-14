package com.pandorika.irbruteforcer.remote

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pandorika.irbruteforcer.ir.AndroidAssetIrLibraryLoader
import com.pandorika.irbruteforcer.ir.AndroidIrTransmitter
import com.pandorika.irbruteforcer.ir.IrLibrary
import com.pandorika.irbruteforcer.ir.IrSignal
import com.pandorika.irbruteforcer.ir.IrTransmitResult
import com.pandorika.irbruteforcer.ir.IrTransmitter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.sign

sealed interface RemoteMode {
    data object Idle : RemoteMode

    /** [index] is the code being sent (or last sent) out of [total]; [label] = model, else protocol. */
    data class Scanning(
        val buttonId: String,
        val index: Int,
        val total: Int,
        val label: String?,
        val remainingMillis: Long,
    ) : RemoteMode
}

/** One-line status for the LCD; the UI maps each to a localized text. */
sealed interface RemoteMessage {
    data object Stopped : RemoteMessage
    data object NoCodes : RemoteMessage

    /** СРАБОТАЛО moved the cursor to library position [position] (1-based). */
    data class CursorSet(val position: Int) : RemoteMessage
    data object CursorReset : RemoteMessage

    /** The scan went through all its codes; [failed] of them were rejected by the platform. */
    data class ScanDone(val failed: Int) : RemoteMessage

    /** [count] codes of the running scan were rejected by the platform so far. */
    data class SendFailed(val count: Int) : RemoteMessage

    /** Fatal transmitter failure; [reason] is technical platform text, shown verbatim. */
    data class Error(val reason: String) : RemoteMessage
}

data class RemoteUiState(
    val category: RemoteCategory = RemoteCategory.Ac,
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasEmitter: Boolean = false,
    /** Per button id: how many codes the NEXT press would scan (from [cursor], or all on fallback). */
    val buttonCounts: Map<String, Int> = emptyMap(),
    /** Category cursor: index into the library's signals (file order) where the next scan starts. */
    val cursor: Int = 0,
    val mode: RemoteMode = RemoteMode.Idle,
    val delayMillis: Long = CursorStore.DEFAULT_DELAY_MILLIS,
    val message: RemoteMessage? = null,
    /** Codes skipped in the current/last scan because the platform rejected them. */
    val failedCount: Int = 0,
    /** Total signals per category library, filled as each library is parsed (dropdown counts). */
    val libraryTotals: Map<RemoteCategory, Int> = emptyMap(),
)

/** Signals [button] brute-forces, in library (file) order; each keeps its index in `library.signals`. */
internal fun scanCandidates(library: IrLibrary, button: RemoteButton): List<IndexedValue<IrSignal>> =
    library.signals.withIndex().filter { it.value.name in button.signalNames }

/** What a scan from [cursor] sends: candidates at or after it; all of them if none is left (no wrap). */
internal fun scanFrom(candidates: List<IndexedValue<IrSignal>>, cursor: Int): List<IndexedValue<IrSignal>> =
    candidates.filter { it.index >= cursor }.ifEmpty { candidates }

/**
 * Scan state machine with one shared, persisted cursor per category.
 *
 * Transmission ownership: at most one logical IR transmission is in flight. Every path that sends or
 * changes the mode runs under [ownerMutex]: cancel-and-join the previous [transmitJob], then apply the
 * mode change, then launch the new job. The launched job holds no lock.
 */
class RemoteViewModel(
    private val loader: suspend (RemoteCategory) -> IrLibrary,
    private val transmitter: IrTransmitter,
    private val store: CursorStore,
    private val parseDispatcher: CoroutineDispatcher,
    private val clock: () -> Long,
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        RemoteUiState(hasEmitter = transmitter.hasEmitter, delayMillis = store.delayMillis),
    )
    val state: StateFlow<RemoteUiState> = mutableState.asStateFlow()

    private val libraries = mutableMapOf<RemoteCategory, IrLibrary>()
    private var loadJob: Job? = null

    private val ownerMutex = Mutex()
    private var transmitJob: Job? = null

    /** The scan of the current/last Scanning mode; assigned under [ownerMutex], filled by its job. */
    private var activeScan: ActiveScan? = null

    private class SentCode(val fileIndex: Int, val startedAt: Long)

    private class ActiveScan(
        val category: RemoteCategory,
        val buttonId: String,
        val codes: List<IndexedValue<IrSignal>>,
    ) {
        /** Codes handed to the transmitter, in order, with the [clock] time their transmission started. */
        val sent = mutableListOf<SentCode>()
    }

    init {
        showCategory(RemoteCategory.Ac)
        preloadLibraries()
    }

    /**
     * Parses the remaining libraries in the background so the dropdown can show every category's code
     * count. Runs on the main dispatcher between parses, so [libraries] is only touched from one thread.
     */
    private fun preloadLibraries() {
        viewModelScope.launch {
            loadJob?.join()
            for (category in RemoteCategory.entries) {
                if (libraries[category] != null) continue
                val library = try {
                    withContext(parseDispatcher) { loader(category) }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    continue // the category reports its own error when the user opens it
                }
                if (libraries[category] == null) libraries[category] = library
                recordTotal(category, libraries.getValue(category))
            }
        }
    }

    private fun recordTotal(category: RemoteCategory, library: IrLibrary) {
        mutableState.update { it.copy(libraryTotals = it.libraryTotals + (category to library.signals.size)) }
    }

    fun selectCategory(category: RemoteCategory) = owned {
        val current = mutableState.value
        if (category == current.category && current.error == null) return@owned
        stopTransmission()
        showCategory(category)
    }

    /** [category] is the one whose pad drew the tapped button; a stale tap for another category is ignored. */
    fun press(category: RemoteCategory, buttonId: String) = owned {
        val current = mutableState.value
        if (current.category != category || current.isLoading) return@owned
        val library = libraries[category] ?: return@owned
        val mode = current.mode
        if (mode is RemoteMode.Scanning && mode.buttonId == buttonId) {
            stopTransmission()
            mutableState.update { it.copy(mode = RemoteMode.Idle, message = RemoteMessage.Stopped) }
            return@owned
        }
        val button = category.button(buttonId) ?: return@owned
        val codes = scanFrom(scanCandidates(library, button), current.cursor)
        if (codes.isEmpty()) {
            mutableState.update { it.copy(message = RemoteMessage.NoCodes) }
            return@owned
        }

        stopTransmission()
        val scan = ActiveScan(category, buttonId, codes)
        activeScan = scan
        mutableState.update {
            it.copy(mode = scanningMode(buttonId, codes, 0, it.delayMillis), failedCount = 0, message = null)
        }
        startTransmission { scan(scan) }
    }

    /** СТОП: stops the scan; the cursor is untouched. */
    fun stop() = owned {
        stopTransmission()
        if (mutableState.value.mode != RemoteMode.Idle) {
            mutableState.update { it.copy(mode = RemoteMode.Idle, message = RemoteMessage.Stopped) }
        }
    }

    /**
     * СРАБОТАЛО: stop the scan and move the category cursor to the last code whose transmission started
     * at least [CURSOR_REWIND_MILLIS] before the tap (else the first code of this scan). Nothing is sent.
     */
    fun confirmWorked() {
        // Read before owned{}: the stop-and-join below may wait for an in-flight chunk (up to ~2 s).
        val tappedAt = clock()
        // The scan on screen at the tap: a press queued ahead of us may replace activeScan before we run.
        val tappedScan = activeScan.takeIf { mutableState.value.mode is RemoteMode.Scanning }
        owned {
            val scan = tappedScan ?: return@owned
            val current = mutableState.value
            if (current.mode !is RemoteMode.Scanning || current.category != scan.category) return@owned
            stopTransmission()
            // Re-read after the join: the scan may have finished (or aborted) meanwhile.
            if (mutableState.value.mode !is RemoteMode.Scanning) return@owned
            val library = libraries[scan.category] ?: return@owned
            val cursor = scan.sent.lastOrNull { it.startedAt <= tappedAt - CURSOR_REWIND_MILLIS }?.fileIndex
                ?: scan.codes.first().index
            store.setCursor(scan.category, cursor)
            mutableState.update {
                it.copy(
                    mode = RemoteMode.Idle,
                    cursor = cursor,
                    buttonCounts = buttonCounts(scan.category, library, cursor),
                    message = RemoteMessage.CursorSet(position = cursor + 1),
                )
            }
        }
    }

    /** СБРОС: the cursor of [category] goes back to the start of its library. */
    fun reset(category: RemoteCategory) = owned {
        val isCurrent = mutableState.value.category == category
        if (isCurrent) stopTransmission()
        store.setCursor(category, 0)
        if (isCurrent) {
            val library = libraries[category]
            mutableState.update {
                it.copy(
                    mode = RemoteMode.Idle,
                    cursor = 0,
                    buttonCounts = library?.let { lib -> buttonCounts(category, lib, 0) } ?: it.buttonCounts,
                    message = RemoteMessage.CursorReset,
                )
            }
        }
    }

    /** Moves the delay one step along [CursorStore.DELAY_STEPS_MILLIS] (clamped at the ends). */
    fun cycleDelay(direction: Int) {
        val steps = CursorStore.DELAY_STEPS_MILLIS
        val index = steps.indexOf(mutableState.value.delayMillis)
            .takeIf { it >= 0 }
            ?: steps.indexOf(CursorStore.DEFAULT_DELAY_MILLIS)
        val delayMillis = steps[(index + direction.sign).coerceIn(steps.indices)]
        store.setDelay(delayMillis)
        mutableState.update { it.copy(delayMillis = delayMillis) }
    }

    override fun onCleared() {
        transmitJob?.cancel()
        loadJob?.cancel()
    }

    private fun owned(block: suspend () -> Unit) {
        viewModelScope.launch { ownerMutex.withLock { block() } }
    }

    /** Caller holds [ownerMutex]. */
    private suspend fun stopTransmission() {
        transmitJob?.cancelAndJoin()
        transmitJob = null
    }

    /** Caller holds [ownerMutex] and has called [stopTransmission]. */
    private fun startTransmission(block: suspend () -> Unit) {
        transmitJob = viewModelScope.launch { block() }
    }

    private suspend fun scan(scan: ActiveScan) {
        val codes = scan.codes
        for ((index, code) in codes.withIndex()) {
            mutableState.update { it.copy(mode = scanningMode(scan.buttonId, codes, index, it.delayMillis)) }
            scan.sent += SentCode(code.index, clock())
            when (val result = transmitter.transmit(code.value.pattern)) {
                IrTransmitResult.Sent -> delay(mutableState.value.delayMillis)
                is IrTransmitResult.Failed -> {
                    scan.sent.removeAt(scan.sent.lastIndex) // rejected: nothing went on air
                    if (result.fatal) {
                        mutableState.update { it.copy(mode = RemoteMode.Idle, message = RemoteMessage.Error(result.reason)) }
                        return
                    }
                    mutableState.update {
                        val failed = it.failedCount + 1
                        it.copy(failedCount = failed, message = RemoteMessage.SendFailed(failed))
                    }
                }
            }
        }
        // Stay in Scanning a little longer so СРАБОТАЛО still works when one of the last codes hit.
        delay(SCAN_TAIL_MILLIS)
        mutableState.update { it.copy(mode = RemoteMode.Idle, message = RemoteMessage.ScanDone(it.failedCount)) }
    }

    /** Switches the visible category; loads its library on first use. Caller holds [ownerMutex] or is init. */
    private fun showCategory(category: RemoteCategory) {
        loadJob?.cancel()
        val cached = libraries[category]
        if (cached != null) {
            mutableState.update { categoryState(it, category, cached) }
            return
        }
        mutableState.update {
            it.copy(
                category = category,
                isLoading = true,
                error = null,
                buttonCounts = emptyMap(),
                cursor = 0,
                mode = RemoteMode.Idle,
                message = null,
                failedCount = 0,
            )
        }
        loadJob = viewModelScope.launch {
            val library = try {
                withContext(parseDispatcher) { loader(category) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                mutableState.update {
                    if (it.category != category) it
                    else it.copy(isLoading = false, error = exception.message ?: exception::class.java.simpleName)
                }
                return@launch
            }
            // The background preload may have stored this category first; keep one canonical instance.
            val canonical = libraries.getOrPut(category) { library }
            recordTotal(category, canonical)
            mutableState.update {
                if (it.category != category || !it.isLoading) it else categoryState(it, category, canonical)
            }
        }
    }

    private fun categoryState(state: RemoteUiState, category: RemoteCategory, library: IrLibrary): RemoteUiState {
        val size = library.signals.size
        // Assets may change between versions: keep a stored cursor inside the current library.
        val cursor = if (size == 0) 0 else store.cursor(category).coerceIn(0, size - 1)
        return state.copy(
            category = category,
            isLoading = false,
            error = null,
            buttonCounts = buttonCounts(category, library, cursor),
            cursor = cursor,
            mode = RemoteMode.Idle,
            message = null,
            failedCount = 0,
        )
    }

    private fun buttonCounts(category: RemoteCategory, library: IrLibrary, cursor: Int): Map<String, Int> =
        category.buttons.associate { it.id to scanFrom(scanCandidates(library, it), cursor).size }

    private fun scanningMode(buttonId: String, codes: List<IndexedValue<IrSignal>>, index: Int, delayMillis: Long) =
        RemoteMode.Scanning(
            buttonId = buttonId,
            index = index,
            total = codes.size,
            label = label(codes[index].value),
            remainingMillis = (index until codes.size).sumOf { i ->
                delayMillis + (codes[i].value.pattern.totalDurationMicros + 999L) / 1_000L
            },
        )

    private fun label(signal: IrSignal): String? = signal.model ?: signal.protocol

    companion object {
        const val SCAN_TAIL_MILLIS = 3_000L

        /** СРАБОТАЛО rewinds to the code that started this long before the tap (human reaction + device lag). */
        const val CURSOR_REWIND_MILLIS = 5_000L

        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    val assetLoader = AndroidAssetIrLibraryLoader(appContext)
                    RemoteViewModel(
                        loader = { category -> assetLoader.load(category.assetPath) },
                        transmitter = AndroidIrTransmitter(appContext),
                        store = SharedPreferencesCursorStore(appContext),
                        parseDispatcher = Dispatchers.Default,
                        clock = SystemClock::elapsedRealtime,
                    )
                }
            }
        }
    }
}

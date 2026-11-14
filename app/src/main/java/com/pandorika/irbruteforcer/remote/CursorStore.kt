package com.pandorika.irbruteforcer.remote

import android.content.Context
import android.content.SharedPreferences

/**
 * Per-category scan cursor (index into the category library's `signals`, file order) and the scan delay.
 * The cursor marks where the next scan of any button of that category starts; reset = `setCursor(category, 0)`.
 */
interface CursorStore {
    fun cursor(category: RemoteCategory): Int

    fun setCursor(category: RemoteCategory, index: Int)

    val delayMillis: Long

    fun setDelay(delayMillis: Long)

    companion object {
        val DELAY_STEPS_MILLIS: List<Long> = listOf(250L, 400L, 650L, 1_000L, 1_500L, 2_000L)
        const val DEFAULT_DELAY_MILLIS: Long = 650L
    }
}

/**
 * Same preferences file and delay key as the former found-codes store, so the saved delay survives the
 * upgrade; the old `found|…` / `anchor|…` keys are left untouched.
 */
class SharedPreferencesCursorStore(context: Context) : CursorStore {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun cursor(category: RemoteCategory): Int = prefs.getInt(cursorKey(category), 0)

    override fun setCursor(category: RemoteCategory, index: Int) {
        prefs.edit().putInt(cursorKey(category), index).apply()
    }

    override val delayMillis: Long
        get() = prefs.getLong(DELAY_KEY, CursorStore.DEFAULT_DELAY_MILLIS)

    override fun setDelay(delayMillis: Long) {
        prefs.edit().putLong(DELAY_KEY, delayMillis).apply()
    }

    private fun cursorKey(category: RemoteCategory) = "cursor|${category.name}"

    private companion object {
        const val FILE_NAME = "found_codes"
        const val DELAY_KEY = "delay_millis"
    }
}

/** Non-persistent store (tests, previews). */
class InMemoryCursorStore(
    override var delayMillis: Long = CursorStore.DEFAULT_DELAY_MILLIS,
) : CursorStore {
    private val cursors = mutableMapOf<RemoteCategory, Int>()

    override fun cursor(category: RemoteCategory): Int = cursors[category] ?: 0

    override fun setCursor(category: RemoteCategory, index: Int) {
        cursors[category] = index
    }

    override fun setDelay(delayMillis: Long) {
        this.delayMillis = delayMillis
    }
}

package com.pandorika.irbruteforcer.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class CursorStoreTest {

    @Test fun cursorDefaultsToZeroAndIsPerCategory() {
        val store = InMemoryCursorStore()
        RemoteCategory.entries.forEach { assertEquals(0, store.cursor(it)) }

        store.setCursor(RemoteCategory.Ac, 42)
        store.setCursor(RemoteCategory.Tv, 7)
        assertEquals(42, store.cursor(RemoteCategory.Ac))
        assertEquals(7, store.cursor(RemoteCategory.Tv))
        assertEquals(0, store.cursor(RemoteCategory.Fan))

        store.setCursor(RemoteCategory.Ac, 0) // СБРОС
        assertEquals(0, store.cursor(RemoteCategory.Ac))
        assertEquals(7, store.cursor(RemoteCategory.Tv))
    }

    @Test fun delayDefaultsAndPersists() {
        val store = InMemoryCursorStore()
        assertEquals(650L, store.delayMillis)
        assertEquals(CursorStore.DEFAULT_DELAY_MILLIS, store.delayMillis)
        store.setDelay(1_500L)
        assertEquals(1_500L, store.delayMillis)
        assertEquals(listOf(250L, 400L, 650L, 1_000L, 1_500L, 2_000L), CursorStore.DELAY_STEPS_MILLIS)
    }
}

package io.github.magisk317.uikit.surface

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PagerTabScaffoldTest {
    @Test
    fun retainedContentDoesNotExpandTheConfiguredPrefetchWindow() {
        assertEquals(1, resolveBeyondViewportPageCount(requested = 1, pageCount = 5))
    }

    @Test
    fun prefetchWindowIsClampedToTheAvailablePages() {
        assertEquals(0, resolveBeyondViewportPageCount(requested = -1, pageCount = 5))
        assertEquals(4, resolveBeyondViewportPageCount(requested = 8, pageCount = 5))
        assertEquals(0, resolveBeyondViewportPageCount(requested = 1, pageCount = 0))
    }
}

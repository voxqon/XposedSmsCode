package io.github.magisk317.uikit.pager

import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit coverage for the neutral pager state contract.
 *
 * Validates: Requirements 1.2, 2.3, 12.1-12.3
 */
class MainPagerStateTest {
    @Test
    fun validTargetUpdatesSelectionAndInvalidTargetPreservesIt() {
        val pager = testPager(pageCount = 5)
        val state = MainPagerState(pager, testScope())

        state.animateToPage(2)
        assertEquals(2, state.selectedPage)

        state.animateToPage(5)
        assertEquals(2, state.selectedPage)

        state.animateToPage(-1)
        assertEquals(2, state.selectedPage)
    }

    @Test
    fun newerTargetCancelsOlderNavigationWithoutChangingNewSelection() {
        val pager = testPager(pageCount = 5)
        val state = MainPagerState(pager, testScope())

        state.animateToPage(1)
        state.animateToPage(3)

        assertEquals(3, state.selectedPage)
    }

    @Test
    fun explicitCancellationAdoptsTheCurrentPagerPage() {
        val pager = testPager(pageCount = 5)
        val state = MainPagerState(pager, testScope())

        state.animateToPage(3)
        pager.requestScrollToPage(1)
        state.cancelNavigation()

        assertEquals(1, state.currentPage)
        assertEquals(1, state.selectedPage)
        assertFalse(state.isNavigating)
    }

    @Test
    fun syncAfterPagerRequestAdoptsSettledPage() {
        val pager = testPager(pageCount = 5)
        val state = MainPagerState(pager, testScope())

        pager.requestScrollToPage(4)
        state.syncPage()

        assertEquals(4, state.currentPage)
        assertEquals(4, state.settledPage)
        assertEquals(4, state.selectedPage)
    }

    @Test
    fun pageStateKeepsAValidFallbackWhenPagerHasNoPages() {
        val pager = testPager(pageCount = 0)
        val state = MainPagerState(pager, testScope())

        state.animateToPage(1)
        state.syncPage()

        assertEquals(0, state.currentPage)
        assertEquals(0, state.settledPage)
        assertEquals(0, state.selectedPage)
        assertFalse(state.isNavigating)
    }

    @Test
    fun adjacentShellContractRemainsNeutralToBusinessActivation() {
        val pager = testPager(pageCount = 5)
        val state = MainPagerState(pager, testScope())

        // The UI Kit state carries only page selection; adjacent shells cannot become active
        // through an unrelated selection update.
        state.animateToPage(2)

        assertEquals(2, state.selectedPage)
        assertTrue(state.currentPage in 0..4)
        assertTrue(state.settledPage in 0..4)
    }

    private fun testPager(pageCount: Int): PagerState = object : PagerState(0, 0f) {
        override val pageCount: Int = pageCount
    }

    private fun testScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
}

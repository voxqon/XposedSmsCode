package io.github.magisk317.uikit.surface

import io.github.magisk317.uikit.scroll.ScrollChromeState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChromeVisibilityTest {
    @Test
    fun pageTransitionResetIsHiddenAndZeroesChromeOffsets() {
        val state = ScrollChromeState()
        state.headerHeightPx = 240f
        state.onScrollDelta(80)
        assertFalse(state.isChromeVisible)
        assertTrue(state.headerOffsetY < 0f)

        state.resetForPageTransition()

        assertFalse(state.isChromeVisible)
        assertEquals(0f, state.headerOffsetY)
        assertEquals(0f, state.animatedHeaderOffsetY)
        assertEquals(0f, state.headerProgress)
    }

    @Test
    fun defaultChromePolicyKeepsMainChromeVisibleAndOffsetsAtZero() {
        val state = ScrollChromeState()
        val visibility = resolveMainChromeVisibility(
            isCompact = true,
            compactChromeRouteAvailable = true,
            keepVisible = false,
            scrollChromeVisible = state.isChromeVisible,
        )

        assertTrue(visibility.compactChromeAvailable)
        assertTrue(visibility.mainChromeVisible)
        assertTrue(visibility.compactBottomBarVisible)
        assertEquals(0f, state.headerOffsetY)
        assertEquals(0f, state.animatedHeaderOffsetY)
    }

    @Test
    fun adjacentShellCannotOwnPageChromeState() {
        val state = ScrollChromeState()

        assertTrue(state.whenScrollChromeAllowed(allowScrollHide = true) === state)
        assertEquals(null, state.whenScrollChromeAllowed(allowScrollHide = false))
    }

    @Test
    fun repeatedTargetPageResetIsIdempotent() {
        val state = ScrollChromeState()
        val controller = MainChromeController(
            scrollChromeState = state,
            pageScrollChromeState = state,
            visibility = resolveMainChromeVisibility(
                isCompact = true,
                compactChromeRouteAvailable = true,
                keepVisible = false,
                scrollChromeVisible = state.isChromeVisible,
            ),
        )

        assertTrue(controller.resetForPageTransition(2))
        state.show()
        state.headerHeightPx = 240f
        state.onScrollDelta(80)
        assertFalse(controller.resetForPageTransition(2))
        assertTrue(state.headerOffsetY < 0f)
        assertTrue(controller.resetForPageTransition(3))
        assertEquals(0f, state.headerOffsetY)
        assertFalse(state.isChromeVisible)
    }
}

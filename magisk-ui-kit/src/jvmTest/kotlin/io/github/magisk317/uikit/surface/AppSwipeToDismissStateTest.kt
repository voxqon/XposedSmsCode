package io.github.magisk317.uikit.surface

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppSwipeToDismissStateTest {
    @Test
    fun settlesToStartToEndAfterCrossingThreshold() = runBlocking {
        val state = AppSwipeToDismissState(AppSwipeToDismissValue.Settled)

        state.dragBy(delta = 60f, width = 100f)
        state.settle(width = 100f)

        assertEquals(AppSwipeToDismissValue.StartToEnd, state.currentValue)
        assertEquals(100f, state.offsetPx)
    }

    @Test
    fun returnsToSettledWhenThresholdIsNotCrossed() = runBlocking {
        val state = AppSwipeToDismissState(AppSwipeToDismissValue.Settled)

        state.dragBy(delta = 40f, width = 100f)
        state.settle(width = 100f)

        assertEquals(AppSwipeToDismissValue.Settled, state.currentValue)
        assertEquals(0f, state.offsetPx)
    }

    @Test
    fun confirmCallbackCanRejectDismiss() = runBlocking {
        val state = AppSwipeToDismissState(AppSwipeToDismissValue.Settled)
        state.setConfirmValueChange { false }

        state.dragBy(delta = -80f, width = 100f)
        state.settle(width = 100f)

        assertEquals(AppSwipeToDismissValue.Settled, state.currentValue)
        assertEquals(0f, state.offsetPx)
    }

    @Test
    fun confirmValueChangeDelegatesToCallback() {
        val state = AppSwipeToDismissState(AppSwipeToDismissValue.Settled)
        state.setConfirmValueChange { value ->
            value == AppSwipeToDismissValue.EndToStart
        }

        assertEquals(true, state.confirmValueChange(AppSwipeToDismissValue.EndToStart))
        assertEquals(false, state.confirmValueChange(AppSwipeToDismissValue.StartToEnd))
    }

    @Test
    fun snapToSettledClearsOffset() = runBlocking {
        val state = AppSwipeToDismissState(AppSwipeToDismissValue.Settled)

        state.dragBy(delta = 70f, width = 100f)
        state.snapTo(AppSwipeToDismissValue.Settled)

        assertEquals(AppSwipeToDismissValue.Settled, state.currentValue)
        assertEquals(0f, state.offsetPx)
    }
}

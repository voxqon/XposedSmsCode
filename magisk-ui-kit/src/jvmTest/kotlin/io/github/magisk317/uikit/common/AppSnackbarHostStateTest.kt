package io.github.magisk317.uikit.common

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppSnackbarHostStateTest {
    @Test
    fun `queued snackbar actions preserve order and result`() = runBlocking {
        val state = AppSnackbarHostState()
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            state.showSnackbar(
                message = "first",
                actionLabel = "Undo",
                duration = AppSnackbarDuration.Indefinite,
            )
        }
        assertEquals("first", state.currentSnackbarData?.visuals?.message)

        val second = async(start = CoroutineStart.UNDISPATCHED) {
            state.showSnackbar("second", duration = AppSnackbarDuration.Indefinite)
        }
        assertEquals("first", state.currentSnackbarData?.visuals?.message)

        state.currentSnackbarData!!.performAction()
        assertEquals(AppSnackbarResult.ActionPerformed, first.await())
        assertEquals("second", state.currentSnackbarData?.visuals?.message)

        state.currentSnackbarData!!.dismiss()
        assertEquals(AppSnackbarResult.Dismissed, second.await())
    }

    @Test
    fun `latest snackbar dismisses the visible entry before enqueueing`() = runBlocking {
        val state = AppSnackbarHostState()
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            state.showSnackbar("first", duration = AppSnackbarDuration.Indefinite)
        }
        assertEquals("first", state.currentSnackbarData?.visuals?.message)

        val latest = async(start = CoroutineStart.UNDISPATCHED) {
            state.showLatestSnackbar("latest", duration = AppSnackbarDuration.Indefinite)
        }
        assertEquals(AppSnackbarResult.Dismissed, first.await())
        assertEquals("latest", state.currentSnackbarData?.visuals?.message)

        state.currentSnackbarData!!.dismiss()
        assertEquals(AppSnackbarResult.Dismissed, latest.await())
    }
}

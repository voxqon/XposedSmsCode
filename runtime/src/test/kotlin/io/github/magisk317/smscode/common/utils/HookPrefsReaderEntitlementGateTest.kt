package io.github.magisk317.smscode.common.utils

import android.content.Context
import android.os.SystemClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Network authorization must not block locally configured SMS actions. */
class HookPrefsReaderEntitlementGateTest {

    @BeforeEach
    fun setUpClock() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1_000L
    }

    @BeforeEach
    fun setUp() {
        // No published entitlement state: the remote provider has never served
        // an always-allowed snapshot or a lease.
        HookPrefsReader.setRemotePrefsProvider { null }
        HookPrefsReader.invalidateCache()
    }

    @AfterEach
    fun tearDown() {
        HookPrefsReader.setRemotePrefsProvider(null)
        HookPrefsReader.invalidateCache()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `all distributions work without a published lease`() {
        val context = mockk<Context>(relaxed = true)
        assertTrue(HookPrefsReader.mobileAutomationAllowed(context))
    }
}

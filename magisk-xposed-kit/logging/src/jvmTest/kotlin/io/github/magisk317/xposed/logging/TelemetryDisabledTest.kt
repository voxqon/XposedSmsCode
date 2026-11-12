package io.github.magisk317.xposed.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class TelemetryDisabledTest {
    @Test
    fun `explicit enabled configuration cannot export or resolve an identity`() {
        var identityReads = 0
        MagiskOtel.configure(
            MagiskOtel.Config(
                enabled = true,
                serviceName = "test",
                endpoint = "https://127.0.0.1:1",
                serviceInstanceIdProvider = { identityReads++; error("must not be called") },
            ),
        )
        MagiskOtel.event("sms.parse", mapOf("code" to "123456"))
        assertFalse(MagiskOtel.isEnabled())
        assertEquals(0, identityReads)
    }

    @Test
    fun `debug builds and legacy preferences cannot opt into telemetry`() {
        for (userEnabled in listOf(false, true)) {
            for (debug in listOf(false, true)) {
                assertFalse(MagiskOtel.effectiveAnalyticsEnabled(userEnabled, debug))
            }
        }
        assertFalse(MagiskOtel.ANALYTICS_ENABLED_DEFAULT)
        assertEquals("", MagiskOtel.DEFAULT_ENDPOINT)
    }
}

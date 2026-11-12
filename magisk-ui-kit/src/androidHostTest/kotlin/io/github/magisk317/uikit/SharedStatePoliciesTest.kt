package io.github.magisk317.uikit

import io.github.magisk317.uikit.platform.selectAndroidDeviceDisplayName
import io.github.magisk317.uikit.state.RetainedValueCache
import io.github.magisk317.uikit.state.TimedValueCache
import io.github.magisk317.uikit.text.maskSensitiveIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SharedStatePoliciesTest {
    @Test
    fun retainedValueCacheSurvivesCompositionOwnedStateRecreation() {
        val cache = RetainedValueCache<String, Boolean>()

        assertNull(cache.get("entitlement"))
        cache.put("entitlement", true)

        assertEquals(true, cache.get("entitlement"))
    }

    @Test
    fun timedValueCacheExpiresAtTtlBoundary() {
        var nowMs = 100L
        val cache = TimedValueCache<String>(ttlMs = 50L, clockMs = { nowMs })

        cache.put("active")
        nowMs = 149L
        assertEquals("active", cache.freshOrNull())
        nowMs = 150L
        assertNull(cache.freshOrNull())
        assertEquals("active", cache.peek())
    }

    @Test
    fun sensitiveIdentifierShowsOnlyFirstAndLastFourCharacters() {
        assertEquals(
            "ABCD****5678",
            maskSensitiveIdentifier(
                value = "ABCD1234567890123456789012345678",
                expectedLength = 32,
                maskLength = 4,
            ),
        )
        assertEquals(
            "ABCD************************5678",
            maskSensitiveIdentifier(
                value = "ABCD1234567890123456789012345678",
                expectedLength = 32,
            ),
        )
        assertEquals(
            "ABCD************************5678",
            maskSensitiveIdentifier(
                value = "ABCD************************5678",
                expectedLength = 32,
            ),
        )
    }

    @Test
    fun sensitiveIdentifierRejectsMissingMalformedAndFullyMaskedValues() {
        assertNull(maskSensitiveIdentifier(null, expectedLength = 32))
        assertNull(maskSensitiveIdentifier("short", expectedLength = 32))
        assertNull(maskSensitiveIdentifier("*".repeat(32), expectedLength = 32))
    }

    @Test
    fun deviceDisplayNamePrefersMarketNameAndFallsBackSafely() {
        assertEquals(
            "Xiaomi 17",
            selectAndroidDeviceDisplayName(
                propertyValues = listOf("Xiaomi 17", "25113PN0EC"),
                fallback = "fallback",
            ),
        )
        assertEquals(
            "25113PN0EC",
            selectAndroidDeviceDisplayName(
                propertyValues = listOf("", "  "),
                fallback = "25113PN0EC",
            ),
        )
        assertEquals("Android", selectAndroidDeviceDisplayName(emptyList(), null))
    }
}

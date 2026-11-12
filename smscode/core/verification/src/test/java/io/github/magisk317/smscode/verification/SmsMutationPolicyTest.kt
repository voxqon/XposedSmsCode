package io.github.magisk317.smscode.verification

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmsMutationPolicyTest {
    @Test
    fun `short sender suffixes and alphanumeric senders must not match other senders`() {
        assertFalse(SmsMutationPolicy.matchesAddress("106900", "900"))
        assertFalse(SmsMutationPolicy.matchesAddress("Bank1234", "Shop1234"))
        assertFalse(SmsMutationPolicy.matchesAddress("+8613800138000", "13800138000"))
        assertTrue(SmsMutationPolicy.matchesAddress("+86 138-0013-8000", "+8613800138000"))
        assertTrue(SmsMutationPolicy.matchesAddress("Bank1234", "Bank1234"))
        assertFalse(SmsMutationPolicy.matchesAddress(null, "106900"))
    }

    @Test
    fun `same code or prefix does not identify a message to delete`() {
        assertFalse(SmsMutationPolicy.matchesBody("Login code 123456", "Payment code 123456"))
        assertFalse(SmsMutationPolicy.matchesBody("Login code 123456", "Login code"))
        assertFalse(SmsMutationPolicy.matchesBody("Login code", "Login code 123456"))
        assertFalse(SmsMutationPolicy.matchesBody("Login code", ""))
        assertTrue(SmsMutationPolicy.matchesBody("Code:\r\n123456", "Code:\n123456"))
    }

    @Test
    fun `mutation requires the original timestamp and handles providers without date_sent`() {
        assertTrue(SmsMutationPolicy.matchesTimestamp(9_000L, 1_000L, 1_000L))
        assertTrue(SmsMutationPolicy.matchesTimestamp(1_000L, 0L, 1_000L))
        assertFalse(SmsMutationPolicy.matchesTimestamp(1_000L, 2_000L, 1_000L))
        assertFalse(SmsMutationPolicy.matchesTimestamp(9_000L, 0L, 1_000L))
        assertFalse(SmsMutationPolicy.matchesTimestamp(0L, 0L, 0L))
        assertFalse(SmsMutationPolicy.matchesTimestamp(Long.MAX_VALUE, Long.MAX_VALUE, -1L))
    }
}

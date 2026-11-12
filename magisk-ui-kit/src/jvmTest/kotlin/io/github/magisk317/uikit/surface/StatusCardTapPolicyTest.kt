package io.github.magisk317.uikit.surface

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusCardTapPolicyTest {

    @Test
    fun firesOnlyOnFifthTapWithinWindow() {
        val policy = StatusCardTapPolicy()

        repeat(4) { index ->
            assertFalse(policy.onTap(1_000L + index * 100L), "tap ${index + 1} must not fire")
        }
        assertTrue(policy.onTap(1_000L + 4 * 100L), "5th tap must fire")
    }

    @Test
    fun counterResetsAfterFiring() {
        val policy = StatusCardTapPolicy()

        repeat(5) { index -> policy.onTap(1_000L + index * 100L) }
        assertFalse(policy.onTap(2_000L), "sequence after firing starts fresh")
    }

    @Test
    fun tapsBeyondWindowRestartTheSequence() {
        val policy = StatusCardTapPolicy()

        repeat(4) { index -> policy.onTap(1_000L + index * 100L) }
        // 1.9s after the 4th tap: outside the 1.8s window, so the count restarts at 1.
        assertFalse(policy.onTap(1_400L + StatusCardTapPolicy.DEFAULT_WINDOW_MS + 100L))
        repeat(3) { index -> assertFalse(policy.onTap(2_000L + index * 100L)) }
        assertTrue(policy.onTap(2_000L + 3 * 100L), "5th tap of the restarted sequence fires")
    }

    @Test
    fun resetDropsInFlightSequence() {
        val policy = StatusCardTapPolicy()

        repeat(4) { index -> policy.onTap(1_000L + index * 100L) }
        policy.reset()

        assertFalse(policy.onTap(1_500L), "tap after reset counts as the first of a new sequence")
    }

    @Test
    fun customWindowAndTapCountAreHonored() {
        val policy = StatusCardTapPolicy(windowMs = 500L, requiredTaps = 2)

        assertFalse(policy.onTap(1_000L))
        assertFalse(policy.onTap(1_600L), "0.6s gap exceeds the custom 0.5s window")
        assertTrue(policy.onTap(2_100L), "2nd tap within the custom window fires")
    }
}

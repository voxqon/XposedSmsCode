package io.github.magisk317.uikit.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Shared click policy for status hero cards.
 *
 * Interaction contract:
 * - entitlement missing (`isEntitled == false`): a single tap invokes [onActivateClick];
 * - entitlement present: [onDiagnosticsToggle] fires only after [requiredTaps] taps inside a
 *   [windowMs] window (default 5 taps within 1.8s), matching the historical diagnostics easter egg;
 * - when neither callback applies, the plain [onClick] fallback is used as-is.
 *
 * Keeping the tap-window rules here avoids re-implementing the same counter in every parent app.
 */
class StatusCardTapPolicy(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val requiredTaps: Int = DEFAULT_REQUIRED_TAPS,
) {
    private var tapCount: Int = 0
    private var windowStartedAtMs: Long = 0L

    /**
     * Feeds one tap into the policy.
     *
     * @param nowMs monotonic clock reading (e.g. `nowMillis()`).
     * @return true when the tap sequence reached [requiredTaps] within [windowMs]; the counter is
     *   reset in that case so the next sequence starts fresh.
     */
    fun onTap(nowMs: Long): Boolean {
        val withinWindow = nowMs - windowStartedAtMs <= windowMs
        tapCount = if (withinWindow) tapCount + 1 else 1
        windowStartedAtMs = nowMs
        if (tapCount < requiredTaps) return false
        reset()
        return true
    }

    /** Clears any in-flight tap sequence. */
    fun reset() {
        tapCount = 0
        windowStartedAtMs = 0L
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 1_800L
        const val DEFAULT_REQUIRED_TAPS = 5
    }
}

/** Remembers a [StatusCardTapPolicy] across recompositions of the hosting card. */
@Composable
fun rememberStatusCardTapPolicy(
    windowMs: Long = StatusCardTapPolicy.DEFAULT_WINDOW_MS,
    requiredTaps: Int = StatusCardTapPolicy.DEFAULT_REQUIRED_TAPS,
): StatusCardTapPolicy = remember(windowMs, requiredTaps) { StatusCardTapPolicy(windowMs, requiredTaps) }

/**
 * Resolves the click handler of a status card from the shared interaction contract.
 *
 * @param isEntitled whether mobile automation entitlement is currently granted.
 * @param onActivateClick invoked on a single tap while entitlement is missing.
 * @param onDiagnosticsToggle invoked after the multi-tap diagnostics sequence while entitled.
 * @param onClick legacy fallback used when no policy callback is provided.
 * @return the resolved handler, or null when the card should not be clickable.
 */
@Composable
fun rememberStatusCardClickHandler(
    isEntitled: Boolean,
    onActivateClick: (() -> Unit)?,
    onDiagnosticsToggle: (() -> Unit)?,
    onClick: (() -> Unit)? = null,
): (() -> Unit)? {
    if (!isEntitled && onActivateClick != null) return onActivateClick
    if (onDiagnosticsToggle != null) {
        val tapPolicy = rememberStatusCardTapPolicy()
        return {
            if (tapPolicy.onTap(nowMillis())) {
                onDiagnosticsToggle()
            }
        }
    }
    return onClick
}

expect fun nowMillis(): Long

package io.github.magisk317.uikit.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import io.github.magisk317.uikit.scroll.BindScrollChromeHeaderOffset
import io.github.magisk317.uikit.scroll.ScrollChromeState
import io.github.magisk317.uikit.scroll.rememberScrollChromeState

@Immutable
data class MainChromeVisibility(
    val compactChromeAvailable: Boolean,
    val mainChromeVisible: Boolean,
    val compactBottomBarVisible: Boolean,
)

fun resolveMainChromeVisible(
    compactChromeAvailable: Boolean,
    keepVisible: Boolean,
    scrollChromeVisible: Boolean,
): Boolean {
    return !compactChromeAvailable || keepVisible || scrollChromeVisible
}

fun resolveMainChromeVisibility(
    isCompact: Boolean,
    compactChromeRouteAvailable: Boolean,
    keepVisible: Boolean,
    scrollChromeVisible: Boolean,
): MainChromeVisibility {
    val compactChromeAvailable = isCompact && compactChromeRouteAvailable
    val mainChromeVisible = resolveMainChromeVisible(
        compactChromeAvailable = compactChromeAvailable,
        keepVisible = keepVisible,
        scrollChromeVisible = scrollChromeVisible,
    )
    return MainChromeVisibility(
        compactChromeAvailable = compactChromeAvailable,
        mainChromeVisible = mainChromeVisible,
        compactBottomBarVisible = compactChromeAvailable && mainChromeVisible,
    )
}

fun ScrollChromeState.whenScrollChromeAllowed(allowScrollHide: Boolean): ScrollChromeState? {
    return if (allowScrollHide) this else null
}

@Stable
class MainChromeController internal constructor(
    val scrollChromeState: ScrollChromeState,
    val pageScrollChromeState: ScrollChromeState?,
    val visibility: MainChromeVisibility,
) {
    private object InitialResetKey
    private var lastResetKey: Any? = InitialResetKey

    val compactChromeAvailable: Boolean
        get() = visibility.compactChromeAvailable

    val mainChromeVisible: Boolean
        get() = visibility.mainChromeVisible

    val compactBottomBarVisible: Boolean
        get() = visibility.compactBottomBarVisible

    /**
     * Resets Chrome once for each target-page key. Repeated settle notifications for the same
     * page are intentionally idempotent and do not restart the reset animation/state transition.
     */
    fun resetForPageTransition(targetPage: Any?): Boolean {
        if (targetPage == lastResetKey) return false
        lastResetKey = targetPage
        scrollChromeState.resetForPageTransition()
        return true
    }
}

/**
 * Parent apps supply chrome *policy* only:
 * - [rememberMainChromeController.compactChromeRouteAvailable]: whether the compact bottom bar
 *   is allowed on this route (main tabs yes, deep detail pages no).
 * - [rememberMainChromeController.keepVisible]: force chrome shown even when the list could scroll.
 *   Typical formula: `keepVisible = !allowScrollHide`.
 *   List-style tabs (apps / records / events) set `allowScrollHide = true` so chrome can hide.
 *   Overview / settings / config hubs keep chrome pinned (`allowScrollHide = false`).
 * - MiPush may keep an explicit route allowlist for `keepVisible` when overview-like routes
 *   overlap with list ranks; siblings usually use the complementary `!allowScrollHide` form.
 */

@Composable
fun rememberMainChromeController(
    isCompact: Boolean,
    compactChromeRouteAvailable: Boolean,
    keepVisible: Boolean,
    allowScrollHide: Boolean,
    resetKey: Any?,
    animationMillis: Int = CHROME_VISIBILITY_ANIMATION_MILLIS,
): MainChromeController {
    val scrollChromeState = rememberScrollChromeState()
    BindScrollChromeHeaderOffset(
        chromeState = scrollChromeState,
        animationMillis = animationMillis,
    )
    val chromeController = MainChromeController(
        scrollChromeState = scrollChromeState,
        pageScrollChromeState = scrollChromeState.whenScrollChromeAllowed(allowScrollHide),
        visibility = resolveMainChromeVisibility(
            isCompact = isCompact,
            compactChromeRouteAvailable = compactChromeRouteAvailable,
            keepVisible = keepVisible,
            scrollChromeVisible = scrollChromeState.isChromeVisible,
        ),
    )
    LaunchedEffect(resetKey) {
        chromeController.resetForPageTransition(resetKey)
    }
    return chromeController
}

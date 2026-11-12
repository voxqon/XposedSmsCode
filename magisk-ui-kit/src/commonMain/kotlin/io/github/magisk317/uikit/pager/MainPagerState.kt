package io.github.magisk317.uikit.pager

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class MainPagerState internal constructor(
    val pagerState: PagerState,
    private val scope: CoroutineScope,
) {
    private var lastValidPage by mutableIntStateOf(pagerState.currentPage.coerceInPageRange())

    /** The page requested by the latest neutral navigation intent. */
    var selectedPage by mutableIntStateOf(lastValidPage)
        private set

    /** The page currently owning pager visuals during a drag or animation. */
    val currentPage: Int
        get() = pagerState.currentPage.validOr(lastValidPage)

    /** The page whose pager motion has settled and may be activated by the caller. */
    val settledPage: Int
        get() = pagerState.settledPage.validOr(lastValidPage)

    var isNavigating by mutableStateOf(false)
        private set

    private var navigationJob: Job? = null

    /**
     * Requests a page change. Invalid pages are ignored and do not alter the last valid state.
     * Cancelling an in-flight request is safe: the replacement request owns its own work.
     */
    fun animateToPage(target: Int) {
        if (!target.isValidPage() || target == selectedPage) return
        navigationJob?.cancel()
        selectedPage = target
        lastValidPage = target
        isNavigating = true
        navigationJob = scope.launch {
            val job = coroutineContext.job
            try {
                pagerState.springAnimateToPage(target)
            } finally {
                if (navigationJob == job) {
                    isNavigating = false
                    syncPage()
                }
            }
        }
    }

    /** Synchronizes selection after a cancelled gesture or an external pager update. */
    fun syncPage() {
        val page = currentPage
        if (!page.isValidPage()) return
        lastValidPage = page
        if (!isNavigating) selectedPage = page
    }

    /** Instantly jumps to [target] without animation. Cancels any in-flight animation. */
    fun scrollToPage(target: Int) {
        if (!target.isValidPage()) return
        navigationJob?.cancel()
        navigationJob = null
        isNavigating = false
        selectedPage = target
        lastValidPage = target
        scope.launch {
            pagerState.scrollToPage(target)
        }
    }

    /** Cancels in-flight pager motion and adopts the pager's last valid visual page. */
    fun cancelNavigation() {
        navigationJob?.cancel()
        navigationJob = null
        isNavigating = false
        syncPage()
    }

    private fun Int.isValidPage(): Boolean = this in 0 until pagerState.pageCount

    private fun Int.validOr(fallback: Int): Int = if (isValidPage()) {
        this
    } else {
        fallback
    }

    private fun Int.coerceInPageRange(): Int = when {
        pagerState.pageCount <= 0 -> 0
        else -> coerceIn(0, pagerState.pageCount - 1)
    }
}

@Composable
fun rememberMainPagerState(
    pageCount: () -> Int,
    initialPage: Int = 0,
    scope: CoroutineScope = rememberCoroutineScope(),
): MainPagerState {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage.coerceAtLeast(0),
        pageCount = pageCount,
    )
    return remember(pagerState, scope) { MainPagerState(pagerState, scope) }
}

private suspend fun PagerState.springAnimateToPage(target: Int) {
    if (target !in 0 until pageCount) return
    var snapToTarget = false
    scroll(MutatePriority.UserInput) {
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val distance = target - currentPage - currentPageOffsetFraction
        val pixels = distance * pageSize
        if (abs(pixels) <= MIN_SCROLL_DELTA_PX) return@scroll
        var consumedScroll = 0f
        var skipScroll = false
        androidx.compose.animation.core.Animatable(0f).animateTo(
            targetValue = pixels,
            animationSpec = PagerNavigationSpringSpec,
        ) {
            if (skipScroll) return@animateTo
            val delta = value - consumedScroll
            if (abs(delta) > MIN_SCROLL_DELTA_PX) {
                val consumed = scrollBy(delta)
                consumedScroll += consumed
                if (abs(delta - consumed) > MAX_SCROLL_CONSUMPTION_ERROR_PX) {
                    snapToTarget = true
                    skipScroll = true
                }
            } else {
                consumedScroll = value
            }
            if (abs(velocity) < MIN_SETTLE_VELOCITY_PX &&
                abs(pixels - consumedScroll) < MAX_SETTLE_DISTANCE_PX
            ) {
                skipScroll = true
            }
        }
        val remaining = pixels - consumedScroll
        if (abs(remaining) > MIN_SCROLL_DELTA_PX) scrollBy(remaining)
    }
    if (snapToTarget || currentPage != target) scrollToPage(target)
}

private const val MIN_SCROLL_DELTA_PX = 0.5f
private const val MAX_SCROLL_CONSUMPTION_ERROR_PX = 0.1f
private const val MIN_SETTLE_VELOCITY_PX = 0.1f
private const val MAX_SETTLE_DISTANCE_PX = 1f

@Immutable
data class PagerTabSpec(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String? = null,
)

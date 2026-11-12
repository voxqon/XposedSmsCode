package io.github.magisk317.uikit.scroll

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val CHROME_SCROLL_THRESHOLD_PX = 32
private const val BOTTOM_REVEAL_SCROLL_THRESHOLD_PX = 96

class ScrollChromeState {
    var isChromeVisible by mutableStateOf(true)
        private set

    var headerOffsetY by mutableFloatStateOf(0f)
        private set

    var animatedHeaderOffsetY by mutableFloatStateOf(0f)

    var headerHeightPx by mutableFloatStateOf(0f)

    val headerProgress: Float
        get() = if (headerHeightPx > 0f) {
            (headerOffsetY / headerHeightPx).coerceIn(-1f, 0f)
        } else {
            0f
        }

    private var accumulatedScrollDelta = 0
    private var wasAtBottom = false
    private var accumulatedBottomRevealDelta = 0

    fun show() {
        isChromeVisible = true
        accumulatedScrollDelta = 0
        wasAtBottom = false
        accumulatedBottomRevealDelta = 0
    }

    fun hide() {
        isChromeVisible = false
        accumulatedScrollDelta = 0
        wasAtBottom = false
        accumulatedBottomRevealDelta = 0
    }

    fun animateToTop() {
        headerOffsetY = 0f
        animatedHeaderOffsetY = 0f
        show()
    }

    /**
     * Resets shared Chrome for a page transition without allowing the outgoing page to leak
     * visual state into the incoming page. The next scroll event may show Chrome again.
     */
    fun resetForPageTransition() {
        headerOffsetY = 0f
        animatedHeaderOffsetY = 0f
        hide()
    }

    fun onScrollDelta(delta: Int, atTop: Boolean = false, atBottom: Boolean = false) {
        if (atTop) {
            headerOffsetY = 0f
            animatedHeaderOffsetY = 0f
            show()
            return
        }
        if (delta == 0) return

        headerOffsetY = max(-headerHeightPx, min(0f, headerOffsetY - delta))

        when {
            atBottom -> {
                wasAtBottom = true
                accumulatedBottomRevealDelta = 0
                if (delta < 0) accumulatedScrollDelta = 0 else handleDirectionalDelta(delta)
            }
            wasAtBottom && delta < 0 -> handleBottomExitDelta(delta)
            else -> {
                if (delta > 0) wasAtBottom = false
                accumulatedBottomRevealDelta = 0
                handleDirectionalDelta(delta)
            }
        }
    }

    private fun handleBottomExitDelta(delta: Int) {
        accumulatedBottomRevealDelta = if (accumulatedBottomRevealDelta.signMatches(delta)) {
            accumulatedBottomRevealDelta + delta
        } else {
            delta
        }
        if (abs(accumulatedBottomRevealDelta) < BOTTOM_REVEAL_SCROLL_THRESHOLD_PX) return
        show()
    }

    private fun handleDirectionalDelta(delta: Int, thresholdPx: Int = CHROME_SCROLL_THRESHOLD_PX) {
        accumulatedScrollDelta = if (accumulatedScrollDelta.signMatches(delta)) {
            accumulatedScrollDelta + delta
        } else {
            delta
        }
        if (abs(accumulatedScrollDelta) < thresholdPx) return
        if (accumulatedScrollDelta > 0) hide() else show()
    }
}

private fun Int.signMatches(other: Int): Boolean {
    return this == 0 || (this > 0 && other > 0) || (this < 0 && other < 0)
}

@Composable
fun rememberScrollChromeState(): ScrollChromeState = remember { ScrollChromeState() }

@Composable
fun BindScrollChromeHeaderOffset(
    chromeState: ScrollChromeState,
    animationMillis: Int = 160,
) {
    val animatedHeaderOffsetY by animateFloatAsState(
        targetValue = chromeState.headerOffsetY,
        animationSpec = if (chromeState.headerOffsetY >= 0f) {
            tween(animationMillis)
        } else {
            tween(0)
        },
        label = "headerOffsetY",
    )
    LaunchedEffect(chromeState, animatedHeaderOffsetY) {
        chromeState.animatedHeaderOffsetY = animatedHeaderOffsetY
    }
}

@Composable
fun ReportLazyListScrollToChrome(
    state: LazyListState,
    chromeState: ScrollChromeState?,
) {
    androidx.compose.runtime.LaunchedEffect(state, chromeState) {
        val target = chromeState ?: return@LaunchedEffect
        var previousIndex = state.firstVisibleItemIndex
        var previousOffset = state.firstVisibleItemScrollOffset
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                val delta = when {
                    index > previousIndex -> CHROME_SCROLL_THRESHOLD_PX
                    index < previousIndex -> -CHROME_SCROLL_THRESHOLD_PX
                    else -> offset - previousOffset
                }
                val layoutInfo = state.layoutInfo
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                val atBottom = lastVisible != null &&
                    lastVisible.index >= layoutInfo.totalItemsCount - 1 &&
                    lastVisible.offset + lastVisible.size <= layoutInfo.viewportEndOffset
                target.onScrollDelta(delta, atTop = index == 0 && offset == 0, atBottom = atBottom)
                previousIndex = index
                previousOffset = offset
            }
    }
}

@Composable
fun ReportScrollStateToChrome(
    state: ScrollState,
    chromeState: ScrollChromeState?,
) {
    androidx.compose.runtime.LaunchedEffect(state, chromeState) {
        val target = chromeState ?: return@LaunchedEffect
        var previousValue = state.value
        snapshotFlow { state.value }
            .distinctUntilChanged()
            .collect { value ->
                target.onScrollDelta(value - previousValue, atTop = value == 0, atBottom = value >= state.maxValue)
                previousValue = value
            }
    }
}

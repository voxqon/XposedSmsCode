package io.github.magisk317.uikit.surface

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Transparent double-tap hotspot for quick return-to-top.
 *
 * Place it over the top bar strip (between the navigation icon and the
 * actions, e.g. `Modifier.align(Alignment.TopStart).padding(start = 72.dp,
 * top = statusBarTop, end = 112.dp).fillMaxWidth().height(64.dp)`) and wire
 * [onDoubleTap] to `scope.launch { listState.animateScrollToItem(0) }`.
 *
 * Pairs with [ScrollToTopFAB] / [ScrollToTopEffect] for the full affordance
 * set. The overlay draws nothing, never changes layout, and only reacts to
 * double taps — single taps fall through to the content underneath.
 */
@Composable
fun DoubleTapToTopOverlay(
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { currentOnDoubleTap() },
            )
        },
    )
}

package io.github.magisk317.uikit.surface

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BoxScope.ScrollToTopFAB(
    listState: LazyListState,
    visible: Boolean = true,
    extraBottomPadding: Dp = 0.dp,
) {
    val isAtTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
    }
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = visible && !isAtTop,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 24.dp + extraBottomPadding),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        AppFloatingActionButton(
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            imageVector = Icons.Filled.VerticalAlignTop,
            contentDescription = "Scroll to top",
        )
    }
}

@Composable
fun BoxScope.ScrollToTopFAB(
    scrollState: ScrollState,
    visible: Boolean = true,
    extraBottomPadding: Dp = 0.dp,
) {
    val isAtTop by remember { derivedStateOf { scrollState.value == 0 } }
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = visible && !isAtTop,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 24.dp + extraBottomPadding),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        AppFloatingActionButton(
            onClick = { scope.launch { scrollState.animateScrollTo(0) } },
            imageVector = Icons.Filled.VerticalAlignTop,
            contentDescription = "Scroll to top",
        )
    }
}

/**
 * Scrolls [lazyListState] to the top when [signal] changes.
 * Increment the signal (e.g. from a tab re-select callback) to trigger the scroll.
 */
@Composable
fun ScrollToTopEffect(
    lazyListState: LazyListState,
    signal: Int,
) {
    LaunchedEffect(signal) {
        if (signal > 0) lazyListState.scrollToItem(0)
    }
}

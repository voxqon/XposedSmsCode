package io.github.magisk317.uikit.surface

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import io.github.magisk317.uikit.pager.MainPagerState

data class PagerTransitionState(
    val currentPage: Int,
    val targetPage: Int,
    val settledPage: Int,
)

/**
 * Main-tab shell backed by a real horizontal pager. Deep routes remain the responsibility of
 * the caller; this component only coordinates top-level pages and navigation chrome.
 *
 * @param pagerVisible when false the pager layer fades to alpha 0 with [TAB_NAV_TRANSITION_MS].
 *   The composition tree is preserved (no expensive recreate or second ViewRootImpl) but the
 *   GPU draw pass is skipped, keeping detail-page transitions smooth.  Defaults to `true`.
 */
@Composable
fun PagerTabScaffold(
    tabs: List<MainTabSpec>,
    pagerState: MainPagerState,
    isCompact: Boolean,
    chromeController: MainChromeController,
    modifier: Modifier = Modifier,
    onTabReselected: (Int) -> Unit = {},
    pagerVisible: Boolean = true,
    beyondViewportPageCount: Int = 0,
    userScrollEnabled: Boolean = true,
    reserveCompactBottomBarSpace: Boolean = true,
    floatingBottomBar: Boolean = false,
    bottomBarBlur: Boolean = false,
    bottomBarBackdrop: Boolean = false,
    railHeader: (@Composable ColumnScope.() -> Unit)? = null,
    animationMillis: Int = CHROME_VISIBILITY_ANIMATION_MILLIS,
    onChromeTransition: (PagerTransitionState) -> Unit = {},
    retainPageContentAfterFirstFrame: Boolean = false,
    showSystemBarsScrim: Boolean = true,
    /**
     * Optional tab-tap override. Defaults to [MainPagerState.animateToPage]. Callers that host a
     * navigation graph behind the pager must pass a root-aware policy: while a deep (non top-level)
     * route keeps the chrome visible, animateToPage alone moves an invisible pager and never pops
     * the route, leaving the tab bar unable to return to its page.
     */
    onTabSelected: ((index: Int) -> Unit)? = null,
    onDragProgress: ((fractionalIndex: Float) -> Unit)? = null,
    /**
     * Optional neutral shell for pages composed beside [pagerState.currentPage]. The shell must
     * remain presentation-only; business activation belongs to the caller's settled-page policy.
     */
    adjacentPageShell: @Composable (page: Int) -> Unit = {},
    /**
     * Optional diagnostics sink, called once per recomposition of the pager content gate and of
     * the pager alpha. A background-only pager can then be attributed to a stuck deferral or to a
     * transparent pager layer from the caller's own log instead of by guessing. Null by default,
     * so callers that collect no diagnostics pay nothing.
     */
    onDiagnostic: ((String) -> Unit)? = null,
    pageContent: @Composable (page: Int, contentPadding: PaddingValues) -> Unit,
) {
    var retainedContentReady by remember { mutableStateOf(false) }
    LaunchedEffect(retainPageContentAfterFirstFrame) {
        if (!retainPageContentAfterFirstFrame || retainedContentReady) return@LaunchedEffect
        // Keep cold-start and the first meaningful frame light. Compose retained pages only after
        // the current page has rendered, so subsequent tab transitions can reuse their trees.
        withFrameNanos { }
        withFrameNanos { }
        retainedContentReady = true
    }
    // Pages that have been composed with real content at least once. The page the pager starts
    // on is pre-composed so it never needs deferral. Seeding page 0 unconditionally instead left
    // any restore that starts on another page rendering an empty placeholder, and that
    // placeholder had no way back: the deferral effect below was keyed on a page number that does
    // not change when the deferral is entered.
    val composedPages = remember { mutableSetOf(pagerState.currentPage) }
    // The page currently in its one-frame deferral window (placeholder shown, real content
    // deferred to the next frame). This keeps the pager animation smooth when a heavy page
    // enters the viewport for the first time.
    var deferredPage by remember { mutableIntStateOf(-1) }
    var deferredContentReady by remember { mutableStateOf(false) }
    // Keyed on the deferred page so the placeholder written during composition always schedules
    // its own resolution one frame later, whatever the current page happens to be.
    LaunchedEffect(deferredPage) {
        if (deferredPage >= 0 && !deferredContentReady) {
            deferredContentReady = true
        }
    }
    LaunchedEffect(
        pagerState.currentPage,
        pagerState.selectedPage,
        pagerState.settledPage,
    ) {
        onChromeTransition(
            PagerTransitionState(
                currentPage = pagerState.currentPage,
                targetPage = pagerState.selectedPage,
                settledPage = pagerState.settledPage,
            ),
        )
    }

    LaunchedEffect(pagerState.currentPage, pagerState.settledPage, pagerState.isNavigating) {
        if (!pagerState.isNavigating) {
            pagerState.syncPage()
        }
    }

    // Alpha hides the pager during detail transitions without destroying the composition
    // tree.  This avoids the expensive recreate cycle and the second ViewRootImpl that
    // AnimatedVisibility would spawn.
    val pagerAlpha by animateFloatAsState(
        targetValue = if (pagerVisible) 1f else 0f,
        animationSpec = tween(TAB_NAV_TRANSITION_MS, easing = EaseInOut),
        label = "pagerAlpha",
    )
    if (onDiagnostic != null) {
        LaunchedEffect(pagerVisible) {
            onDiagnostic("PagerAlpha: pagerVisible=$pagerVisible")
        }
    }
    val calculatedSelectedIndex = if (pagerState.isNavigating) pagerState.selectedPage else pagerState.currentPage
    MainTabScaffold(
        tabs = tabs,
        selectedIndex = calculatedSelectedIndex,
        isCompact = isCompact,
        chromeController = chromeController,
        onTabSelected = onTabSelected ?: { pagerState.animateToPage(it) },
        onTabReselected = onTabReselected,
        modifier = modifier.alpha(pagerAlpha),
        railHeader = railHeader,
        animationMillis = animationMillis,
        reserveCompactBottomBarSpace = reserveCompactBottomBarSpace,
        showSystemBarsScrim = showSystemBarsScrim,
        floatingBottomBar = floatingBottomBar,
        bottomBarBlur = bottomBarBlur,
        bottomBarBackdrop = bottomBarBackdrop,
        onDragProgress = onDragProgress,
    ) { contentPadding ->
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState.pagerState,
                // Retention controls whether pages in the configured prefetch window receive
                // their real content. It must not silently widen that window to every tab: doing
                // so keeps all heavyweight feature trees in each pager measure/draw pass.
                beyondViewportPageCount = resolveBeyondViewportPageCount(
                    requested = beyondViewportPageCount,
                    pageCount = tabs.size,
                ),
                userScrollEnabled = userScrollEnabled,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val isCurrentPage = page == pagerState.currentPage
                val isInViewport = retainedContentReady
                val wasComposed = page in composedPages
                val showPlaceholder = isCurrentPage && !wasComposed && !deferredContentReady
                if (onDiagnostic != null) {
                    val branch = when {
                        showPlaceholder -> "placeholder"
                        isCurrentPage || isInViewport || wasComposed -> "content"
                        else -> "shell"
                    }
                    // Built during composition so it reports the decision that was just taken
                    // rather than a later state read, then emitted as a side effect because
                    // composition is not a place for observable work.
                    val report = "PagerGate: page=$page current=${pagerState.currentPage} " +
                        "retainedReady=$retainedContentReady composed=$wasComposed " +
                        "deferredPage=$deferredPage deferredReady=$deferredContentReady " +
                        "branch=$branch"
                    SideEffect { onDiagnostic(report) }
                }
                when {
                    // Current page entering the viewport for the first time: defer its heavy
                    // content by one frame so the pager animation is not blocked.
                    showPlaceholder -> {
                        if (deferredPage != page) {
                            deferredPage = page
                            deferredContentReady = false
                        }
                        adjacentPageShell(page)
                    }
                    // Real content: either already composed, or the deferral frame has elapsed.
                    isCurrentPage || isInViewport || wasComposed -> {
                        composedPages += page
                        if (deferredPage == page) {
                            deferredPage = -1
                            deferredContentReady = false
                        }
                        pageContent(page, contentPadding)
                    }
                    // Pages in the prefetch window but not yet current.
                    else -> adjacentPageShell(page)
                }
            }
        }
    }
}

internal fun resolveBeyondViewportPageCount(requested: Int, pageCount: Int): Int =
    requested.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

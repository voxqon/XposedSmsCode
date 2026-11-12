package io.github.magisk317.uikit.surface

import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/** Double-tap window for reselecting the current main tab (refresh). */
const val MAIN_TAB_DOUBLE_TAP_REFRESH_WINDOW_MS = 350L

@Immutable
data class MainTabSpec(
    val label: String,
    val icon: ImageVector,
    val testTag: String? = null,
    val badgeLabel: String? = null,
)

/**
 * Shared main-tab shell used by parent apps:
 * compact bottom navigation chrome + optional expanded rail + system-bar scrim.
 *
 * Parents own routing ([selectedIndex], [onTabSelected], [onTabReselected]) and pass a
 * [content] slot (typically a [androidx.navigation.compose.NavHost]).
 * [onContentBottomPaddingChanged] reports the initial effective compact-navigation inset and
 * later changes after successful composition, without repeating unchanged values. Replacing the
 * callback does not replay the current inset; the latest callback receives the next change.
 */
@Composable
fun MainTabScaffold(
    tabs: List<MainTabSpec>,
    selectedIndex: Int,
    isCompact: Boolean,
    chromeController: MainChromeController,
    onTabSelected: (index: Int) -> Unit,
    onTabReselected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    railHeader: (@Composable ColumnScope.() -> Unit)? = null,
    alwaysShowLabel: Boolean = false,
    chromeShape: Shape = RectangleShape,
    chromeTopPadding: Dp = 0.dp,
    animationMillis: Int = CHROME_VISIBILITY_ANIMATION_MILLIS,
    reserveCompactBottomBarSpace: Boolean = false,
    floatingBottomBar: Boolean = false,
    bottomBarBlur: Boolean = false,
    bottomBarBackdrop: Boolean = false,
    showSystemBarsScrim: Boolean = true,
    onDragProgress: ((fractionalIndex: Float) -> Unit)? = null,
    onContentBottomPaddingChanged: (Dp) -> Unit = {},
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    var compactBottomBarHeight by remember { mutableStateOf(0.dp) }
    val tabLastTapAt = remember { mutableStateMapOf<Int, Long>() }
    val compactBottomBarVisible = chromeController.compactBottomBarVisible
    // miuix-blur declares API 33 because its graphics-layer implementation uses RuntimeShader.
    // Keep the artifact optional at runtime: API 28-32 stay on the translucent surface fallback.
    val navigationBackdrop = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        floatingBottomBar &&
        bottomBarBlur &&
        bottomBarBackdrop
    ) {
        rememberLayerBackdrop()
    } else {
        null
    }
    // Keep the page viewport stable while the bar animates to avoid a second measure/layout pass.
    val contentBottomPadding =
        if (compactBottomBarVisible || reserveCompactBottomBarSpace) compactBottomBarHeight else 0.dp

    ReportContentBottomPadding(
        contentBottomPadding = contentBottomPadding,
        onContentBottomPaddingChanged = onContentBottomPaddingChanged,
    )

    fun handleTabClick(index: Int, selected: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val last = tabLastTapAt[index] ?: 0L
        tabLastTapAt[index] = now
        if (selected) {
            if (now - last <= MAIN_TAB_DOUBLE_TAP_REFRESH_WINDOW_MS) {
                onTabReselected(index)
            }
            return
        }
        chromeController.scrollChromeState.animateToTop()
        onTabSelected(index)
    }

    val navigationItems = tabs.mapIndexed { index, tab ->
        AppNavigationItemSpec(
            label = tab.label,
            icon = tab.icon,
            selected = index == selectedIndex,
            onClick = { handleTabClick(index, index == selectedIndex) },
            testTag = tab.testTag,
            badgeLabel = tab.badgeLabel,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(navigationBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                // Fill the sampling layer with an opaque page surface so the floating glass
                // navigation bar never frosts over transparent holes: pages that do not paint
                // their own surface (bare-Box pages) would leave the recorded layer empty
                // where their content does not reach, and miuix-blur samples an empty region
                // as black — the pill then renders dark instead of frosted page content.
                // The fill is placed AFTER layerBackdrop so it lands inside the recorded
                // subtree, and gated on the backdrop being active so non-glass chrome
                // configurations keep their previous look.
                .then(
                    if (navigationBackdrop != null) {
                        Modifier.background(pageSurfaceColor())
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (!isCompact) {
                AppNavigationRail(
                    items = navigationItems,
                    modifier = Modifier.fillMaxHeight(),
                    header = railHeader,
                    alwaysShowLabel = alwaysShowLabel,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                    ),
            ) {
                content(PaddingValues(bottom = contentBottomPadding))
            }
        }

        AnimatedCompactBottomNavigationChrome(
            visible = compactBottomBarVisible,
            items = navigationItems,
            alwaysShowLabel = alwaysShowLabel,
            animationMillis = animationMillis,
            chromeShape = chromeShape,
            chromeTopPadding = chromeTopPadding,
            floating = floatingBottomBar,
            blurred = bottomBarBlur,
            backdrop = navigationBackdrop,
            onHeightChanged = { compactBottomBarHeight = it },
            onDragProgress = onDragProgress,
        )

        AnimatedSystemBarsScrim(
            visible = resolveSystemBarsScrimVisible(
                showSystemBarsScrim = showSystemBarsScrim,
            ),
            animationMillis = animationMillis,
        )
    }
}

internal fun resolveSystemBarsScrimVisible(
    showSystemBarsScrim: Boolean,
): Boolean = showSystemBarsScrim

/** Reports effective content padding after successful composition and suppresses duplicates. */
@Composable
private fun ReportContentBottomPadding(
    contentBottomPadding: Dp,
    onContentBottomPaddingChanged: (Dp) -> Unit,
) {
    val reporter = remember { ContentBottomPaddingReporter() }
    SideEffect {
        reporter.report(contentBottomPadding, onContentBottomPaddingChanged)
    }
}

internal class ContentBottomPaddingReporter {
    private var lastReportedPadding: Dp? = null

    fun report(contentBottomPadding: Dp, onContentBottomPaddingChanged: (Dp) -> Unit) {
        if (contentBottomPadding == lastReportedPadding) return
        lastReportedPadding = contentBottomPadding
        onContentBottomPaddingChanged(contentBottomPadding)
    }
}

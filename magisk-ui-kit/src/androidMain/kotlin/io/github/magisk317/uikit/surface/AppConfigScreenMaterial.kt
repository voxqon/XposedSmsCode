@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.scroll.ScrollChromeState
import io.github.magisk317.uikit.theme.LocalUiKitSurfaceBlur
import kotlinx.coroutines.launch

/**
 * Expressive/Material chrome for the per-app config screens: overlay search
 * header with a collapsing M3 top bar (exitUntilCollapsed) above the
 * pager-coordinated app list. The overlay height measured by
 * [OverlayHeaderScaffold] becomes the list's top padding, and the material
 * scroll behavior drives both the bar collapse and the list's nested
 * scroll connection. When the theme backdrop is active the collapsed search
 * bar frosts over the recorded list (glass model shared with
 * OverviewScreenMaterial / ApplicationListExpressive).
 * Quick return-to-top: double-tap the bar title strip or use the FAB that
 * appears once the bar has collapsed.
 *
 * Host-supplied values: [title]/[searchPlaceholder]/[backContentDescription]
 * resolve product strings; [bottomPadding] carries the host's effective
 * bottom inset; [actionIcon]/[actionsAfterSearch] place the settings action.
 */
@Composable
fun AppConfigScreenMaterial(
    title: String,
    searchPlaceholder: String,
    backContentDescription: String?,
    onBack: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    actionIcon: ImageVector,
    scrollChromeState: ScrollChromeState?,
    searchState: SearchOverlayState,
    listState: LazyListState,
    bottomPadding: Dp,
    actionsAfterSearch: Boolean = false,
    body: @Composable (PaddingValues, Modifier) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val topGlass = rememberUiKitGlassTopBar()
    val glassOn = LocalUiKitSurfaceBlur.current.usesBackdrop
    val scrollScope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val defaultTopPadding = statusBarTop + 156.dp
    OverlayHeaderScaffold(
        fallbackTopPadding = defaultTopPadding,
        bottomPadding = bottomPadding,
        headerOffsetY = scrollChromeState?.animatedHeaderOffsetY ?: 0f,
        onHeaderHeightChanged = { scrollChromeState?.headerHeightPx = it.toFloat() },
        overlayModifier = Modifier
            .fillMaxWidth(),
        overlay = {
            Box(modifier = Modifier.fillMaxWidth()) {
                SearchOverlayContent(
                    state = searchState,
                    title = title,
                    searchPlaceholder = searchPlaceholder,
                    navigationIcon = if (onBack != null) {
                        {
                            AppIconButton(onClick = onBack) {
                                AppIcon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = backContentDescription,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    actions = {
                        AppIconButton(onClick = onOpenSettings) {
                            AppIcon(
                                imageVector = actionIcon,
                                contentDescription = null,
                            )
                        }
                    },
                    actionsAfterSearch = actionsAfterSearch,
                    scrollBehavior = scrollBehavior,
                    glassTopBar = topGlass,
                )
                // Hidden while the search field is expanded so the hotspot
                // never fights the text field for taps.
                if (!searchState.expanded) {
                    DoubleTapToTopOverlay(
                        onDoubleTap = {
                            scrollScope.launch { listState.animateScrollToItem(0) }
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 72.dp, top = statusBarTop, end = 112.dp)
                            .fillMaxWidth()
                            .height(64.dp),
                    )
                }
            }
        },
        content = { overlayPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                body(
                    overlayPadding,
                    Modifier
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .then(if (glassOn) topGlass.contentRecorder() else Modifier),
                )
                ScrollToTopFAB(
                    listState = listState,
                    visible = scrollChromeState?.isChromeVisible != true,
                    extraBottomPadding = bottomPadding,
                )
            }
        },
    )
}

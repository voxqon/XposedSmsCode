package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material3 expanded breakpoint (dp). */
const val EXPANDED_WIDTH_DP = 840
/** Material3 medium breakpoint (dp). Below this width the UI is treated as compact. */
const val MEDIUM_WIDTH_DP = 600

internal enum class NavigationWidthClass {
    Compact,
    Medium,
    Expanded,
}

internal fun navigationWidthClass(width: Dp): NavigationWidthClass = when {
    width >= EXPANDED_WIDTH_DP.dp -> NavigationWidthClass.Expanded
    width >= MEDIUM_WIDTH_DP.dp -> NavigationWidthClass.Medium
    else -> NavigationWidthClass.Compact
}

/**
 * Returns true when the current container width is below the [MEDIUM_WIDTH_DP] breakpoint,
 * i.e. the layout should use the compact (bottom-bar) treatment rather than a rail/drawer.
 */
@Composable
fun rememberIsCompactWidth(): Boolean {
    val widthDp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    return remember(widthDp) { navigationWidthClass(widthDp) == NavigationWidthClass.Compact }
}

/**
 * Responsive scaffold that switches between bottom bar, navigation rail, or drawer based on
 * its current Compose container width rather than the device's full display width.
 */
@Composable
fun NavigationSuiteScaffold(
    bottomBar: @Composable () -> Unit,
    navigationRail: (@Composable () -> Unit)? = null,
    drawerContent: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val widthDp = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    when (navigationWidthClass(widthDp)) {
        NavigationWidthClass.Expanded -> {
            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet {
                        drawerContent?.invoke(this)
                    }
                },
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    navigationRail?.invoke()
                    Box(modifier = Modifier.weight(1f)) {
                        content(PaddingValues())
                    }
                }
            }
        }
        NavigationWidthClass.Medium -> {
            Row(modifier = Modifier.fillMaxSize()) {
                navigationRail?.invoke()
                Box(modifier = Modifier.weight(1f)) {
                    content(PaddingValues())
                }
            }
        }
        NavigationWidthClass.Compact -> {
            Scaffold(
                bottomBar = bottomBar,
                content = content,
            )
        }
    }
}

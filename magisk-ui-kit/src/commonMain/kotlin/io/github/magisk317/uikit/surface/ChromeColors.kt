package io.github.magisk317.uikit.surface

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

const val CHROME_SURFACE_ALPHA = 0.96f
const val CHROME_SEARCH_FIELD_ALPHA = 0.50f

/**
 * Opaque page surface matching what [AppScaffold] paints for the active style
 * (Miuix -> `MiuixTheme.colorScheme.surface`, Expressive -> `MaterialTheme.colorScheme.background`).
 *
 * Used to fill chrome backdrop layers (e.g. the floating navigation bar's `layerBackdrop`
 * sampling layer): pages that do not paint their own surface would otherwise leave
 * transparent holes in the recorded layer, and miuix-blur samples a transparent hole as
 * black — the glass then renders a dark pill instead of frosted page content.
 */
@Composable
fun pageSurfaceColor(): Color {
    return if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.background
    }
}

/**
 * Chrome (top bar / page shell) surface for the active style.
 *
 * Miuix -> opaque `MiuixTheme.colorScheme.surface` (miuix chrome has no translucent
 * alpha treatment; the stock library paints an opaque surface, so a container color
 * sampled from an M3 token would show a foreign tint under miuix themes).
 * Expressive -> `MaterialTheme.colorScheme.surfaceContainerHigh` at [alpha].
 *
 * The [alpha] argument is honoured on the Expressive branch only: the Miuix branch is
 * opaque by design.
 */
@Composable
fun chromeSurfaceColor(alpha: Float = CHROME_SURFACE_ALPHA): Color {
    return if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
    }
}

@Composable
fun chromeStatusBarColor(alpha: Float = CHROME_SURFACE_ALPHA): Color {
    return chromeSurfaceColor(alpha)
}

@Composable
fun chromeNavigationBarColor(alpha: Float = CHROME_SURFACE_ALPHA): Color {
    return chromeSurfaceColor(alpha)
}

@Composable
fun workspaceSearchFieldContainerColor(alpha: Float = CHROME_SEARCH_FIELD_ALPHA): Color {
    return MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun chromeTopAppBarColors(): TopAppBarColors {
    val chromeColor = chromeSurfaceColor()
    return TopAppBarDefaults.topAppBarColors(
        containerColor = chromeColor,
        scrolledContainerColor = chromeColor,
    )
}

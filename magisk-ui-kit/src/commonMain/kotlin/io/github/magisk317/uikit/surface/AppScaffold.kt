package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.HorizontalDivider as MaterialHorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold as MaterialScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixHorizontalDivider
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun Color.orElse(fallback: Color): Color =
    if (this == Color.Unspecified) fallback else this

/**
 * Shared page shell. The content slot receives the same [PaddingValues] contract for both
 * Material/Expressive and Miuix, while each style owns its native inset and popup behavior.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    contentWindowInsets: WindowInsets? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixScaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = snackbarHost,
            containerColor = containerColor.orElse(MiuixTheme.colorScheme.surface),
            contentWindowInsets = contentWindowInsets
                ?: WindowInsets.systemBars.union(WindowInsets.displayCutout),
            content = content,
        )
        return
    }

    MaterialScaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = containerColor.orElse(MaterialTheme.colorScheme.background),
        contentWindowInsets = contentWindowInsets ?: WindowInsets.systemBars,
        content = content,
    )
}

@Composable
fun AppHorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    thickness: Dp = Dp.Hairline,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixHorizontalDivider(
            modifier = modifier,
            thickness = thickness,
            color = color.orElse(MiuixTheme.colorScheme.dividerLine),
        )
        return
    }

    MaterialHorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color.orElse(MaterialTheme.colorScheme.outlineVariant),
    )
}

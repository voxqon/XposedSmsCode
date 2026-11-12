package io.github.magisk317.uikit.scroll

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * Adds the miuix end-of-scroll haptic to a scrollable container. No-op outside [UiKitStyle.Miuix],
 * so callers can apply it unconditionally.
 *
 * Overscroll needs no modifier here: the theme host wraps content in `MiuixTheme`, which swaps
 * `LocalOverscrollFactory` for the miuix rebound effect. Applying `overScrollVertical()` on top of
 * that would stack a second rebound, which is why only the haptic is added.
 */
@Composable
fun Modifier.uiKitScrollEndHaptic(enabled: Boolean = true): Modifier {
    if (!enabled || LocalUiKitStyle.current != UiKitStyle.Miuix) return this
    return this.scrollEndHaptic()
}

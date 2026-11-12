package io.github.magisk317.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val superLarge: Dp = 32.dp,
) {
    fun scaled(factor: Float): Spacing = copy(
        extraSmall = extraSmall * factor,
        small = small * factor,
        medium = medium * factor,
        large = large * factor,
        extraLarge = extraLarge * factor,
        superLarge = superLarge * factor,
    )
}

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalUiKitLayoutScale = staticCompositionLocalOf { UiKitLayoutScale.Standard }

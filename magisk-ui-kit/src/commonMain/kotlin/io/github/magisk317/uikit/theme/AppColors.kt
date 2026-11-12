package io.github.magisk317.uikit.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Semantic colour roles resolved against the active [UiKitStyle].
 *
 * Shared bodies (XposedSmsCode / MiPushFramework / xinyi-relay) keep
 * `MaterialTheme` mounted under the Miuix theme, so a raw
 * `MaterialTheme.colorScheme.*` reference inside a shared body keeps the
 * Material palette in a Miuix chrome. Picking a role instead keeps colour
 * ownership with the active style, the same way [io.github.magisk317.uikit.text.AppTextRole]
 * owns typography.
 *
 * Roles with no exact miuix counterpart fall back to the closest token:
 * [AppColorRole.Tertiary] -> primary variant (miuix has no plain tertiary
 * accent), [AppColorRole.Scrim] -> black (miuix has no scrim token, matching
 * the Material baseline), [AppColorRole.SurfaceContainerLow] -> surfaceContainer
 * (miuix has no `low` step), [AppColorRole.OutlineVariant] -> dividerLine
 * (miuix's divider token is the outline-variant analogue).
 */
enum class AppColorRole {
    OnSurface,
    OnSurfaceVariant,
    Primary,
    OnPrimary,
    PrimaryContainer,
    OnPrimaryContainer,
    Secondary,
    SecondaryContainer,
    OnSecondaryContainer,
    Error,
    OnError,
    ErrorContainer,
    OnErrorContainer,
    Outline,
    SurfaceContainer,
    SurfaceContainerHigh,
    SurfaceVariant,
    Background,
    Tertiary,
    Scrim,
    SurfaceContainerLow,
    SurfaceContainerHighest,
    OutlineVariant,
    TertiaryContainer,
    OnTertiaryContainer,
    OnBackground,
}

/** Resolves [role] to the active style's own colour token. */
@Composable
fun appColor(role: AppColorRole): Color {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) return miuixColor(role)
    return materialColor(role)
}

@Composable
private fun miuixColor(role: AppColorRole): Color = when (role) {
    AppColorRole.OnSurface -> MiuixTheme.colorScheme.onSurface
    AppColorRole.OnSurfaceVariant -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    AppColorRole.Primary -> MiuixTheme.colorScheme.primary
    AppColorRole.OnPrimary -> MiuixTheme.colorScheme.onPrimary
    AppColorRole.PrimaryContainer -> MiuixTheme.colorScheme.primaryContainer
    AppColorRole.OnPrimaryContainer -> MiuixTheme.colorScheme.onPrimaryContainer
    AppColorRole.Secondary -> MiuixTheme.colorScheme.secondary
    AppColorRole.SecondaryContainer -> MiuixTheme.colorScheme.secondaryContainer
    AppColorRole.OnSecondaryContainer -> MiuixTheme.colorScheme.onSecondaryContainer
    AppColorRole.Error -> MiuixTheme.colorScheme.error
    AppColorRole.OnError -> MiuixTheme.colorScheme.onError
    AppColorRole.ErrorContainer -> MiuixTheme.colorScheme.errorContainer
    AppColorRole.OnErrorContainer -> MiuixTheme.colorScheme.onErrorContainer
    AppColorRole.Outline -> MiuixTheme.colorScheme.outline
    AppColorRole.SurfaceContainer -> MiuixTheme.colorScheme.surfaceContainer
    AppColorRole.SurfaceContainerHigh -> MiuixTheme.colorScheme.surfaceContainerHigh
    AppColorRole.SurfaceVariant -> MiuixTheme.colorScheme.surfaceVariant
    AppColorRole.Background -> MiuixTheme.colorScheme.background
    AppColorRole.Tertiary -> MiuixTheme.colorScheme.primaryVariant
    AppColorRole.Scrim -> Color.Black
    // miuix exposes no surfaceContainerLow: fall back to the nearest container.
    AppColorRole.SurfaceContainerLow -> MiuixTheme.colorScheme.surfaceContainer
    AppColorRole.SurfaceContainerHighest -> MiuixTheme.colorScheme.surfaceContainerHighest
    // miuix's divider token is the outline-variant analogue.
    AppColorRole.OutlineVariant -> MiuixTheme.colorScheme.dividerLine
    AppColorRole.TertiaryContainer -> MiuixTheme.colorScheme.tertiaryContainer
    AppColorRole.OnTertiaryContainer -> MiuixTheme.colorScheme.onTertiaryContainer
    AppColorRole.OnBackground -> MiuixTheme.colorScheme.onBackground
}

@Composable
private fun materialColor(role: AppColorRole): Color = when (role) {
    AppColorRole.OnSurface -> MaterialTheme.colorScheme.onSurface
    AppColorRole.OnSurfaceVariant -> MaterialTheme.colorScheme.onSurfaceVariant
    AppColorRole.Primary -> MaterialTheme.colorScheme.primary
    AppColorRole.OnPrimary -> MaterialTheme.colorScheme.onPrimary
    AppColorRole.PrimaryContainer -> MaterialTheme.colorScheme.primaryContainer
    AppColorRole.OnPrimaryContainer -> MaterialTheme.colorScheme.onPrimaryContainer
    AppColorRole.Secondary -> MaterialTheme.colorScheme.secondary
    AppColorRole.SecondaryContainer -> MaterialTheme.colorScheme.secondaryContainer
    AppColorRole.OnSecondaryContainer -> MaterialTheme.colorScheme.onSecondaryContainer
    AppColorRole.Error -> MaterialTheme.colorScheme.error
    AppColorRole.OnError -> MaterialTheme.colorScheme.onError
    AppColorRole.ErrorContainer -> MaterialTheme.colorScheme.errorContainer
    AppColorRole.OnErrorContainer -> MaterialTheme.colorScheme.onErrorContainer
    AppColorRole.Outline -> MaterialTheme.colorScheme.outline
    AppColorRole.SurfaceContainer -> MaterialTheme.colorScheme.surfaceContainer
    AppColorRole.SurfaceContainerHigh -> MaterialTheme.colorScheme.surfaceContainerHigh
    AppColorRole.SurfaceVariant -> MaterialTheme.colorScheme.surfaceVariant
    AppColorRole.Background -> MaterialTheme.colorScheme.background
    AppColorRole.Tertiary -> MaterialTheme.colorScheme.tertiary
    AppColorRole.Scrim -> MaterialTheme.colorScheme.scrim
    AppColorRole.SurfaceContainerLow -> MaterialTheme.colorScheme.surfaceContainerLow
    AppColorRole.SurfaceContainerHighest -> MaterialTheme.colorScheme.surfaceContainerHighest
    AppColorRole.OutlineVariant -> MaterialTheme.colorScheme.outlineVariant
    AppColorRole.TertiaryContainer -> MaterialTheme.colorScheme.tertiaryContainer
    AppColorRole.OnTertiaryContainer -> MaterialTheme.colorScheme.onTertiaryContainer
    AppColorRole.OnBackground -> MaterialTheme.colorScheme.onBackground
}

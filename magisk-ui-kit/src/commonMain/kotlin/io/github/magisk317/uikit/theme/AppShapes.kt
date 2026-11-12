package io.github.magisk317.uikit.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle

/**
 * Semantic shape roles resolved against the active [UiKitStyle].
 *
 * miuix 0.9.4 exposes no shape tokens (its surfaces use fixed corner radii),
 * so the Miuix track maps each role to a fixed radius that matches the common
 * MIUI chrome scale. The M3 track returns the corresponding
 * [MaterialTheme.shapes] member, keeping the Material scale intact.
 */
enum class AppShapeRole {
    Small,
    Medium,
    Large,
    ExtraLarge,
}

/** Resolves [role] to the active style's own [Shape]. */
@Composable
@ReadOnlyComposable
fun appShape(role: AppShapeRole): Shape {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) return miuixShape(role)
    return materialShape(role)
}

@Composable
@ReadOnlyComposable
private fun materialShape(role: AppShapeRole): Shape = when (role) {
    AppShapeRole.Small -> MaterialTheme.shapes.small
    AppShapeRole.Medium -> MaterialTheme.shapes.medium
    AppShapeRole.Large -> MaterialTheme.shapes.large
    AppShapeRole.ExtraLarge -> MaterialTheme.shapes.extraLarge
}

/** Fixed radii for the Miuix track (miuix 0.9.4 exposes no shape tokens). */
private fun miuixShape(role: AppShapeRole): Shape = when (role) {
    AppShapeRole.Small -> RoundedCornerShape(8.dp)
    AppShapeRole.Medium -> RoundedCornerShape(12.dp)
    AppShapeRole.Large -> RoundedCornerShape(16.dp)
    AppShapeRole.ExtraLarge -> RoundedCornerShape(28.dp)
}

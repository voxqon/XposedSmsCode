package io.github.magisk317.uikit.theme

import androidx.compose.runtime.Immutable

/**
 * Discrete layout-density choices for page spacing.
 *
 * This intentionally does not change [androidx.compose.ui.unit.Density] or system fontScale;
 * text metrics and minimum interactive targets remain owned by the platform/components.
 */
@Immutable
enum class UiKitLayoutScale(val value: Int, val spacingFactor: Float) {
    Compact(value = 0, spacingFactor = 0.92f),
    Standard(value = 1, spacingFactor = 1f),
    Comfortable(value = 2, spacingFactor = 1.08f),
    ;

    companion object {
        fun fromValue(value: Int): UiKitLayoutScale =
            entries.firstOrNull { it.value == value } ?: Standard
    }
}

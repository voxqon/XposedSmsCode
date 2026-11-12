package io.github.magisk317.uikit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The two UI tracks a host app can render in.
 *
 * Naming note: `Expressive` follows Material 3's official variant name
 * (`MaterialExpressiveTheme` / `ExperimentalMaterial3ExpressiveApi`), and it is
 * what the user-facing settings label shows. The track *file suffix* is
 * `*Material.kt` (see the dual-track gate), not `*Expressive.kt`; the enum
 * constant is kept as `Expressive` so persisted int values and user-facing
 * strings stay stable. Do not rename the constant without a migration plan.
 */
enum class UiKitStyle(val value: Int) {
    Expressive(0),
    Miuix(1),
    ;

    companion object {
        /** Style used when the host app has no persisted preference. */
        val Default: UiKitStyle get() = Expressive

        fun fromValue(value: Int): UiKitStyle {
            return entries.firstOrNull { it.value == value } ?: Default
        }
    }
}

internal val LocalUiKitStyle = staticCompositionLocalOf { UiKitStyle.Expressive }

@Composable
@ReadOnlyComposable
fun currentUiKitStyle(): UiKitStyle = LocalUiKitStyle.current

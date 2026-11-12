package io.github.magisk317.uikit.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The JVM target has no wallpaper-derived dynamic color; the static scheme is used. */
@Composable
internal actual fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

/** System bars are an Android concept; the desktop window shell owns its own chrome. */
@Composable
actual fun UpdateSystemBars(darkTheme: Boolean) = Unit

/** The multiplatform runtime ships no Material Expressive theme; the plain M3 theme keeps the same contract. */
@Composable
internal actual fun kitMaterialTheme(colorScheme: ColorScheme, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Desktop has no RuntimeShader backdrop; surfaces keep the static (non-blur) tier. */
@Composable
actual fun UiKitSurfaceBlurRoot(
    enabled: Boolean,
    backgroundColor: Color,
    content: @Composable () -> Unit,
) {
    content()
}

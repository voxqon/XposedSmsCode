package io.github.magisk317.uikit.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Wallpaper-derived scheme on Android 12+; null below API 31 or when dynamic color is off. */
@Composable
internal actual fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal actual fun kitMaterialTheme(colorScheme: ColorScheme, content: @Composable () -> Unit) {
    MaterialExpressiveTheme(colorScheme = colorScheme, content = content)
}

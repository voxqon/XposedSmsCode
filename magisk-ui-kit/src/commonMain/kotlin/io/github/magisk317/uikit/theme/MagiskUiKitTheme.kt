package io.github.magisk317.uikit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor as LocalMaterialContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme
import androidx.compose.ui.graphics.compositeOver
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

private const val LUMA_WEIGHT_RED = 0.2126f
private const val LUMA_WEIGHT_GREEN = 0.7152f
private const val LUMA_WEIGHT_BLUE = 0.0722f
private const val LUMA_LIGHT_THRESHOLD = 0.55f

enum class MagiskThemeMode(val value: Int) {
    System(0),
    Light(1),
    Dark(2),
    PureBlack(3),
    ;

    companion object {
        fun fromValue(value: Int): MagiskThemeMode {
            return entries.firstOrNull { it.value == value } ?: System
        }
    }
}

/**
 * The dark flag [MagiskUiKitTheme] resolved for the current theme mode. Components whose
 * palette can't be read off MiuixTheme -- Expressive hosts leave the miuix theme at its
 * light defaults -- read this instead of guessing from resolved colours.
 */
val LocalMagiskDarkTheme = compositionLocalOf<Boolean?> { null }

private fun Color.isLightTone(): Boolean =
    red * LUMA_WEIGHT_RED + green * LUMA_WEIGHT_GREEN + blue * LUMA_WEIGHT_BLUE > LUMA_LIGHT_THRESHOLD

private fun accentColorScheme(accent: Color, darkTheme: Boolean): ColorScheme {
    val background = if (darkTheme) Color(0xFF141218) else Color(0xFFFFFBFE)
    val container = accent
        .copy(alpha = if (darkTheme) 0.36f else 0.14f)
        .compositeOver(background)
    val onAccent = if (accent.isLightTone()) Color.Black else Color.White
    val onContainer = if (container.isLightTone()) Color.Black else Color.White

    return if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = container,
            onSecondaryContainer = onContainer,
            tertiary = accent,
            onTertiary = onAccent,
            tertiaryContainer = container,
            onTertiaryContainer = onContainer,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = container,
            onSecondaryContainer = onContainer,
            tertiary = accent,
            onTertiary = onAccent,
            tertiaryContainer = container,
            onTertiaryContainer = onContainer,
        )
    }
}

@Composable
fun MagiskUiKitTheme(
    themeMode: Int,
    dynamicColor: Boolean = true,
    accentColor: Int = 0,
    monetEnabled: Boolean = false,
    paletteStyle: UiKitPaletteStyle = UiKitPaletteStyle.TonalSpot,
    colorSpec: UiKitColorSpec = UiKitColorSpec.Spec2025,
    surfaceBlur: Boolean = false,
    uiKitStyle: UiKitStyle = UiKitStyle.Expressive,
    layoutScale: UiKitLayoutScale = UiKitLayoutScale.Standard,
    content: @Composable () -> Unit,
) {
    val resolvedMode = MagiskThemeMode.fromValue(themeMode)
    val darkTheme = when (resolvedMode) {
        MagiskThemeMode.Light -> false
        MagiskThemeMode.Dark, MagiskThemeMode.PureBlack -> true
        MagiskThemeMode.System -> isSystemInDarkTheme()
    }

    val explicitAccent = accentColor.takeUnless { it == 0 }?.let(::Color)
    val effectiveColorSpec = colorSpec.effectiveFor(paletteStyle)
    val useDynamicColor = dynamicColor && explicitAccent == null
    // Platform dynamic color: the wallpaper scheme on Android 12+, null on every other
    // target (previews always pass dynamicColor = false so they stay device-independent).
    val dynamicLight = if (useDynamicColor) systemDynamicColorScheme(darkTheme = false) else null
    val dynamicDark = if (useDynamicColor) systemDynamicColorScheme(darkTheme = true) else null
    val seedColor = explicitAccent ?: (if (darkTheme) dynamicDark else dynamicLight)?.primary
        ?: Color(0xFF6750A4)
    val monetScheme = if (monetEnabled) {
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = darkTheme,
            isAmoled = resolvedMode == MagiskThemeMode.PureBlack,
            style = paletteStyle.toMaterialStyle(),
            specVersion = effectiveColorSpec.toMaterialSpec(),
        )
    } else {
        null
    }
    val lightScheme = when {
        monetEnabled -> rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = false,
            isAmoled = false,
            style = paletteStyle.toMaterialStyle(),
            specVersion = effectiveColorSpec.toMaterialSpec(),
        )
        explicitAccent != null -> accentColorScheme(explicitAccent, darkTheme = false)
        dynamicLight != null -> dynamicLight
        else -> lightColorScheme()
    }
    val darkScheme = when {
        monetEnabled -> monetScheme ?: rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = true,
            isAmoled = false,
            style = paletteStyle.toMaterialStyle(),
            specVersion = effectiveColorSpec.toMaterialSpec(),
        )
        explicitAccent != null -> accentColorScheme(explicitAccent, darkTheme = true)
        dynamicDark != null -> dynamicDark
        else -> darkColorScheme()
    }
    val colorScheme: ColorScheme = when {
        resolvedMode == MagiskThemeMode.PureBlack -> {
            darkScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceContainer = Color.Black,
                surfaceContainerLow = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerHigh = Color.Black,
                surfaceContainerHighest = Color.Black,
            )
        }
        darkTheme -> darkScheme
        else -> lightScheme
    }

    UpdateSystemBars(darkTheme)

    CompositionLocalProvider(
        LocalUiKitStyle provides uiKitStyle,
        LocalUiKitLayoutScale provides layoutScale,
        LocalMagiskDarkTheme provides darkTheme,
        LocalSpacing provides Spacing().scaled(layoutScale.spacingFactor),
    ) {
        kitMaterialTheme(colorScheme = colorScheme) {
            UiKitSurfaceBlurRoot(
                enabled = surfaceBlur,
                backgroundColor = colorScheme.background,
            ) {
                if (uiKitStyle == UiKitStyle.Miuix) {
                    val controller = remember(darkTheme, resolvedMode, seedColor, paletteStyle, effectiveColorSpec) {
                        val colorMode = when (resolvedMode) {
                            MagiskThemeMode.System -> ColorSchemeMode.System
                            MagiskThemeMode.Light -> ColorSchemeMode.Light
                            MagiskThemeMode.Dark, MagiskThemeMode.PureBlack -> ColorSchemeMode.Dark
                        }
                        ThemeController(
                            colorSchemeMode = colorMode,
                            keyColor = seedColor,
                            isDark = darkTheme,
                            paletteStyle = paletteStyle.toMiuixPaletteStyle(),
                            colorSpec = effectiveColorSpec.toMiuixColorSpec(),
                        )
                    }
                    val baseMiuixColors = controller.currentColors()
                    val miuixColors = remember(baseMiuixColors, resolvedMode) {
                        if (resolvedMode == MagiskThemeMode.PureBlack) {
                            // AMOLED: keep accent/primary roles, drive every surface role to pure black.
                            baseMiuixColors.copy(
                                background = Color.Black,
                                surface = Color.Black,
                                surfaceVariant = Color.Black,
                                surfaceContainer = Color.Black,
                                surfaceContainerHigh = Color.Black,
                                surfaceContainerHighest = Color.Black,
                            )
                        } else {
                            baseMiuixColors
                        }
                    }
                    MiuixTheme(colors = miuixColors) {
                        // Provide both content-color locals: miuix components read the kit's own
                        // LocalContentColor, while material3 Text/Surface children read
                        // androidx.compose.material3.LocalContentColor (compose.ui dropped its
                        // LocalContentColor; without this, bare material3 Text falls back to
                        // Color.Black and renders illegible in Miuix style).
                        CompositionLocalProvider(
                            LocalContentColor provides miuixColors.onBackground,
                            LocalMaterialContentColor provides miuixColors.onBackground,
                        ) {
                            content()
                        }
                    }
                } else {
                    // Expressive/Material chrome: material3 Surface provides its own
                    // LocalContentColor, but Text outside any Surface (e.g. directly under
                    // Scaffold) would fall back to the default Color.Black.
                    CompositionLocalProvider(LocalMaterialContentColor provides colorScheme.onSurface) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Platform dynamic color scheme: Android resolves the wallpaper palette on API 31+, every
 * other target returns null and the theme falls back to the static Material 3 scheme.
 */
@Composable
internal expect fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** System-bar appearance for [darkTheme]; a no-op on platforms without system bars. */
@Composable
expect fun UpdateSystemBars(darkTheme: Boolean)

/**
 * The Material 3 theme root: Android uses the Expressive variant, other targets the plain
 * Material 3 theme (the multiplatform runtime ships no Expressive variant yet).
 */
@Composable
internal expect fun kitMaterialTheme(colorScheme: ColorScheme, content: @Composable () -> Unit)

/**
 * Hosts the shared surface-blur root: Android allocates a RuntimeShader backdrop when the
 * platform supports it, other targets degrade to the static (non-blur) tier.
 */
@Composable
expect fun UiKitSurfaceBlurRoot(
    enabled: Boolean,
    backgroundColor: Color,
    content: @Composable () -> Unit,
)

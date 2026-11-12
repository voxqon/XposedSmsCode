package io.github.magisk317.uikit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

private const val STATIC_FALLBACK_MIN_ALPHA = 0.88f
private const val BACKDROP_MAX_ALPHA = 0.78f

/** The rendering tier selected for shared surfaces after capability detection. */
enum class UiKitSurfaceBlurTier {
    Disabled,
    StaticFallback,
    Backdrop,
}

fun resolveSurfaceBlurTier(enabled: Boolean, shaderSupported: Boolean): UiKitSurfaceBlurTier = when {
    !enabled -> UiKitSurfaceBlurTier.Disabled
    shaderSupported -> UiKitSurfaceBlurTier.Backdrop
    else -> UiKitSurfaceBlurTier.StaticFallback
}

@Immutable
data class UiKitSurfaceBlurState(
    val enabled: Boolean,
    val tier: UiKitSurfaceBlurTier,
    val backdrop: LayerBackdrop?,
) {
    val usesBackdrop: Boolean
        get() = enabled && tier == UiKitSurfaceBlurTier.Backdrop && backdrop != null

    companion object {
        val Disabled = UiKitSurfaceBlurState(
            enabled = false,
            tier = UiKitSurfaceBlurTier.Disabled,
            backdrop = null,
        )
    }
}

val LocalUiKitSurfaceBlur = compositionLocalOf { UiKitSurfaceBlurState.Disabled }

/**
 * Hosts the shared backdrop source for UI Kit surfaces.
 *
 * The policy is deliberately progressive: the preference can remain enabled on every device,
 * but RuntimeShader/Backdrop is only allocated when the platform supports it. Unsupported or
 * unavailable devices keep an opaque-ish static surface and never lose content or input.
 */
@Composable
actual fun UiKitSurfaceBlurRoot(
    enabled: Boolean,
    backgroundColor: Color,
    content: @Composable () -> Unit,
) {
    val shaderSupported = remember { isRuntimeShaderSupported() }
    val tier = resolveSurfaceBlurTier(enabled, shaderSupported)
    val backdrop = if (tier == UiKitSurfaceBlurTier.Backdrop) {
        rememberLayerBackdrop {
            drawRect(backgroundColor)
            drawContent()
        }
    } else {
        null
    }
    val state = remember(enabled, tier, backdrop) {
        UiKitSurfaceBlurState(
            enabled = enabled,
            tier = tier,
            backdrop = backdrop,
        )
    }

    // Provide the backdrop via CompositionLocal only.
    // Do NOT apply layerBackdrop to the root Box — that creates infinite recursion
    // because the backdrop tries to capture its own content as background.
    CompositionLocalProvider(LocalUiKitSurfaceBlur provides state) {
        content()
    }
}

fun UiKitSurfaceBlurState.staticSurfaceAlpha(defaultAlpha: Float): Float = when (tier) {
    UiKitSurfaceBlurTier.Disabled -> defaultAlpha
    UiKitSurfaceBlurTier.StaticFallback -> defaultAlpha.coerceAtLeast(STATIC_FALLBACK_MIN_ALPHA)
    UiKitSurfaceBlurTier.Backdrop -> defaultAlpha.coerceAtMost(BACKDROP_MAX_ALPHA)
}

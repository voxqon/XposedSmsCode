package io.github.magisk317.uikit.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.magisk317.uikit.glass.vibrancy
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitSurfaceBlur
import io.github.magisk317.uikit.theme.staticSurfaceAlpha
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop

/**
 * Applies the shared surface blur tier without changing layout or hit targets.
 * The modifier is a no-op unless the theme host has a supported backdrop source.
 */
@Composable
fun Modifier.uiKitSurfaceBlur(
    shape: Shape,
    surfaceColor: Color,
    blurRadius: Dp = 18.dp,
): Modifier {
    val state = LocalUiKitSurfaceBlur.current
    val backdrop = state.backdrop
    if (!state.usesBackdrop || backdrop == null) return this

    val resolvedColor = surfaceColor.copy(alpha = state.staticSurfaceAlpha(surfaceColor.alpha))
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            blur(
                radiusX = blurRadius.toPx(),
                radiusY = blurRadius.toPx(),
            )
        },
        onDrawSurface = {
            drawRect(resolvedColor)
        },
    )
}

@Composable
fun surfaceBlurContainerColor(color: Color): Color {
    val state = LocalUiKitSurfaceBlur.current
    return if (state.usesBackdrop) Color.Transparent else {
        color.copy(alpha = state.staticSurfaceAlpha(color.alpha))
    }
}

/** Bundles recording/sampling for a page glass top bar without leaking miuix-blur types to app modules. */
class UiKitGlassTopBarScope internal constructor(internal val backdrop: LayerBackdrop) {
    /** Apply to the page content container so the bar can sample it. */
    fun contentRecorder(): Modifier = Modifier.layerBackdrop(backdrop)
}

@Composable
fun rememberUiKitGlassTopBar(): UiKitGlassTopBarScope {
    // No opaque base fill: empty regions stay transparent so unfrosted content shows
    // through wherever nothing has scrolled under the bar yet.
    val backdrop = rememberLayerBackdrop()
    return remember(backdrop) { UiKitGlassTopBarScope(backdrop) }
}

/** Records page content into the shared glass layer. Null scope = no recording. */
fun Modifier.uiKitRecordSurfaceGlassBackdrop(scope: UiKitGlassTopBarScope?): Modifier =
    if (scope == null) this else Modifier.layerBackdrop(scope.backdrop)

fun Modifier.uiKitSurfaceGlassSample(scope: UiKitGlassTopBarScope?): Modifier =
    if (scope == null) this else with(scope) {
        drawBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = {
                vibrancy()
                blur(8.dp.toPx(), 8.dp.toPx())
            },
        )
    }

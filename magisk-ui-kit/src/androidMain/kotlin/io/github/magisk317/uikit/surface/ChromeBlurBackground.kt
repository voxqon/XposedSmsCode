package io.github.magisk317.uikit.surface

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared chrome background for bars that overlay scrollable edge-to-edge content.
 */
@Composable
fun ChromeBlurBackground(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    blurRadius: Dp = 24.dp,
    alpha: Float = CHROME_DEFAULT_ALPHA,
) {
    val hasBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val effectiveAlpha = if (hasBlur) alpha else CHROME_FALLBACK_ALPHA
    val surfaceColor = chromeSurfaceColor(effectiveAlpha)

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (hasBlur) Modifier.blur(blurRadius) else Modifier)
            .background(surfaceColor),
    )
}

private const val CHROME_DEFAULT_ALPHA = CHROME_SURFACE_ALPHA
private const val CHROME_FALLBACK_ALPHA = CHROME_SURFACE_ALPHA

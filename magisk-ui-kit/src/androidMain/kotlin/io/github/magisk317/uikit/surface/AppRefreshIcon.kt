package io.github.magisk317.uikit.surface

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Refresh icon that spins for as long as [refreshing] is true and rests
 * otherwise. The infinite transition is only composed while spinning, so
 * the animation stops (and the icon stops recomposing) once the refresh
 * finishes.
 */
@Composable
fun AppRefreshIcon(
    refreshing: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageVector: ImageVector = Icons.Filled.Refresh,
) {
    if (refreshing) {
        val transition = rememberInfiniteTransition(label = "appRefreshSpin")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
            label = "appRefreshAngle",
        )
        AppIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier.rotate(angle),
        )
    } else {
        AppIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

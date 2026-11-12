package io.github.magisk317.uikit.theme

import android.graphics.Bitmap
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CancellationException
import kotlin.math.hypot

private const val THEME_REVEAL_DURATION_MILLIS = 600
private const val MAX_CAPTURE_PIXELS = 8_388_608L
private const val MAX_CAPTURE_SIDE_PX = 4096

/** State and lifecycle owner for a screenshot-based theme transition. */
@Stable
class ThemeRevealState {
    private val progress = Animatable(1f)

    var screenshotBitmap: Bitmap? by mutableStateOf(null)
        private set

    var animationCenter: Offset by mutableStateOf(Offset.Unspecified)
        private set

    /**
     * Captures the current (old-theme) view, invokes [onContentUpdate], then reveals the
     * updated content through a smooth circular hole in the old screenshot.
     *
     * Returns false when the view cannot be captured or the bitmap is too large. In that case
     * callers should still apply the new theme directly from [onContentUpdate].
     */
    suspend fun animateThemeChange(
        view: View,
        requestedCenter: Offset = Offset.Unspecified,
        onContentUpdate: () -> Unit,
    ): Boolean {
        val width = view.width
        val height = view.height
        val pixelCount = width.toLong() * height.toLong()
        if (
            width <= 0 ||
            height <= 0 ||
            width > MAX_CAPTURE_SIDE_PX ||
            height > MAX_CAPTURE_SIDE_PX ||
            pixelCount > MAX_CAPTURE_PIXELS
        ) {
            return false
        }

        clear()
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            screenshotBitmap = bitmap
            animationCenter = if (!requestedCenter.x.isNaN() && !requestedCenter.y.isNaN()) {
                requestedCenter
            } else {
                Offset(width / 2f, height / 2f)
            }

            onContentUpdate()
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = THEME_REVEAL_DURATION_MILLIS),
            )
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        } finally {
            clear()
        }
    }

    val progressValue: Float
        get() = progress.value

    fun clear() {
        screenshotBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        screenshotBitmap = null
    }
}

@Composable
fun rememberThemeRevealState(): ThemeRevealState = remember { ThemeRevealState() }

/** Draws the old screenshot above the new theme and clears a growing circular hole. */
@Composable
fun ThemeRevealOverlay(
    state: ThemeRevealState,
    modifier: Modifier = Modifier,
) {
    val bitmap = state.screenshotBitmap ?: return
    if (bitmap.isRecycled) return

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val radius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() *
                    state.progressValue
                drawCircle(
                    color = Color.Transparent,
                    radius = radius,
                    center = state.animationCenter,
                    blendMode = BlendMode.Clear,
                )
            },
    )
}

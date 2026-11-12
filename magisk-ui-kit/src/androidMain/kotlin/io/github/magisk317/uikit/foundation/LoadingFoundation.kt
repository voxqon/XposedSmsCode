package io.github.magisk317.uikit.foundation

import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object LoadingIndicatorTokens {
    const val MIN_VISIBLE_DURATION_MILLIS: Long = 500L

    val OverlayTopSpacing: Dp = 8.dp
    val ContainedSize: Dp = 56.dp
}

@Composable
fun AppLinearLoadingIndicator(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun rememberMinDurationLoading(
    actualLoading: Boolean,
    minDurationMillis: Long = LoadingIndicatorTokens.MIN_VISIBLE_DURATION_MILLIS,
): Boolean {
    var visibleLoading by remember { mutableStateOf(actualLoading) }
    var loadingStartAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(actualLoading) {
        if (actualLoading) {
            if (loadingStartAt == 0L) {
                loadingStartAt = SystemClock.elapsedRealtime()
            }
            visibleLoading = true
        } else {
            if (loadingStartAt == 0L) {
                visibleLoading = false
                return@LaunchedEffect
            }
            val elapsed = SystemClock.elapsedRealtime() - loadingStartAt
            val remaining = (minDurationMillis - elapsed).coerceAtLeast(0L)
            if (remaining > 0) {
                delay(remaining)
            }
            visibleLoading = false
            loadingStartAt = 0L
        }
    }

    return visibleLoading
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PolygonMorphLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = LoadingIndicatorTokens.ContainedSize,
) {
    ContainedLoadingIndicator(
        modifier = modifier.size(size),
    )
}

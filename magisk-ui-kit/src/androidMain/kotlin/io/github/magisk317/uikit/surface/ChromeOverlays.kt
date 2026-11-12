package io.github.magisk317.uikit.surface

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.SystemBarsScrim
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.blur.Backdrop

const val CHROME_VISIBILITY_ANIMATION_MILLIS = 160

/**
 * A compact navigation chrome anchored to the bottom of its [BoxScope]. The component owns that
 * anchor; [modifier] customizes its appearance and size but cannot reposition it.
 */
@Composable
fun BoxScope.AnimatedCompactBottomNavigationChrome(
    visible: Boolean,
    items: List<AppNavigationItemSpec>,
    modifier: Modifier = Modifier,
    alwaysShowLabel: Boolean = false,
    animationMillis: Int = CHROME_VISIBILITY_ANIMATION_MILLIS,
    chromeShape: Shape = RectangleShape,
    chromeTopPadding: Dp = 0.dp,
    floating: Boolean = false,
    blurred: Boolean = false,
    backdrop: Backdrop? = null,
    onHeightChanged: (Dp) -> Unit = {},
    onDragProgress: ((fractionalIndex: Float) -> Unit)? = null,
) {
    val density = LocalDensity.current
    // 高度上报去抖：chrome 进出场是 animationMillis tween（slide + fade），动画期间
    // onSizeChanged 每帧回调的都是中间态高度，直接透传给 onHeightChanged 会让
    // 消费方（MainTabScaffold 的 compactBottomBarHeight -> contentBottomPadding）
    // 跟着切 tab 抖。做法：采样值只写进 remember 状态，由 LaunchedEffect 以采样值
    // 为 key 放行——每个新中间采样都会取消上一轮 delay；只有持续整个动画时长
    // 不再变化的采样才真正发出。稳态值必触发且只触发一次完整协程，
    // 因此稳态必然通过（最多迟一个窗口），中间帧永远发不出去。
    val heightDebounceMillis = animationMillis.toLong()
    val sampledHeight = remember { mutableStateOf<Dp?>(null) }
    LaunchedEffect(sampledHeight.value) {
        val height = sampledHeight.value ?: return@LaunchedEffect
        delay(heightDebounceMillis)
        onHeightChanged(height)
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(animationMillis),
            initialOffsetY = { it },
        ) + fadeIn(animationSpec = tween(animationMillis)),
        exit = slideOutVertically(
            animationSpec = tween(animationMillis),
            targetOffsetY = { it },
        ) + fadeOut(animationSpec = tween(animationMillis)),
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .onSizeChanged { size ->
                with(density) {
                    sampledHeight.value = size.height.toDp()
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = chromeTopPadding)
                .clip(chromeShape),
        ) {
            if (floating) {
                AppFloatingNavigationBar(
                    items = items,
                    alwaysShowLabel = alwaysShowLabel,
                    blurred = blurred,
                    backdrop = if (blurred) backdrop else null,
                    onDragProgress = onDragProgress,
                )
            } else {
                ChromeBlurBackground(
                    modifier = Modifier.matchParentSize(),
                    shape = chromeShape,
                )
                AppBottomNavigationBar(
                    items = items,
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    alwaysShowLabel = alwaysShowLabel,
                )
            }
        }
    }
}

@Composable
fun AnimatedSystemBarsScrim(
    visible: Boolean,
    modifier: Modifier = Modifier,
    animationMillis: Int = CHROME_VISIBILITY_ANIMATION_MILLIS,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(animationMillis),
        label = "systemBarsChromeAlpha",
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(animationMillis)),
        exit = fadeOut(animationSpec = tween(animationMillis)),
        modifier = modifier,
    ) {
        // Only scrim the status-bar edge. The bottom band is intentionally not
        // drawn: HyperOS (and API 35+ enforced edge-to-edge) needs app content
        // behind the gesture pill so it can fade out, and every bottom surface
        // (floating bar / bottom bar) already paints its own background.
        SystemBarsScrim(
            statusBarAlpha = alpha,
            navBarAlpha = 0f,
        )
    }
}

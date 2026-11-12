package io.github.magisk317.uikit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import kotlin.math.roundToInt

/** Corner radius shared with the Miuix list-row card (`CardDefaults.CornerRadius`). */
private val SwipeRevealCornerRadius: Dp = 16.dp

/**
 * Hairline inset keeping the reveal silhouette strictly inside the dragged card's
 * anti-aliased edge, so no reveal colour peeks out at the four corners while the
 * row is settled.
 */
private val SwipeRevealInset: Dp = 1.dp

/** Toolkit-neutral values for a horizontal dismiss gesture. */
enum class AppSwipeToDismissValue {
    Settled,
    StartToEnd,
    EndToStart,
}

@Stable
class AppSwipeToDismissState internal constructor(
    initialValue: AppSwipeToDismissValue,
) {
    var currentValue by mutableStateOf(initialValue)
        private set

    var dismissDirection by mutableStateOf<AppSwipeToDismissValue?>(
        initialValue.takeUnless { it == AppSwipeToDismissValue.Settled },
    )
        private set

    internal var offsetPx by mutableStateOf(0f)
        private set
    private var confirm: (AppSwipeToDismissValue) -> Boolean = { true }
    private var materialSnapTo: (suspend (AppSwipeToDismissValue) -> Unit)? = null

    internal fun setConfirmValueChange(value: (AppSwipeToDismissValue) -> Boolean) {
        confirm = value
    }

    internal fun confirmValueChange(value: AppSwipeToDismissValue): Boolean =
        confirm(value)

    internal fun updateCurrentValue(value: AppSwipeToDismissValue) {
        currentValue = value
        dismissDirection = value.takeUnless { it == AppSwipeToDismissValue.Settled }
    }

    internal fun updateDismissDirection(value: AppSwipeToDismissValue?) {
        dismissDirection = value
    }

    internal fun attachMaterialSnapTo(value: suspend (AppSwipeToDismissValue) -> Unit) {
        materialSnapTo = value
    }

    internal fun detachMaterialSnapTo() {
        materialSnapTo = null
    }

    /** Returns the state to a settled row without exposing a toolkit state type. */
    suspend fun snapTo(value: AppSwipeToDismissValue) {
        materialSnapTo?.invoke(value) ?: run {
            if (value == AppSwipeToDismissValue.Settled) {
                offsetPx = 0f
            }
            updateCurrentValue(value)
        }
    }

    internal fun dragBy(delta: Float, width: Float) {
        offsetPx = (offsetPx + delta).coerceIn(-width, width)
    }

    internal suspend fun settle(width: Float) {
        val direction = when {
            offsetPx > width * 0.5f -> AppSwipeToDismissValue.StartToEnd
            offsetPx < -width * 0.5f -> AppSwipeToDismissValue.EndToStart
            else -> AppSwipeToDismissValue.Settled
        }
        val accepted = direction != AppSwipeToDismissValue.Settled && confirm(direction)
        offsetPx = if (accepted) {
            updateCurrentValue(direction)
            if (direction == AppSwipeToDismissValue.StartToEnd) width else -width
        } else {
            0f
        }
        if (!accepted) {
            updateCurrentValue(AppSwipeToDismissValue.Settled)
        }
    }
}

@Composable
fun rememberAppSwipeToDismissState(
    initialValue: AppSwipeToDismissValue = AppSwipeToDismissValue.Settled,
    confirmValueChange: (AppSwipeToDismissValue) -> Boolean = { true },
): AppSwipeToDismissState {
    val latestConfirmValueChange by rememberUpdatedState(confirmValueChange)
    val state = remember(initialValue) { AppSwipeToDismissState(initialValue) }
    SideEffect {
        state.setConfirmValueChange(latestConfirmValueChange)
    }
    return state
}

/**
 * Style-aware background surface for the reveal area behind a swipe-to-dismiss row.
 *
 * The reveal is the row's own silhouette: a full-row surface carrying the row card's
 * rounded shape (Miuix rows are rounded cards; Expressive rows are plain rectangles).
 * Filling the whole row instead of tracking the drag offset makes the reveal read as
 * the same rounded rectangle slowly uncovered behind the sliding card — the card's
 * rounded corners always sit on continuous reveal colour. An offset-width strip can
 * only round its outer corners: rounding the edge touching the card cuts notches of
 * page background against the card's straight leading edge (looks like the reveal is
 * prying the card open), while keeping that edge square leaves the card's corners
 * showing page background that does not follow the card's curve.
 *
 * The hairline inset keeps the silhouette strictly inside the dragged card's
 * anti-aliased edge, so no reveal colour peeks out at the four corners while the
 * row is settled.
 *
 * Typical use inside [AppSwipeToDismissBox]'s (or the platform `SwipeToDismissBox`'s)
 * `backgroundContent`, replacing a bare `Modifier.background(...)`:
 *
 * ```
 * Box(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .then(swipeRevealSurface(color = MaterialTheme.colorScheme.errorContainer))
 *         .padding(horizontal = 24.dp),
 *     contentAlignment = revealAlignment,
 * ) { /* delete affordance */ }
 * ```
 *
 * @param color reveal fill (typically `MaterialTheme.colorScheme.errorContainer`).
 * @param cornerRadius reveal corner radius; defaults to the Miuix list-row card radius.
 *   Ignored in the Expressive style, whose rows are rectangular.
 */
@Composable
fun swipeRevealSurface(
    color: Color,
    cornerRadius: Dp = SwipeRevealCornerRadius,
): Modifier = if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
    Modifier
        .fillMaxSize()
        .padding(SwipeRevealInset)
        .background(color, RoundedCornerShape(cornerRadius))
} else {
    Modifier
        .fillMaxSize()
        .background(color)
}

/**
 * Cross-style horizontal dismiss container. Material keeps the platform component;
 * Miuix uses a lightweight foundation gesture because Miuix 0.9.3 has no native
 * SwipeToDismissBox equivalent.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSwipeToDismissBox(
    state: AppSwipeToDismissState,
    modifier: Modifier = Modifier,
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    backgroundContent: @Composable (AppSwipeToDismissValue?) -> Unit,
    content: @Composable () -> Unit,
) {
    if (io.github.magisk317.uikit.theme.LocalUiKitStyle.current == io.github.magisk317.uikit.theme.UiKitStyle.Expressive) {
        val materialState = rememberSwipeToDismissBoxState(
            initialValue = state.currentValue.toMaterialValue(),
            confirmValueChange = { value ->
                val appValue = value.toAppValue()
                val accepted = state.confirmValueChange(appValue)
                if (accepted) {
                    state.updateCurrentValue(appValue)
                }
                accepted
            },
        )
        DisposableEffect(materialState) {
            state.attachMaterialSnapTo { value -> materialState.snapTo(value.toMaterialValue()) }
            onDispose { state.detachMaterialSnapTo() }
        }
        SideEffect {
            state.updateDismissDirection(materialState.dismissDirection.toAppValue())
        }
        androidx.compose.runtime.LaunchedEffect(materialState.currentValue) {
            state.updateCurrentValue(materialState.currentValue.toAppValue())
        }
        SwipeToDismissBox(
            modifier = modifier,
            state = materialState,
            enableDismissFromStartToEnd = enableDismissFromStartToEnd,
            enableDismissFromEndToStart = enableDismissFromEndToStart,
            backgroundContent = {
                backgroundContent(materialState.dismissDirection.toAppValue())
            },
            content = { content() },
        )
        return
    }

    BoxWithConstraints(modifier = modifier) {
        var widthPx by remember { mutableIntStateOf(0) }
        var settleRequest by remember { mutableIntStateOf(0) }
        var cancelRequest by remember { mutableIntStateOf(0) }
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current

        LaunchedEffect(settleRequest, cancelRequest) {
            when {
                cancelRequest > 0 -> state.snapTo(AppSwipeToDismissValue.Settled)
                settleRequest > 0 -> state.settle(widthPx.toFloat())
            }
        }

        val gestureModifier = Modifier
            .fillMaxSize()
            .onSizeChanged { widthPx = it.width }
            .pointerInput(state, enableDismissFromStartToEnd, enableDismissFromEndToStart, layoutDirection) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        val direction = physicalDirection(dragAmount, layoutDirection)
                        val enabled = when (direction) {
                            AppSwipeToDismissValue.StartToEnd -> enableDismissFromStartToEnd
                            AppSwipeToDismissValue.EndToStart -> enableDismissFromEndToStart
                            AppSwipeToDismissValue.Settled -> false
                        }
                        if (enabled) {
                            state.updateDismissDirection(direction)
                            state.dragBy(dragAmount, widthPx.toFloat())
                        }
                    },
                    onDragEnd = { settleRequest++ },
                    onDragCancel = { cancelRequest++ },
                )
            }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.matchParentSize()) {
                backgroundContent(state.dismissDirection)
            }
            Box(
                modifier = gestureModifier
                    .offset { IntOffset(state.offsetPx.roundToInt(), 0) }
                    .zIndex(1f),
            ) {
                content()
            }
        }
    }
}

private fun physicalDirection(
    dragAmount: Float,
    layoutDirection: LayoutDirection,
): AppSwipeToDismissValue = if (dragAmount >= 0f) {
    if (layoutDirection == LayoutDirection.Ltr) {
        AppSwipeToDismissValue.StartToEnd
    } else {
        AppSwipeToDismissValue.EndToStart
    }
} else if (layoutDirection == LayoutDirection.Ltr) {
    AppSwipeToDismissValue.EndToStart
} else {
    AppSwipeToDismissValue.StartToEnd
}

@OptIn(ExperimentalMaterial3Api::class)
private fun AppSwipeToDismissValue.toMaterialValue(): SwipeToDismissBoxValue = when (this) {
    AppSwipeToDismissValue.Settled -> SwipeToDismissBoxValue.Settled
    AppSwipeToDismissValue.StartToEnd -> SwipeToDismissBoxValue.StartToEnd
    AppSwipeToDismissValue.EndToStart -> SwipeToDismissBoxValue.EndToStart
}

private fun SwipeToDismissBoxValue.toAppValue(): AppSwipeToDismissValue = when (this) {
    SwipeToDismissBoxValue.Settled -> AppSwipeToDismissValue.Settled
    SwipeToDismissBoxValue.StartToEnd -> AppSwipeToDismissValue.StartToEnd
    SwipeToDismissBoxValue.EndToStart -> AppSwipeToDismissValue.EndToStart
}

package io.github.magisk317.uikit.surface

import androidx.activity.BackEventCompat
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/** Default duration for main-tab / rank-based horizontal nav transitions. */
const val TAB_NAV_TRANSITION_MS = 300

private const val PREDICTIVE_BACK_SCALE = 0.9f
private const val PREDICTIVE_BACK_SHIFT_DIVISOR = 20
private val predictiveBackEasing = CubicBezierEasing(0f, 0f, 0f, 1f)

/**
 * Direction of a tab-index (or route-rank) transition.
 *
 * Positive means the target is to the right of the initial index (forward along the bar).
 * When indices are equal, [isPop] forces a pop-style direction of `-1`.
 */
fun tabTransitionDirection(
    initialIndex: Int,
    targetIndex: Int,
    isPop: Boolean = false,
): Int {
    return when {
        targetIndex > initialIndex -> 1
        targetIndex < initialIndex -> -1
        isPop -> -1
        else -> 1
    }
}

/**
 * Forward direction used by host-level enter/exit pairs when equal ranks should still
 * animate as "forward" (`+1`), matching historical MiPush behavior.
 */
fun tabForwardDirection(
    initialIndex: Int,
    targetIndex: Int,
): Int {
    return if (targetIndex >= initialIndex) 1 else -1
}

fun tabEnterTransition(
    direction: Int,
    durationMillis: Int = TAB_NAV_TRANSITION_MS,
): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(durationMillis, easing = EaseInOut),
        initialOffsetX = { fullWidth -> direction * fullWidth },
    ) + fadeIn(animationSpec = tween(durationMillis))
}

fun tabExitTransition(
    direction: Int,
    durationMillis: Int = TAB_NAV_TRANSITION_MS,
): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(durationMillis, easing = EaseInOut),
        targetOffsetX = { fullWidth -> -direction * fullWidth },
    ) + fadeOut(animationSpec = tween(durationMillis))
}

fun tabPopEnterTransition(
    direction: Int,
    durationMillis: Int = TAB_NAV_TRANSITION_MS,
): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(durationMillis, easing = EaseInOut),
        initialOffsetX = { fullWidth -> -direction * fullWidth },
    ) + fadeIn(animationSpec = tween(durationMillis))
}

fun tabPopExitTransition(
    direction: Int,
    durationMillis: Int = TAB_NAV_TRANSITION_MS,
): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(durationMillis, easing = EaseInOut),
        targetOffsetX = { fullWidth -> direction * fullWidth },
    ) + fadeOut(animationSpec = tween(durationMillis))
}

/** Reveals the previous destination without competing with the system's edge animation. */
fun tabPredictivePopEnterTransition(
    durationMillis: Int = TAB_NAV_TRANSITION_MS,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis, easing = predictiveBackEasing),
    )
}

/** Applies Android's restrained predictive-back transform to the current destination. */
fun tabPredictivePopExitTransition(
    swipeEdge: Int,
    durationMillis: Int = TAB_NAV_TRANSITION_MS,
): ExitTransition {
    val direction = if (swipeEdge == BackEventCompat.EDGE_RIGHT) -1 else 1

    return slideOutHorizontally(
        animationSpec = tween(durationMillis, easing = predictiveBackEasing),
        targetOffsetX = { fullWidth ->
            direction * (fullWidth / PREDICTIVE_BACK_SHIFT_DIVISOR)
        },
    ) + scaleOut(
        animationSpec = tween(durationMillis, easing = predictiveBackEasing),
        targetScale = PREDICTIVE_BACK_SCALE,
    )
}

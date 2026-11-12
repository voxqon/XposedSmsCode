package io.github.magisk317.uikit.theme

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.magisk317.uikit.surface.chromeNavigationBarColor
import io.github.magisk317.uikit.surface.chromeStatusBarColor

fun applyEdgeToEdge(activity: Activity) {
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    applySystemBarStyle(activity.window)
}

@Composable
actual fun UpdateSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) {
        return
    }
    val window = (view.context as? Activity)?.window ?: return
    SideEffect {
        applySystemBarStyle(window)
        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}

@Composable
fun SystemBarsScrim(
    statusBarAlpha: Float = 1f,
    navBarAlpha: Float = 1f,
) {
    val statusBarColor = chromeStatusBarColor()
    val navBarColor = chromeNavigationBarColor()
    Box(modifier = Modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = statusBarAlpha }
                .background(statusBarColor),
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .align(Alignment.BottomStart)
                .graphicsLayer { alpha = navBarAlpha }
                .background(navBarColor),
        )
    }
}

private const val API_VANILLA_ICE_CREAM = 35

private fun applySystemBarStyle(window: Window) {
    // On API 35+ edge-to-edge is enforced; statusBarColor/navigationBarColor
    // are deprecated and ignored by the platform.  Skip them to avoid the
    // Play Console deprecation warning.
    if (Build.VERSION.SDK_INT < API_VANILLA_ICE_CREAM) {
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }
    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q until 35) {
        @Suppress("DEPRECATION")
        run {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }
    // Deliberately NOT setting layoutInDisplayCutoutMode.  AndroidX
    // enableEdgeToEdge sets SHORT_EDGES (28-29) / ALWAYS (30+) there, which
    // trips the Play Console deprecation advisory.  Accepted trade-off: on
    // API 28-34 cutout devices the notch strip shows the window background
    // instead of the themed scrim.
}


package io.github.magisk317.uikit.foundation

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.magisk317.uikit.common.AppSnackbarHostState

val LocalSnackbarHostState = staticCompositionLocalOf<AppSnackbarHostState> {
    error("LocalSnackbarHostState not provided")
}

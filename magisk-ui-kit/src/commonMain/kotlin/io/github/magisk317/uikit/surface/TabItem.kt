package io.github.magisk317.uikit.surface

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A single bottom-navigation / rail tab: its label, icon, and the typed route it
 * navigates to. [T] is the app-specific route type (a sealed route object, a
 * serializable route, etc.).
 */
@Immutable
data class TabItem<T : Any>(
    val label: String,
    val icon: ImageVector,
    val route: T,
)

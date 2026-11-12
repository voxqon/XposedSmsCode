package io.github.magisk317.uikit.preference

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A lightweight section title used above a group of preference items or list
 * rows. Style is intentionally minimal; callers supply their own surrounding
 * padding via [modifier] when needed.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

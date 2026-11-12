package io.github.magisk317.uikit.surface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Dual-track plain list row: M3 [ListItem] / miuix `BasicComponent`.
 *
 * Unlike [io.github.magisk317.uikit.preference.AppArrowItem] this row carries no
 * navigation arrow; it is a display row, optionally clickable. Title/summary are
 * strings because miuix `BasicComponent` only accepts strings — both tracks then
 * render the same content contract.
 */
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        BasicComponent(
            modifier = modifier.fillMaxWidth(),
            title = title,
            titleColor = BasicComponentDefaults.titleColor(
                color = MiuixTheme.colorScheme.onSurface,
                disabledColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
            summary = summary?.takeIf { it.isNotEmpty() },
            summaryColor = BasicComponentDefaults.summaryColor(
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                disabledColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.38f),
            ),
            startAction = leadingContent,
            endActions = {
                if (trailingContent != null) {
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .align(Alignment.CenterVertically),
                    ) {
                        trailingContent()
                    }
                }
            },
            enabled = enabled,
            onClick = onClick,
        )
        return
    }

    ListItem(
        modifier = if (onClick != null) {
            modifier.clickable(enabled = enabled, onClick = onClick)
        } else {
            modifier
        },
        supportingContent = summary?.takeIf { it.isNotEmpty() }?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

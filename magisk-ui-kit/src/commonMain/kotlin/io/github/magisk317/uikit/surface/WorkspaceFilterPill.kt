package io.github.magisk317.uikit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.shape.RoundedCornerShape

private const val FULL_ROUND_PERCENT = 50

/**
 * A compact selectable pill shared by filter surfaces across Material and Miuix.
 * The caller owns the selection model; this component only renders and reports a choice.
 */
@Composable
fun WorkspaceFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isMiuix = LocalUiKitStyle.current == UiKitStyle.Miuix
    val containerColor = if (isMiuix) {
        if (selected) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surfaceContainer
    } else {
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isMiuix) {
        if (selected) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurfaceVariantSummary
    } else {
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textStyle: TextStyle = if (isMiuix) {
        MiuixTheme.textStyles.body2
    } else {
        MaterialTheme.typography.labelLarge
    }
    val disabledAlpha = 0.38f

    Row(
        modifier = modifier
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.Tab,
            )
            .background(
                color = containerColor.copy(alpha = if (enabled) 1f else disabledAlpha),
                shape = RoundedCornerShape(FULL_ROUND_PERCENT),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        MiuixText(
            text = label,
            style = textStyle,
            color = contentColor.copy(alpha = if (enabled) 1f else disabledAlpha),
        )
    }
}

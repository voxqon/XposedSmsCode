package io.github.magisk317.uikit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.text.AppText
import io.github.magisk317.uikit.text.AppTextRole
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val APP_CHIP_FULL_ROUND_PERCENT = 50

/**
 * Dual-track selectable filter chip: M3 [FilterChip] / miuix pill.
 *
 * miuix 0.9.4 ships no chip, so the Miuix track renders a composed pill over
 * miuix colors (same recipe as [WorkspaceFilterPill]). The label fills the chip
 * width and centres, matching the previous `CenteredChipText` call sites.
 */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        AppChipPill(
            label = label,
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            role = Role.Checkbox,
        )
        return
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { AppChipLabel(label) },
        modifier = modifier,
        enabled = enabled,
    )
}

/**
 * Dual-track action chip (no selection state): M3 [AssistChip] / miuix pill.
 */
@Composable
fun AppAssistChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        AppChipPill(
            label = label,
            selected = false,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            role = Role.Button,
        )
        return
    }

    AssistChip(
        onClick = onClick,
        label = { AppChipLabel(label) },
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
private fun AppChipLabel(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = label,
            role = AppTextRole.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    role: Role,
) {
    val containerColor = if (selected) {
        MiuixTheme.colorScheme.primaryContainer
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.onPrimaryContainer
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val disabledAlpha = 0.38f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = role,
            )
            .background(
                color = containerColor.copy(alpha = if (enabled) 1f else disabledAlpha),
                shape = RoundedCornerShape(APP_CHIP_FULL_ROUND_PERCENT),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixText(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = contentColor.copy(alpha = if (enabled) 1f else disabledAlpha),
        )
    }
}

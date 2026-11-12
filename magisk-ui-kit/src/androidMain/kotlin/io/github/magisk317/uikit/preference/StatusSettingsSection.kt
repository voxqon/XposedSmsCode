package io.github.magisk317.uikit.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.surface.UiKitRowInsideCard
import io.github.magisk317.uikit.theme.spacing
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.Card as MiuixCard

/**
 * Standalone group card hosting a single status/action row — e.g. the
 * activation-status entry at the top of a settings page. Unlike
 * [SectionCard] there is no accordion header: the row itself is the whole
 * group, rendered inside the same group container (MiuixCard / Material
 * Card) so it lines up visually with the other settings groups. Shared by
 * all UI Kit hosts.
 */
@Composable
fun StatusSettingsSection(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.small),
            insideMargin = PaddingValues(0.dp),
        ) {
            UiKitRowInsideCard {
                AppArrowItem(
                    title = title,
                    summary = summary,
                    trailingContent = trailingContent,
                    onClick = onClick,
                )
            }
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.small),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        AppArrowItem(
            title = title,
            summary = summary,
            trailingContent = trailingContent,
            onClick = onClick,
        )
    }
}

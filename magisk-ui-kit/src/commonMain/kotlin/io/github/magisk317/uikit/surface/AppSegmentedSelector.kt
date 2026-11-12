package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.text.AppText
import io.github.magisk317.uikit.text.AppTextRole
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle

/** A single segment: the value it selects plus its display label. */
@Immutable
data class AppSegmentedOption<T>(val value: T, val label: String)

/**
 * Dual-track single-choice segmented selector.
 *
 * - M3 track: [SingleChoiceSegmentedButtonRow] + [SegmentedButton].
 * - Miuix track: a row of [AppFilterChip] pills. miuix 0.9.4 ships no segmented button, and the
 *   previously used `TabRow` is a top-level tab bar — full width with a sliding indicator — which
 *   read as a misplaced tab strip wherever this selector sits inside a card. The pills reuse the
 *   kit's own miuix chip recipe, so they match the filter pills used elsewhere in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppSingleChoiceSegmentedSelector(
    options: List<AppSegmentedOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return

    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                AppFilterChip(
                    label = option.label,
                    selected = option.value == selected,
                    onClick = { onSelect(option.value) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        return
    }

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option.value == selected
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelect(option.value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = if (isSelected) {
                    Modifier.widthIn(min = 0.dp)
                } else {
                    Modifier.weight(1f)
                }.heightIn(min = 40.dp),
                icon = {},
                label = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(
                            text = option.label,
                            role = AppTextRole.Body,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            )
        }
    }
}

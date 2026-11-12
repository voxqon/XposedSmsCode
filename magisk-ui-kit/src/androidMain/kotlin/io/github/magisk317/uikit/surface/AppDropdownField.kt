package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.magisk317.uikit.preference.AppArrowItem
import io.github.magisk317.uikit.text.AppText
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.overlay.OverlayListPopup

/**
 * A "pick one value out of a list" form field that renders the picker matching the
 * active UI kit style. This is the shared form of every dropdown that used to be an
 * M3 `ExposedDropdownMenuBox` anchored on a read-only text field: sender targets,
 * channel pickers, query presets and friends all render the same field, so one
 * component covers all of them.
 *
 * - [UiKitStyle.Miuix]: a settings row ([AppArrowItem]) whose tap opens the stock miuix
 *   [OverlayListPopup] rendered with [ListPopupColumn] + [DropdownImpl], so the
 *   selected entry carries the HyperOS / KernelSU check mark. miuix has no
 *   outlined-field dropdown idiom; its idiom is exactly this row + popup pair,
 *   the same recipe [io.github.magisk317.uikit.preference.SettingsChoiceRow]
 *   uses for settings pages.
 * - [UiKitStyle.Expressive]: the classic M3 [ExposedDropdownMenuBox] over the kit
 *   [AppTextField], keeping the outlined-field look and the inline popup.
 *
 * The field owns its own expand/dismiss state instead of taking `expanded`,
 * `onExpandedChange` and `onDismissRequest` parameters. Hosts previously had to
 * keep a per-field `expanded` boolean and route it through both the box and the
 * text field; keeping it here removes the duplication.
 *
 * @param title Field label - row title on the Miuix track, text-field label on the
 *   Expressive track.
 * @param value Display text of the current selection; when empty,
 *   [placeholderText] is shown instead.
 * @param options Human readable options in display order.
 * @param selectedIndex Index of the current selection, or a negative value when
 *   nothing matches.
 * @param onSelect Invoked with the picked index. The picker closes immediately
 *   afterwards.
 * @param modifier Modifier applied to the field container.
 * @param placeholderText Hint shown while [value] is empty.
 * @param enabled When false the field is not clickable and no picker can be opened.
 */
@Composable
fun AppDropdownField(
    title: String,
    value: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    enabled: Boolean = true,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        AppDropdownFieldMiuix(
            title = title,
            value = value,
            options = options,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier,
            placeholderText = placeholderText,
            enabled = enabled,
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        AppTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = title,
            placeholderText = placeholderText,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { AppText(option) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                    trailingIcon = if (index == selectedIndex) {
                        { Icon(imageVector = Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun AppDropdownFieldMiuix(
    title: String,
    value: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    placeholderText: String?,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val dismiss: () -> Unit = { expanded = false }

    Box(modifier = modifier.fillMaxWidth()) {
        AppArrowItem(
            title = title,
            summary = value.ifEmpty { placeholderText.orEmpty() },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
        )

        OverlayListPopup(
            show = expanded,
            onDismissRequest = dismiss,
            content = {
                ListPopupColumn {
                    options.forEachIndexed { index, option ->
                        DropdownImpl(
                            text = option,
                            optionSize = options.size,
                            isSelected = index == selectedIndex,
                            index = index,
                            onSelectedIndexChange = { picked ->
                                onSelect(picked)
                                dismiss()
                            },
                        )
                    }
                }
            },
        )
    }
}

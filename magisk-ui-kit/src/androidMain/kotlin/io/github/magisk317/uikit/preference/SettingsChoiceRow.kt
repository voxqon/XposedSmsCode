package io.github.magisk317.uikit.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.magisk317.uikit.surface.AppIcon
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.currentUiKitStyle
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.overlay.OverlayListPopup

/**
 * A single-choice settings row that renders the picker matching the active UI kit style.
 *
 * This is the shared form of every "pick one value out of a list" setting: theme, UI kit
 * style, layout scale, palette style, colour spec, accent colour and friends all render the
 * same row, so one component covers all of them.
 *
 * - [UiKitStyle.Miuix]: tapping the row opens the stock miuix [OverlayListPopup] anchored
 *   under the row, rendered with [ListPopupColumn] + [DropdownImpl] so the selected entry
 *   carries the HyperOS / KernelSU check mark.
 * - [UiKitStyle.Expressive]: tapping the row opens the classic [SingleChoiceOptionDialog]
 *   radio list, or [SingleChoicePositionDialog] when [onSelectWithPosition] is supplied.
 *   Both dialogs ship without buttons and therefore do not close themselves, so this row
 *   dismisses them from inside the selection callback.
 *
 * The row owns its own expand/dismiss state instead of taking `expanded`, `onOpen` and
 * `onDismiss` parameters. Hosts previously had to keep a page level `activeChoice` enum and
 * route one shared dialog for every row, and that state machine was copied verbatim into
 * each consumer; keeping it here removes the duplication.
 *
 * @param title Row title, reused as the Expressive dialog title.
 * @param summary Secondary text rendered under [title].
 * @param options Human readable options in display order.
 * @param selectedIndex Index of the current selection, or -1 when nothing matches.
 * @param onSelect Invoked with the picked index when [onSelectWithPosition] is null. The
 *   picker closes immediately afterwards.
 * @param modifier Modifier applied to the row container that anchors the miuix popup.
 * @param enabled When false the row is not clickable and no picker can be opened.
 * @param onSelectWithPosition Optional reveal-animation hook that additionally reports the
 *   tapped screen position (x and y in pixels). Supplying it switches the Expressive picker
 *   to [SingleChoicePositionDialog]. The two callbacks are mutually exclusive: when this is
 *   non-null only this one fires, and when it is null only [onSelect] fires — never both.
 *
 *   Known limitation: the miuix dropdown carries no coordinate semantics, so under
 *   [UiKitStyle.Miuix] the position callback is never invoked and the row falls back to the
 *   plain [onSelect] path. Callers driving a reveal animation from these coordinates must
 *   treat Miuix as "no coordinates available" and skip the animation; no coordinates are
 *   synthesised on their behalf.
 */
@Composable
fun SettingsChoiceRow(
    title: String,
    summary: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelectWithPosition: ((Int, Float, Float) -> Unit)? = null,
) {
    val style = currentUiKitStyle()
    var expanded by remember { mutableStateOf(false) }
    val dismiss: () -> Unit = { expanded = false }

    Box(modifier = modifier.fillMaxWidth()) {
        val trailingContent: (@Composable () -> Unit)? = if (style == UiKitStyle.Expressive) {
            { AppIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        } else {
            null
        }
        AppArrowItem(
            title = title,
            summary = summary,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            trailingContent = trailingContent,
            onClick = { expanded = true },
        )

        when (style) {
            UiKitStyle.Miuix -> OverlayListPopup(
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

            UiKitStyle.Expressive -> {
                if (expanded) {
                    if (onSelectWithPosition != null) {
                        SingleChoicePositionDialog(
                            title = title,
                            options = options,
                            selectedIndex = selectedIndex,
                            onSelectionChange = { index, screenPosition ->
                                onSelectWithPosition.invoke(
                                    index,
                                    screenPosition.x,
                                    screenPosition.y,
                                )
                                dismiss()
                            },
                            onDismissRequest = dismiss,
                        )
                    } else {
                        SingleChoiceOptionDialog(
                            title = title,
                            options = options,
                            selectedIndex = selectedIndex,
                            onSelectionChange = { index ->
                                onSelect(index)
                                dismiss()
                            },
                            onDismissRequest = dismiss,
                        )
                    }
                }
            }
        }
    }
}

package io.github.magisk317.uikit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.text.AppText
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One action row of an [AppListPopup]. */
@Immutable
data class AppListPopupItem(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Dual-track action popup menu.
 *
 * - M3 track: [DropdownMenu] + [DropdownMenuItem].
 * - Miuix track: [OverlayListPopup] + [ListPopupColumn] with rows composed over
 *   `DropdownDefaults.dropdownColors()` (miuix ships no standalone menu-item
 *   composable; `DropdownItem` is a data model for the `Dropdown` picker).
 *
 * Call this as a sibling of its trigger inside a shared parent (e.g. a `Box`):
 * both tracks anchor the popup to the surrounding layout node.
 */
@Composable
fun AppListPopup(
    show: Boolean,
    onDismissRequest: () -> Unit,
    items: List<AppListPopupItem>,
    modifier: Modifier = Modifier,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        OverlayListPopup(
            show = show,
            popupPositionProvider = AppListPopupMenuPositionProvider,
            onDismissRequest = onDismissRequest,
            content = {
                ListPopupColumn {
                    items.forEachIndexed { index, item ->
                        AppListPopupMiuixRow(
                            item = item,
                            first = index == 0,
                            last = index == items.lastIndex,
                            onDismissRequest = onDismissRequest,
                        )
                    }
                }
            },
        )
        return
    }

    DropdownMenu(
        expanded = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                text = { AppText(item.label) },
                enabled = item.enabled,
                onClick = {
                    onDismissRequest()
                    item.onClick()
                },
            )
        }
    }
}

@Composable
private fun AppListPopupMiuixRow(
    item: AppListPopupItem,
    first: Boolean,
    last: Boolean,
    onDismissRequest: () -> Unit,
) {
    val colors = DropdownDefaults.dropdownColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled) {
                onDismissRequest()
                item.onClick()
            }
            .background(colors.containerColor)
            .padding(horizontal = 20.dp)
            .padding(
                top = if (first) 20.dp else 12.dp,
                bottom = if (last) 20.dp else 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixText(
            text = item.label,
            fontSize = MiuixTheme.textStyles.body1.fontSize,
            fontWeight = FontWeight.Medium,
            color = colors.contentColor,
        )
    }
}

/**
 * Anchors the popup to the bottom-start of the trigger, clamped to the window.
 * miuix's own `ListPopupDefaults` providers are not reachable from Kotlin here,
 * so the kit ships its own — same interface, same behaviour.
 */
private object AppListPopupMenuPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align,
    ): IntOffset {
        val resolved = alignment.resolve(layoutDirection)
        val offsetX: Int
        val offsetY: Int
        when (resolved) {
            PopupPositionProvider.Align.TopStart -> {
                offsetX = anchorBounds.left + popupMargin.left
                offsetY = anchorBounds.bottom + popupMargin.top
            }

            PopupPositionProvider.Align.TopEnd -> {
                offsetX = anchorBounds.right - popupContentSize.width - popupMargin.right
                offsetY = anchorBounds.bottom + popupMargin.top
            }

            PopupPositionProvider.Align.BottomStart -> {
                offsetX = anchorBounds.left + popupMargin.left
                offsetY = anchorBounds.top - popupContentSize.height - popupMargin.bottom
            }

            PopupPositionProvider.Align.BottomEnd -> {
                offsetX = anchorBounds.right - popupContentSize.width - popupMargin.right
                offsetY = anchorBounds.top - popupContentSize.height - popupMargin.bottom
            }

            else -> {
                offsetX = if (resolved == PopupPositionProvider.Align.End) {
                    anchorBounds.right - popupContentSize.width - popupMargin.right
                } else {
                    anchorBounds.left + popupMargin.left
                }
                offsetY = when {
                    windowBounds.bottom - anchorBounds.bottom > popupContentSize.height ->
                        anchorBounds.bottom + popupMargin.bottom

                    anchorBounds.top - windowBounds.top > popupContentSize.height ->
                        anchorBounds.top - popupContentSize.height - popupMargin.top

                    else -> anchorBounds.top + anchorBounds.height / 2 - popupContentSize.height / 2
                }
            }
        }
        return IntOffset(
            x = offsetX.coerceIn(
                windowBounds.left,
                (windowBounds.right - popupContentSize.width - popupMargin.right)
                    .coerceAtLeast(windowBounds.left),
            ),
            y = offsetY.coerceIn(
                (windowBounds.top + popupMargin.top)
                    .coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom),
                windowBounds.bottom - popupContentSize.height - popupMargin.bottom,
            ),
        )
    }

    override fun getMargins(): PaddingValues = PaddingValues(start = 20.dp)
}

private fun PopupPositionProvider.Align.resolve(layoutDirection: LayoutDirection): PopupPositionProvider.Align {
    if (layoutDirection == LayoutDirection.Ltr) return this
    return when (this) {
        PopupPositionProvider.Align.Start -> PopupPositionProvider.Align.End
        PopupPositionProvider.Align.End -> PopupPositionProvider.Align.Start
        PopupPositionProvider.Align.TopStart -> PopupPositionProvider.Align.TopEnd
        PopupPositionProvider.Align.TopEnd -> PopupPositionProvider.Align.TopStart
        PopupPositionProvider.Align.BottomStart -> PopupPositionProvider.Align.BottomEnd
        PopupPositionProvider.Align.BottomEnd -> PopupPositionProvider.Align.BottomStart
    }
}

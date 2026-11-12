package io.github.magisk317.uikit.preference

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.magisk317.uikit.surface.AppBasicDialog
import io.github.magisk317.uikit.surface.AppDialogSurface
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!expanded) return

    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        AppBasicDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
        ) {
            AppDialogSurface(title = title) {
                SingleChoiceOptionList(
                    options = options,
                    selectedIndex = selectedIndex,
                    onSelectionChange = onSelectionChange,
                    enabled = enabled,
                )
            }
        }
        return
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        options.forEachIndexed { index, option ->
            DropdownMenuItem(
                text = { Text(option) },
                enabled = enabled,
                onClick = {
                    onSelectionChange(index)
                    onDismissRequest()
                },
            )
        }
    }
}

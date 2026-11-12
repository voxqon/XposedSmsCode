package io.github.magisk317.uikit.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.R
import io.github.magisk317.uikit.surface.AppAlertDialog
import io.github.magisk317.uikit.surface.AppBasicDialog
import io.github.magisk317.uikit.surface.AppDialogSurface
import io.github.magisk317.uikit.surface.AppTextButton
import io.github.magisk317.uikit.theme.AppColorRole
import io.github.magisk317.uikit.theme.appColor

@Composable
fun SingleChoiceOptionList(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(options) { index, option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelectionChange(index) },
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppRadioButton(
                    selected = index == selectedIndex,
                    onClick = null,
                    enabled = enabled,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = option,
                    // The list can sit on a Miuix dialog card (AppDialogSurface); the role
                    // keeps the text on the active style's palette instead of the M3 one.
                    color = appColor(AppColorRole.OnSurface),
                )
            }
        }
    }
}

@Composable
fun SingleChoiceOptionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    if (confirmButton == null && dismissButton == null) {
        AppBasicDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
        ) {
            AppDialogSurface(title = title, modifier = modifier) {
                SingleChoiceOptionList(
                    options = options,
                    selectedIndex = selectedIndex,
                    onSelectionChange = onSelectionChange,
                )
            }
        }
        return
    }

    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Text(text = title) },
        text = {
            SingleChoiceOptionList(
                options = options,
                selectedIndex = selectedIndex,
                onSelectionChange = onSelectionChange,
            )
        },
        confirmButton = { confirmButton?.invoke() },
        dismissButton = dismissButton,
    )
}

/** A buttonless single-choice dialog that reports the tapped screen position. */
@Composable
fun SingleChoicePositionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (index: Int, screenPosition: Offset) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBasicDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        AppDialogSurface(title = title, modifier = modifier) {
            Column {
                options.forEachIndexed { index, option ->
                    var rowCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }
                    val view = LocalView.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .onGloballyPositioned { rowCoordinates = it }
                            .pointerInput(Unit) {
                                detectTapGestures { tapOffset ->
                                    val locationOnScreen = IntArray(2)
                                    view.getLocationOnScreen(locationOnScreen)
                                    val rootOffset = rowCoordinates?.positionInRoot() ?: Offset.Zero
                                    onSelectionChange(
                                        index,
                                        Offset(
                                            x = locationOnScreen[0] + rootOffset.x + tapOffset.x,
                                            y = locationOnScreen[1] + rootOffset.y + tapOffset.y,
                                        ),
                                    )
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppRadioButton(
                            selected = index == selectedIndex,
                            onClick = null,
                        )
                        Text(
                            text = option,
                            color = appColor(AppColorRole.OnSurface),
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SingleChoiceConfirmDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceOptionDialog(
        title = title,
        options = options,
        selectedIndex = selectedIndex,
        onSelectionChange = onSelectionChange,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        confirmButton = {
            AppTextButton(
                text = stringResource(R.string.uikit_action_confirm),
                enabled = selectedIndex in options.indices,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            AppTextButton(
                text = stringResource(R.string.uikit_action_cancel),
                onClick = onDismissRequest,
            )
        },
    )
}

@Composable
fun SingleChoiceValueDialog(
    title: String,
    options: List<String>,
    values: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceOptionDialog(
        title = title,
        options = options,
        selectedIndex = selectedValueIndex(values, selectedValue),
        onSelectionChange = { index ->
            values.getOrNull(index)?.let(onValueChange)
        },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    )
}

@Composable
fun SingleChoiceValueConfirmDialog(
    title: String,
    options: List<String>,
    values: List<String>,
    selectedValue: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember(selectedValue, values) {
        mutableIntStateOf(selectedValueIndex(values, selectedValue))
    }

    SingleChoiceConfirmDialog(
        title = title,
        options = options,
        selectedIndex = selectedIndex,
        onSelectionChange = { selectedIndex = it },
        onDismissRequest = onDismissRequest,
        onConfirm = {
            values.getOrNull(selectedIndex)?.let(onConfirm)
        },
        modifier = modifier,
    )
}

private fun selectedValueIndex(values: List<String>, selectedValue: String): Int {
    val selectedIndex = values.indexOf(selectedValue)
    return when {
        selectedIndex >= 0 -> selectedIndex
        values.isNotEmpty() -> 0
        else -> -1
    }
}

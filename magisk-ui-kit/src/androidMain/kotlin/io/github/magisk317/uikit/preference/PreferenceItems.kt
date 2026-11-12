package io.github.magisk317.uikit.preference

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import io.github.magisk317.uikit.surface.AppTextField
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.R
import io.github.magisk317.uikit.surface.AppAlertDialog
import io.github.magisk317.uikit.surface.AppIcon
import io.github.magisk317.uikit.surface.AppIconButton
import io.github.magisk317.uikit.surface.AppTextButton
import io.github.magisk317.uikit.surface.rememberSaveableTextFieldState
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.spacing
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A group of preference rows inside one card.
 *
 * The header is optional: pass a `title` when the group has siblings to tell apart, and leave it
 * out when the screen *is* the group - a header there restates the title the top bar already shows,
 * and a summary of a group is a hand-copy of the rows under it, which goes stale without any check
 * failing. `accordionMode` (with `sectionExpanded` / `onExpandedChange`) is for collapsible groups;
 * a static group omits all three.
 */
@Composable
fun SectionCard(
    title: String? = null,
    summary: String = "",
    accordionMode: Boolean = false,
    sectionExpanded: Boolean = true,
    onExpandedChange: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    // The horizontal gutter lives here and nowhere else: hosts must not add
    // their own page padding around section cards (double-gutter regression).
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixSectionCard(
            title = title,
            summary = summary,
            accordionMode = accordionMode,
            sectionExpanded = sectionExpanded,
            onExpandedChange = onExpandedChange,
            content = content,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.small),
    ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (title != null) {
                ListItem(
                    supportingContent = summary.takeIf(String::isNotEmpty)?.let { sectionSummary ->
                        {
                            Text(
                                text = sectionSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    trailingContent = {
                        if (accordionMode) {
                            Icon(
                                imageVector = if (sectionExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = accordionMode, onClick = onExpandedChange),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            AnimatedVisibility(visible = if (accordionMode) sectionExpanded else true) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        }
    }
    }
}

@Composable
fun AppArrowItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String = "",
    enabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixItem(
            title = title,
            summary = summary,
            modifier = modifier,
            enabled = enabled,
            trailingContent = trailingContent,
            onClick = onClick,
        )
        return
    }

    ListItem(
        supportingContent = if (summary.isNotEmpty()) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            null
        },
        trailingContent = trailingContent,
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun StateSwitchItem(
    title: String,
    summary: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onTitleClick: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixStateSwitchItem(
            title = title,
            summary = summary,
            checked = checked,
            enabled = enabled,
            modifier = modifier,
            onTitleClick = onTitleClick,
            onCheckedChange = onCheckedChange,
        )
        return
    }

    ListItem(
        supportingContent = if (summary.isNotEmpty()) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (onTitleClick != null) {
                        Modifier.clickable(enabled = enabled, onClick = onTitleClick)
                    } else {
                        Modifier
                    },
                )
            }
        } else {
            null
        },
        trailingContent = {
            AppSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
        modifier = if (onTitleClick == null) {
            modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
        } else {
            modifier
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = if (onTitleClick != null) {
                Modifier.clickable(enabled = enabled, onClick = onTitleClick)
            } else {
                Modifier
            },
        )
    }
}

@Composable
fun ActionSwitchItem(
    title: String,
    summary: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixActionSwitchItem(
            title = title,
            summary = summary,
            checked = checked,
            enabled = enabled,
            modifier = modifier,
            onClick = onClick,
            onCheckedChange = onCheckedChange,
        )
        return
    }

    ListItem(
        supportingContent = if (summary.isNotEmpty()) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            null
        },
        trailingContent = {
            AppSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun MiuixSectionCard(
    title: String?,
    summary: String,
    accordionMode: Boolean,
    sectionExpanded: Boolean,
    onExpandedChange: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.small),
    ) {
    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        if (title != null) {
            BasicComponent(
                title = title,
                summary = summary.ifEmpty { null },
                onClick = if (accordionMode) onExpandedChange else null,
                endActions = {
                    if (accordionMode) {
                        Image(
                            imageVector = MiuixIcons.Basic.ArrowRight,
                            contentDescription = null,
                            modifier = Modifier
                                .graphicsLayer { rotationZ = if (sectionExpanded) 90f else 0f }
                                .padding(end = 8.dp),
                            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantActions),
                        )
                    }
                },
                insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            )
        }
        AnimatedVisibility(visible = if (accordionMode) sectionExpanded else true) {
            io.github.magisk317.uikit.surface.UiKitRowInsideCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    content()
                }
            }
        }
    }
    }
}

@Composable
private fun MiuixItem(
    title: String,
    summary: String,
    modifier: Modifier,
    enabled: Boolean,
    trailingContent: @Composable (() -> Unit)?,
    onClick: () -> Unit,
) {
    BasicComponent(
        modifier = modifier.fillMaxWidth(),
        title = title,
        titleColor = BasicComponentDefaults.titleColor(
            color = MiuixTheme.colorScheme.onSurface,
            disabledColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        summary = summary.ifEmpty { null },
        summaryColor = BasicComponentDefaults.summaryColor(
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            disabledColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.38f),
        ),
        enabled = enabled,
        onClick = onClick,
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
            MiuixItemArrow(enabled = enabled)
        },
    )
}

@Composable
private fun MiuixStateSwitchItem(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onTitleClick: (() -> Unit)?,
    onCheckedChange: (Boolean) -> Unit,
) {
    BasicComponent(
        modifier = modifier.fillMaxWidth(),
        title = title,
        titleColor = BasicComponentDefaults.titleColor(
            color = MiuixTheme.colorScheme.onSurface,
            disabledColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        summary = summary.ifEmpty { null },
        summaryColor = BasicComponentDefaults.summaryColor(
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            disabledColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.38f),
        ),
        enabled = enabled,
        onClick = onTitleClick ?: { onCheckedChange(!checked) },
        endActions = {
            MiuixSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}

@Composable
private fun MiuixActionSwitchItem(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    BasicComponent(
        modifier = modifier.fillMaxWidth(),
        title = title,
        titleColor = BasicComponentDefaults.titleColor(
            color = MiuixTheme.colorScheme.onSurface,
            disabledColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        summary = summary.ifEmpty { null },
        summaryColor = BasicComponentDefaults.summaryColor(
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            disabledColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.38f),
        ),
        enabled = enabled,
        onClick = onClick,
        endActions = {
            MiuixSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}

@Composable
private fun MiuixItemArrow(enabled: Boolean) {
    Image(
        imageVector = MiuixIcons.Basic.ArrowRight,
        contentDescription = null,
        modifier = Modifier.padding(end = 8.dp),
        colorFilter = ColorFilter.tint(
            if (enabled) MiuixTheme.colorScheme.onSurfaceVariantActions
            else MiuixTheme.colorScheme.disabledOnSecondaryVariant,
        ),
    )
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
        return
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixCheckbox(
            state = ToggleableState(checked),
            onClick = if (onCheckedChange != null) {
                { onCheckedChange(!checked) }
            } else {
                null
            },
            modifier = modifier,
            enabled = enabled,
        )
        return
    }

    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun AppRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixRadioButton(
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
        return
    }

    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

/**
 * 列表选择设置项
 *
 * 点击后弹出对话框，显示单选列表
 */
@Composable
fun ListItemSelection(
    title: String,
    summary: String,
    values: List<String>,
    selected: Int,
    onValueSelected: (Int) -> Unit,
) {
    var shouldShowDialog by remember { mutableStateOf(false) }

    AppArrowItem(
        title = title,
        summary = summary,
        onClick = { shouldShowDialog = true },
    )

    if (shouldShowDialog) {
        SingleChoiceOptionDialog(
            title = title,
            options = values,
            selectedIndex = selected,
            onSelectionChange = { index ->
                onValueSelected(index)
                shouldShowDialog = false
            },
            onDismissRequest = { shouldShowDialog = false },
        )
    }
}

/**
 * 自定义对话框设置项
 *
 * 点击后弹出对话框，显示自定义内容
 */
@Composable
fun DialogItem(
    title: String,
    summary: String,
    shouldShowDialog: Boolean,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AppArrowItem(
        title = title,
        summary = summary,
        onClick = onClick,
    )

    if (shouldShowDialog) {
        AppAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = title) },
            text = content,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
        )
    }
}

/**
 * 文本输入对话框
 */
@Composable
fun TextInputDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 6,
    supportingText: String? = null,
    resetValue: String? = null,
    validator: ((String) -> String?)? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default,
    inputFilter: ((String) -> String)? = null,
    onFocusLost: ((String) -> Unit)? = null,
    showClearButton: Boolean = false,
    // When true the initial text is fully selected on open (TextRange(0, length))
    // instead of placing the caret at the end. Mirrors the "select-all on edit"
    // affordance Compose 1.12's SelectionState brings to read-only text, applied
    // to the editable field here, so overwrite-style edits (URLs, identifiers,
    // phone numbers) pre-select the existing value for one-tap replacement.
    selectAllOnOpen: Boolean = false,
    onConfirm: (String) -> Unit,
) {
    val normalizedInitial = inputFilter?.invoke(initialValue) ?: initialValue
    val state = rememberSaveableTextFieldState(
        initialText = normalizedInitial,
        initialSelection = if (selectAllOnOpen) {
            TextRange(0, normalizedInitial.length)
        } else {
            TextRange(normalizedInitial.length)
        },
        title, initialValue, selectAllOnOpen,
    )
    var hadFocus by remember(title, initialValue) { mutableStateOf(false) }
    val currentText = state.text.toString()
    val errorMessage = validator?.invoke(currentText)
    val filter = inputFilter
    val inputTransformation = filter?.let { fn ->
        androidx.compose.foundation.text.input.InputTransformation {
            val original = toString()
            val normalized = fn(original)
            if (normalized != original) {
                replace(0, length, normalized)
                selection = TextRange(normalized.length)
            }
        }
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (resetValue != null) {
                    AppTextButton(
                        text = stringResource(R.string.uikit_action_reset),
                        onClick = {
                            val normalized = inputFilter?.invoke(resetValue) ?: resetValue
                            state.edit {
                                replace(0, length, normalized)
                                selection = if (selectAllOnOpen) {
                                    TextRange(0, normalized.length)
                                } else {
                                    TextRange(normalized.length)
                                }
                            }
                        },
                    )
                }
            }
        },
        text = {
            AppTextField(
                state = state,
                label = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hadFocus = true
                        } else if (hadFocus) {
                            hadFocus = false
                            onFocusLost?.invoke(state.text.toString())
                        }
                    },
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardOptions = keyboardOptions,
                inputTransformation = inputTransformation,
                trailingIcon = if (showClearButton) {
                    {
                        if (currentText.isNotEmpty()) {
                            AppIconButton(
                                onClick = {
                                    state.edit { replace(0, length, "") }
                                },
                            ) {
                                AppIcon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.uikit_action_clear),
                                )
                            }
                        }
                    }
                } else {
                    null
                },
                supportingText = if (errorMessage != null || supportingText != null) {
                    { Text(text = errorMessage ?: supportingText!!, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else null,
            )
        },
        confirmButton = {},
        dismissButton = {
            io.github.magisk317.uikit.surface.DialogActionRow(
                actions = listOf(
                    io.github.magisk317.uikit.surface.DialogAction(
                        label = stringResource(R.string.uikit_action_cancel),
                        onClick = onDismiss,
                        style = io.github.magisk317.uikit.surface.DialogActionStyle.Secondary,
                    ),
                    io.github.magisk317.uikit.surface.DialogAction(
                        label = stringResource(R.string.uikit_action_confirm),
                        enabled = errorMessage == null,
                        onClick = {
                            if (errorMessage == null) {
                                onConfirm(state.text.toString())
                            }
                        },
                        style = io.github.magisk317.uikit.surface.DialogActionStyle.Primary,
                    ),
                ),
            )
        },
    )
}

@Composable
fun NonNegativeIntegerInputDialog(
    title: String,
    initialValue: Int,
    errorText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    minimumValue: Int = 0,
    supportingText: String? = null,
    showClearButton: Boolean = false,
    onConfirm: (Int) -> Unit,
) {
    require(minimumValue >= 0) { "minimumValue must be non-negative" }
    TextInputDialog(
        title = title,
        initialValue = initialValue.toString(),
        onDismiss = onDismiss,
        modifier = modifier,
        supportingText = supportingText,
        validator = { input ->
            if (parseNonNegativeIntAtLeast(input, minimumValue) != null) {
                null
            } else {
                errorText
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
        ),
        inputFilter = ::filterNonNegativeIntegerInput,
        showClearButton = showClearButton,
    ) { value ->
        parseNonNegativeIntAtLeast(value, minimumValue)?.let(onConfirm)
    }
}

private fun filterNonNegativeIntegerInput(raw: String): String = buildString(raw.length) {
    raw.forEach { ch ->
        ch.digitToIntOrNull()?.let { digit -> append(digit) }
    }
}

private fun parseNonNegativeIntAtLeast(raw: String, minimumValue: Int): Int? {
    return filterNonNegativeIntegerInput(raw)
        .toIntOrNull()
        ?.takeIf { it >= minimumValue }
}

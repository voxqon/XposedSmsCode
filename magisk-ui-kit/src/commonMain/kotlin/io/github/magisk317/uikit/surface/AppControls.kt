package io.github.magisk317.uikit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.text.AppText
import io.github.magisk317.uikit.text.AppTextRole
import io.github.magisk317.uikit.theme.AppColorRole
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.appColor
import io.github.magisk317.uikit.theme.spacing
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFloatingActionButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorColors
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Resolve Miuix button colors from the style defaults, overriding only the
 * slots the call site set. [Color.Unspecified] keeps the theme default for
 * that slot, mirroring how the Material 3 [ButtonDefaults] factories treat an
 * unset colour.
 */
@Composable
private fun miuixButtonColors(
    primary: Boolean,
    containerColor: Color,
    contentColor: Color,
): ButtonColors {
    val base = if (primary) {
        MiuixButtonDefaults.buttonColorsPrimary()
    } else {
        MiuixButtonDefaults.buttonColors()
    }
    return base.copy(
        color = if (containerColor == Color.Unspecified) base.color else containerColor,
        contentColor = if (contentColor == Color.Unspecified) base.contentColor else contentColor,
    )
}

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = miuixButtonColors(
                primary = true,
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            MiuixText(text = text, maxLines = maxLines, overflow = overflow)
        }
        return
    }

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(text = text, maxLines = maxLines, overflow = overflow)
    }
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        // Miuix has no outlined button variant; render the secondary button with the distinct
        // secondaryVariant color so it reads as secondary vs the primary (primary) button.
        MiuixButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = miuixButtonColors(
                primary = false,
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            MiuixText(text = text, maxLines = maxLines, overflow = overflow)
        }
        return
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(text = text, maxLines = maxLines, overflow = overflow)
    }
}

@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    interactionSource: MutableInteractionSource? = null,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        // Miuix TextButton renders its label through MiuixTheme.textStyles.button (no colour
        // of its own), so the text colour has to travel through the TextButtonColors slot.
        val baseColors = MiuixButtonDefaults.textButtonColors()
        val colors = if (color == Color.Unspecified) {
            baseColors
        } else {
            baseColors.copy(textColor = color)
        }
        MiuixTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        )
        return
    }

    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Text(text = text, color = color)
    }
}


@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val resolvedTint = if (tint != Color.Unspecified) {
        tint
    } else if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        val miuixColor = top.yukonga.miuix.kmp.theme.LocalContentColor.current
        if (miuixColor != Color.Unspecified) miuixColor else appColor(AppColorRole.OnSurface)
    } else {
        val materialColor = LocalContentColor.current
        if (materialColor != Color.Unspecified) materialColor else appColor(AppColorRole.OnSurface)
    }

    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = resolvedTint,
        )
        return
    }

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = resolvedTint,
    )
}

/**
 * Painter variant of [AppIcon]: drawable resources loaded through
 * `painterResource` are the common case in screens that were migrated from
 * XML, and routing them through the same tint resolution keeps a shared body
 * from falling back to a bare material3 `Icon` (which renders as
 * `Color.Black` under the Miuix theme).
 */
@Composable
fun AppIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val resolvedTint = if (tint != Color.Unspecified) {
        tint
    } else if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        val miuixColor = top.yukonga.miuix.kmp.theme.LocalContentColor.current
        if (miuixColor != Color.Unspecified) miuixColor else appColor(AppColorRole.OnSurface)
    } else {
        val materialColor = LocalContentColor.current
        if (materialColor != Color.Unspecified) materialColor else appColor(AppColorRole.OnSurface)
    }

    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        top.yukonga.miuix.kmp.basic.Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = resolvedTint,
        )
        return
    }

    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = resolvedTint,
    )
}

@Composable
fun AppPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = miuixButtonColors(
                primary = true,
                containerColor = containerColor,
                contentColor = contentColor,
            ),
            content = content,
        )
        return
    }

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        content = content,
    )
}

@Composable
fun AppSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues? = null,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            insideMargin = contentPadding ?: MiuixButtonDefaults.InsideMargin,
            colors = miuixButtonColors(
                primary = false,
                containerColor = containerColor,
                contentColor = contentColor,
            ),
            content = content,
        )
        return
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding ?: ButtonDefaults.ContentPadding,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        content = content,
    )
}

@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            content = content,
        )
        return
    }

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        content = content,
    )
}

@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp),
            )
        }
        return
    }

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

/**
 * Icon + label FAB. The Material track delegates to
 * `ExtendedFloatingActionButton`; miuix 0.9.x has no extended variant, so the
 * Miuix track builds one from `MiuixFloatingActionButton` - its container is
 * sized by `minWidth`/`minHeight` rather than a fixed box, so a padded Row of
 * icon + label grows past the circular default instead of being clipped.
 */
@Composable
fun AppExtendedFloatingActionButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppIcon(painter = icon, contentDescription = null)
                AppText(text = text, role = AppTextRole.Body)
            }
        }
        return
    }

    ExtendedFloatingActionButton(
        text = { AppText(text = text, role = AppTextRole.Body) },
        icon = { AppIcon(painter = icon, contentDescription = null) },
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val contentColor = appColor(AppColorRole.OnSurface)
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        top.yukonga.miuix.kmp.basic.IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        ) {
            CompositionLocalProvider(
                top.yukonga.miuix.kmp.theme.LocalContentColor provides contentColor,
                LocalContentColor provides contentColor,
                content = content,
            )
        }
        return
    }

    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            content = content,
        )
    }
}

@Composable
fun AppOutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    containerColor: Color = appColor(AppColorRole.SurfaceContainer),
    borderColor: Color = appColor(AppColorRole.Outline),
    contentColor: Color = appColor(AppColorRole.OnSurface),
    content: @Composable () -> Unit,
) {
    val styledModifier = modifier
        .clip(shape)
        .border(
            width = 1.dp,
            color = borderColor,
            shape = shape,
        )
        .background(
            color = containerColor,
            shape = shape,
        )

    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        top.yukonga.miuix.kmp.basic.IconButton(
            onClick = onClick,
            modifier = styledModifier,
            enabled = enabled,
        ) {
            CompositionLocalProvider(
                top.yukonga.miuix.kmp.theme.LocalContentColor provides contentColor,
                LocalContentColor provides contentColor,
                content = content,
            )
        }
        return
    }

    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = styledModifier,
        enabled = enabled,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            content = content,
        )
    }
}

@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        // Miuix groups both colours in ProgressIndicatorColors. Unspecified on
        // both slots keeps ProgressIndicatorDefaults' theme colours; a single
        // explicit slot falls back to the theme token, not Color.Unspecified,
        // which the underlying Canvas would draw as transparent.
        val colors = if (color == Color.Unspecified && trackColor == Color.Unspecified) {
            ProgressIndicatorDefaults.progressIndicatorColors()
        } else {
            ProgressIndicatorColors(
                foregroundColor = if (color == Color.Unspecified) {
                    MiuixTheme.colorScheme.primary
                } else {
                    color
                },
                disabledForegroundColor = if (color == Color.Unspecified) {
                    MiuixTheme.colorScheme.disabledPrimarySlider
                } else {
                    color
                },
                backgroundColor = if (trackColor == Color.Unspecified) {
                    MiuixTheme.colorScheme.secondaryContainer
                } else {
                    trackColor
                },
            )
        }
        top.yukonga.miuix.kmp.basic.CircularProgressIndicator(
            modifier = modifier,
            progress = progress,
            colors = colors,
            strokeWidth = strokeWidth,
        )
        return
    }

    // Material theme tokens stand in for the unset slots (they match
    // ProgressIndicatorTokens' ActiveIndicatorColor/TrackColor intent). With
    // both slots Unspecified the overload call is identical to the
    // defaults-only path, so theme behaviour is untouched.
    val indicatorColor = if (color == Color.Unspecified) {
        MaterialTheme.colorScheme.primary
    } else {
        color
    }
    val indicatorTrackColor = if (trackColor == Color.Unspecified) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        trackColor
    }

    if (progress == null) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = modifier,
            color = indicatorColor,
            trackColor = indicatorTrackColor,
            strokeWidth = strokeWidth,
        )
    } else {
        androidx.compose.material3.CircularProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = indicatorColor,
            trackColor = indicatorTrackColor,
            strokeWidth = strokeWidth,
        )
    }
}

@Composable
fun AppLinearProgressIndicator(
    progress: Float? = null,
    modifier: Modifier = Modifier,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixLinearProgressIndicator(
            progress = progress,
            modifier = modifier,
        )
        return
    }

    if (progress == null) {
        LinearProgressIndicator(modifier = modifier)
    } else {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
        )
    }
}

/**
 * Single-value slider rendered with the widget matching the active UI kit
 * style, so hosts never branch on [UiKitStyle] themselves.
 *
 * The contract is the stateless pair ([value], [onValueChange]) - the shape
 * miuix's [MiuixSlider] natively exposes. Hosts that need a dialog-scoped
 * draft (a slider inside a confirm dialog, for example) keep their own Float
 * state; Material 3's own state-based overload is therefore not re-exported.
 *
 * [steps] is forwarded natively on both tracks: 0 keeps the slider
 * continuous, a positive value snaps it to that many discrete stops.
 */
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
        return
    }

    val state = rememberSliderState(value = value, steps = steps, trackRange = valueRange)
    SideEffect { state.value = value }
    Slider(
        state = state,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
    )
}

/**
 * Caller-owned state text field. The [state] is owned by the call site, which
 * keeps validation, reset, clear and password visibility logic operating on a
 * single source of truth. Miuix renders through the state-based TextField API
 * directly; Material 3 renders through its state-based OutlinedTextField.
 *
 * [isError] is forwarded natively on the Material track. miuix's TextField has
 * no error state (verified against miuix 0.9.4), so the Miuix track conveys
 * errors through supportingText - colour it with
 * `appColor(AppColorRole.Error)` at the call site.
 */
@Composable
fun AppTextField(
    state: TextFieldState,
    label: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    placeholderText: String? = null,
    supportingText: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MiuixTextField(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                label = placeholderText ?: label,
                enabled = enabled,
                readOnly = readOnly,
                inputTransformation = inputTransformation,
                textStyle = textStyle,
                keyboardOptions = keyboardOptions,
                onKeyboardAction = onKeyboardAction,
                lineLimits = textFieldLineLimits(singleLine, minLines, maxLines),
                trailingIcon = trailingIcon,
                onTextLayout = { getResult -> getResult()?.let(onTextLayout) },
                useLabelAsPlaceholder = placeholderText != null,
                outputTransformation = outputTransformation,
            )
            if (supportingText != null) {
                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    supportingText()
                }
            }
        }
        return
    }

    OutlinedTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        textStyle = textStyle,
        label = { Text(label) },
        placeholder = placeholderText?.let { text -> { Text(text) } },
        supportingText = supportingText,
        trailingIcon = trailingIcon,
        lineLimits = textFieldLineLimits(singleLine, minLines, maxLines),
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        onTextLayout = { getResult -> getResult()?.let(onTextLayout) },
    )
}

/**
 * String-value variant of [AppTextField] for call sites that keep their own
 * source of truth (a draft object, for example) and need external writes - a
 * refresh/regenerate button - to land in the field. The internal
 * [TextFieldState] syncs both ways: user edits propagate out through
 * [onValueChange]; an external change to [value] is written back into the
 * field (the cursor jumps to the end only on those external writes).
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    label: String = "",
    placeholderText: String? = null,
    supportingText: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val state = rememberSaveableTextFieldState(value)
    LaunchedEffect(state.text) {
        val current = state.text.toString()
        if (current != value) onValueChange(current)
    }
    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            state.edit {
                replace(0, length, value)
                selection = TextRange(value.length)
            }
        }
    }
    AppTextField(
        state = state,
        label = label,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        placeholderText = placeholderText,
        supportingText = supportingText,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        textStyle = textStyle,
        onTextLayout = onTextLayout,
    )
}

private val TextFieldStateSaver = listSaver<TextFieldState, Any>(
    save = { listOf(it.text.toString(), it.selection.start, it.selection.end) },
    restore = { TextFieldState(it[0] as String, TextRange(it[1] as Int, it[2] as Int)) },
)

/**
 * A [rememberSaveable] variant of [androidx.compose.foundation.text.input.rememberTextFieldState]:
 * TextFieldState itself is not a SavableState, so the text and selection are
 * persisted explicitly and survive process death.
 */
@Composable
fun rememberSaveableTextFieldState(
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length),
    vararg inputs: Any?,
): TextFieldState = rememberSaveable(*inputs, saver = TextFieldStateSaver) {
    TextFieldState(initialText, initialSelection)
}

private fun textFieldLineLimits(
    singleLine: Boolean,
    minLines: Int,
    maxLines: Int,
): TextFieldLineLimits = if (singleLine) {
    TextFieldLineLimits.SingleLine
} else {
    TextFieldLineLimits.MultiLine(minLines, maxLines)
}

@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiuixText(
                text = title,
                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title3,
                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
            )
            if (!summary.isNullOrBlank()) {
                MiuixText(
                    text = summary,
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DialogButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        androidx.compose.foundation.layout.Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterHorizontally),
            content = content,
        )
        return
    }
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small, androidx.compose.ui.Alignment.CenterHorizontally),
        content = content,
    )
}

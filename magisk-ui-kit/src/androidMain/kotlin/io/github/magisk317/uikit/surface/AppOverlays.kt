package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.R
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.spacing
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Text as MiuixText

@Composable
fun AppDialogSurface(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        val dialogCornerRadius = 26.dp
        MiuixCard(
            modifier = modifier
                .fillMaxWidth()
                .uiKitSurfaceBlur(
                    shape = RoundedCornerShape(dialogCornerRadius),
                    surfaceColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                ),
            cornerRadius = dialogCornerRadius,
            colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
                color = surfaceBlurContainerColor(MiuixTheme.colorScheme.surfaceContainer),
            ),
            insideMargin = PaddingValues(0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!title.isNullOrBlank()) {
                    MiuixText(
                        text = title,
                        style = MiuixTheme.textStyles.title2,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
                content()
            }
        }
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .uiKitSurfaceBlur(
                shape = MaterialTheme.shapes.extraLarge,
                surfaceColor = MaterialTheme.colorScheme.surface,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = surfaceBlurContainerColor(MaterialTheme.colorScheme.surface),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}

@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable (() -> Unit) = {},
    dismissButton: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
        val bottomMargin = if (bottomInset > 0.dp) bottomInset + 12.dp else 24.dp
        WindowDialog(
            show = true,
            title = null,
            backgroundColor = Color.Transparent,
            outsideMargin = DpSize(0.dp, 0.dp),
            insideMargin = DpSize(0.dp, 0.dp),
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Top),
            ),
            onDismissRequest = onDismissRequest,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = bottomMargin),
            ) {
                AppDialogSurface(modifier = modifier) {
                    if (title != null) {
                        title()
                    }
                    if (text != null) {
                        text()
                    }
                    DialogButtonRow {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small, Alignment.CenterHorizontally),
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        },
        dismissButton = null,
        modifier = modifier,
        properties = properties,
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.uikit_action_cancel),
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        AppBasicDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
        ) {
            AppDialogSurface(title = title) {
                MiuixText(
                    text = message,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                DialogActionRow(
                    actions = listOf(
                        DialogAction(
                            label = cancelText,
                            onClick = onDismissRequest,
                            style = DialogActionStyle.Secondary,
                        ),
                        DialogAction(
                            label = confirmText,
                            onClick = onConfirm,
                            style = DialogActionStyle.Primary,
                        ),
                    ),
                )
            }
        }
        return
    }

    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            AppTextButton(
                text = confirmText,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            AppTextButton(
                text = cancelText,
                onClick = onDismissRequest,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBasicDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
        val bottomMargin = if (bottomInset > 0.dp) bottomInset + 12.dp else 24.dp
        WindowDialog(
            show = true,
            title = null,
            backgroundColor = Color.Transparent,
            outsideMargin = DpSize(0.dp, 0.dp),
            insideMargin = DpSize(0.dp, 0.dp),
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Top),
            ),
            onDismissRequest = onDismissRequest,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = bottomMargin),
            ) {
                content()
            }
        }
        return
    }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!show) return

    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        WindowBottomSheet(
            show = true,
            modifier = modifier,
            title = title,
            onDismissRequest = onDismissRequest,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}

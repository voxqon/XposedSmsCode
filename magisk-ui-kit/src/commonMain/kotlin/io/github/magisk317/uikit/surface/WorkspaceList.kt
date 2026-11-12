package io.github.magisk317.uikit.surface

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.spacing
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Provided `true` by grouped-card containers (DetailSectionCard, SectionCard) around their
 * content. A [WorkspaceListItem] row rendered inside such a card must stay a plain row —
 * wrapping it in its own card produces the "rounded cards joined inside a card" artifact the
 * style guide rejects. Standalone rows (app/event/permission lists) keep their card.
 */
@Composable
internal fun UiKitRowInsideCard(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalUiKitRowInsideCard provides true, content = content)
}

internal val LocalUiKitRowInsideCard = compositionLocalOf { false }

/**
 * Sizes and gaps shared by every list row, so hosts stop re-deriving them per page.
 *
 * Follow-up (deliberately not fixed here): [miuixContentPadding] keeps the historical
 * hard-coded 18/14 dp instead of deriving from `MaterialTheme.spacing`, so the layout-density
 * setting ([io.github.magisk317.uikit.theme.UiKitLayoutScale]) scales the Expressive branch
 * only. Switching Miuix to spacing tokens would move all existing call sites at once and
 * belongs in its own change.
 */
object WorkspaceListItemDefaults {
    /** Default app icon edge length; matches `AppIconImage`'s default size. */
    val iconSize: Dp = 40.dp

    /** Fixed width reserved for the leading icon column so icons line up across pages. */
    val iconColumnWidth: Dp = 56.dp

    /** Gap between the leading content and the first text line. */
    val leadingGap: Dp = 16.dp

    /** Gap between the text block and the trailing content. */
    val trailingGap: Dp = 12.dp

    /** Gap between the selection control and the leading content. */
    val selectionGap: Dp = 12.dp

    /** Vertical gap between the stacked text lines of a row. */
    val textLineGap: Dp = 4.dp

    /** Leading inset applied to [WorkspaceListDivider]. */
    val dividerInset: Dp = 16.dp

    /** Stroke width applied to [WorkspaceListDivider]. */
    val dividerThickness: Dp = 0.5.dp

    /** Row padding for the Expressive style, derived from `MaterialTheme.spacing`. */
    @Composable
    @ReadOnlyComposable
    fun expressiveContentPadding(): PaddingValues = PaddingValues(
        start = MaterialTheme.spacing.large,
        end = MaterialTheme.spacing.extraLarge,
        top = MaterialTheme.spacing.medium,
        bottom = MaterialTheme.spacing.medium,
    )

    /** Row padding for the Miuix style, kept at the historical 18/14 dp. */
    @Composable
    @ReadOnlyComposable
    fun miuixContentPadding(): PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
}

/**
 * The row body shared by all three style branches of [WorkspaceListItem], so the selection
 * control, the leading column and the gaps cannot drift apart per branch.
 */
@Composable
private fun WorkspaceListItemRow(
    modifier: Modifier,
    contentPadding: PaddingValues,
    selectionMode: Boolean,
    selectionContent: (@Composable () -> Unit)?,
    leadingContent: (@Composable () -> Unit)?,
    leadingWidth: Dp,
    trailingContent: (@Composable () -> Unit)?,
    supportingContent: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode && selectionContent != null) {
            selectionContent()
            Spacer(modifier = Modifier.size(WorkspaceListItemDefaults.selectionGap))
        }
        if (leadingContent != null) {
            if (leadingWidth != Dp.Unspecified) {
                Column(
                    modifier = Modifier.width(leadingWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    leadingContent()
                }
            } else {
                leadingContent()
            }
            Spacer(modifier = Modifier.size(WorkspaceListItemDefaults.leadingGap))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WorkspaceListItemDefaults.textLineGap),
        ) {
            content()
            supportingContent?.invoke()
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.size(WorkspaceListItemDefaults.trailingGap))
            trailingContent()
        }
    }
}

/**
 * A single row of a workspace list: optional selection control, optional leading content
 * (usually an app icon), a weighted text column and optional trailing content.
 *
 * The row renders itself for the active UI kit style: Miuix wraps standalone rows in a card
 * and keeps them plain when they already sit inside one ([UiKitRowInsideCard]), Expressive
 * paints [containerColor] behind a plain row.
 *
 * @param leadingWidth Fixed width for the leading column. Pass
 *   [WorkspaceListItemDefaults.iconColumnWidth] so icons of different pages line up;
 *   [Dp.Unspecified] (the default) keeps the historical wrap-content behaviour.
 * @param selectionMode When true the [selectionContent] slot is rendered before the leading
 *   content. Nothing is rendered when [selectionContent] is null.
 * @param selectionContent Caller-owned selection control, typically
 *   `AppCheckbox(checked = selected, onCheckedChange = onSelectionChange)`. `surface` keeps no
 *   dependency on `preference`, so no concrete control is assumed here.
 * @param selected Current selection state. The row itself does not read it; it exists so the
 *   caller's [selectionContent] can close over it.
 * @param onSelectionChange Selection callback. The row itself does not invoke it; it exists so
 *   the caller's [selectionContent] can close over it.
 * @param supportingContent Rendered below [content] in the text column. Previously dropped on
 *   the Expressive branch; all styles render it now.
 * @param showDivider Appends a [WorkspaceListDivider] below the row.
 */
@Composable
fun WorkspaceListItem(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    selectionContent: (@Composable () -> Unit)? = null,
    leadingWidth: Dp = Dp.Unspecified,
    showDivider: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier: Modifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick ?: {},
            onLongClick = onLongClick,
        )
    } else if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    val miuixPadding = WorkspaceListItemDefaults.miuixContentPadding()

    Column(modifier = modifier.fillMaxWidth()) {
        when {
            LocalUiKitStyle.current == UiKitStyle.Miuix && LocalUiKitRowInsideCard.current -> {
                WorkspaceListItemRow(
                    modifier = clickModifier,
                    contentPadding = miuixPadding,
                    selectionMode = selectionMode,
                    selectionContent = selectionContent,
                    leadingContent = leadingContent,
                    leadingWidth = leadingWidth,
                    trailingContent = trailingContent,
                    supportingContent = supportingContent,
                    content = content,
                )
            }

            LocalUiKitStyle.current == UiKitStyle.Miuix -> {
                MiuixCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(clickModifier),
                    insideMargin = PaddingValues(0.dp),
                    colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
                        color = if (containerColor == Color.Transparent) {
                            MiuixTheme.colorScheme.surfaceContainer
                        } else {
                            containerColor
                        },
                        contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                    ),
                ) {
                    WorkspaceListItemRow(
                        modifier = Modifier,
                        contentPadding = miuixPadding,
                        selectionMode = selectionMode,
                        selectionContent = selectionContent,
                        leadingContent = leadingContent,
                        leadingWidth = leadingWidth,
                        trailingContent = trailingContent,
                        supportingContent = supportingContent,
                        content = content,
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(containerColor),
                ) {
                    WorkspaceListItemRow(
                        modifier = clickModifier,
                        contentPadding = WorkspaceListItemDefaults.expressiveContentPadding(),
                        selectionMode = selectionMode,
                        selectionContent = selectionContent,
                        leadingContent = leadingContent,
                        leadingWidth = leadingWidth,
                        trailingContent = trailingContent,
                        supportingContent = supportingContent,
                        content = content,
                    )
                }
            }
        }

        if (showDivider) {
            WorkspaceListDivider()
        }
    }
}

/**
 * The standard separator between list rows: inset horizontally by
 * [WorkspaceListItemDefaults.dividerInset] on both edges, and style-aware (miuix
 * `dividerLine` / Material `outlineVariant`).
 */
@Composable
fun WorkspaceListDivider(modifier: Modifier = Modifier) {
    AppHorizontalDivider(
        modifier = modifier.padding(horizontal = WorkspaceListItemDefaults.dividerInset),
        thickness = WorkspaceListItemDefaults.dividerThickness,
    )
}

/**
 * A workspace row for record-style lists: a leading icon, a title line with a trailing meta
 * (typically the timestamp), an optional body and an optional status footer.
 *
 * Thin wrapper around [WorkspaceListItem] — it owns nothing but the stacking of the text
 * slots, so padding and gaps stay owned by [WorkspaceListItemDefaults] and both the app and
 * the record lists of a host line up.
 *
 * @param titleRow Caller-owned "title + right-aligned meta" row, e.g. the code or sender plus
 *   the formatted date.
 * @param body Optional message body, stacked below [titleRow].
 * @param footer Optional status line, stacked below [body].
 * @param selected Current selection state. The row does not read it; it exists so the caller's
 *   [selectionContent] can close over it.
 * @param selectionMode When true the [selectionContent] slot is rendered before the leading
 *   content. Nothing is rendered when [selectionContent] is null.
 * @param onSelectionChange Selection callback. The row does not invoke it; it exists so the
 *   caller's [selectionContent] can close over it.
 * @param selectionContent Caller-owned selection control, typically
 *   `AppCheckbox(checked = selected, onCheckedChange = onSelectionChange)`. `surface` keeps no
 *   dependency on `preference`, so no concrete control is assumed here.
 */
@Composable
fun WorkspaceRecordRow(
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    titleRow: @Composable () -> Unit,
    body: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    selectionContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    WorkspaceListItem(
        modifier = modifier,
        leadingContent = leadingContent,
        leadingWidth = if (leadingContent != null) {
            WorkspaceListItemDefaults.iconColumnWidth
        } else {
            Dp.Unspecified
        },
        selected = selected,
        selectionMode = selectionMode,
        onSelectionChange = onSelectionChange,
        selectionContent = selectionContent,
        showDivider = showDivider,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        titleRow()
        if (body != null) {
            Spacer(modifier = Modifier.height(WorkspaceListItemDefaults.textLineGap))
            body()
        }
        if (footer != null) {
            Spacer(modifier = Modifier.height(WorkspaceListItemDefaults.textLineGap))
            footer()
        }
    }
}

@Composable
fun WorkspaceEmptyState(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.size(16.dp))
            }
            MiuixText(
                text = title,
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.size(8.dp))
            MiuixText(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.size(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun WorkspaceTrailingIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier.size(20.dp),
        tint = tint,
    )
}

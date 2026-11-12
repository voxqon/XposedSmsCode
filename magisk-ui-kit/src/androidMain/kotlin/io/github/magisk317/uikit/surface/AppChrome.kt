package io.github.magisk317.uikit.surface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.AppColorRole
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.appColor
import io.github.magisk317.uikit.theme.spacing
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixHorizontalDivider
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val METRIC_GRID_ITEM_WIDTH_FRACTION = 0.5f

data class MetricSpec(
    val label: String,
    val value: String,
    val accent: Color? = null,
)

@Composable
fun SectionColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
    ) {
        content()
    }
}

@Composable
fun FeatureEntryCard(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixFeatureEntryCard(
            title = title,
            summary = summary,
            modifier = modifier,
            onClick = onClick,
        )
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .uiKitSurfaceBlur(
                shape = RoundedCornerShape(12.dp),
                surfaceColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceBlurContainerColor(MaterialTheme.colorScheme.surfaceContainer),
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Hero card for module/entitlement status.
 *
 * Click policy (shared across parent apps, see [rememberStatusCardClickHandler]):
 * - [isEntitled] == false and [onActivateClick] != null: a single tap opens the activation page.
 * - [isEntitled] == true and [onDiagnosticsToggle] != null: 5 taps within 1.8s toggle diagnostics.
 * - Otherwise the plain [onClick] fallback (if any) is used.
 *
 * @param highlighted true when both module activation and entitlement are OK.
 * @param isEntitled whether mobile automation entitlement is currently granted.
 * @param onActivateClick single-tap action offered while entitlement is missing.
 * @param onDiagnosticsToggle multi-tap action that toggles diagnostics while entitled.
 */
@Composable
fun StatusHeroCard(
    title: String,
    icon: ImageVector,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    diagnostics: List<Pair<String, String>> = emptyList(),
    onClick: (() -> Unit)? = null,
    isEntitled: Boolean = true,
    onActivateClick: (() -> Unit)? = null,
    onDiagnosticsToggle: (() -> Unit)? = null,
) {
    val resolvedOnClick = rememberStatusCardClickHandler(
        isEntitled = isEntitled,
        onActivateClick = onActivateClick,
        onDiagnosticsToggle = onDiagnosticsToggle,
        onClick = onClick,
    )
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixCard(
            modifier = modifier
                .fillMaxWidth()
                .uiKitSurfaceBlur(
                    shape = MaterialTheme.shapes.extraLarge,
                    surfaceColor = if (highlighted) {
                        MiuixTheme.colorScheme.primaryContainer
                    } else {
                        MiuixTheme.colorScheme.errorContainer
                    },
                ),
            onClick = resolvedOnClick,
            colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
                color = surfaceBlurContainerColor(
                    if (highlighted) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.errorContainer,
                ),
                contentColor = if (highlighted) {
                    MiuixTheme.colorScheme.onPrimaryContainer
                } else {
                    MiuixTheme.colorScheme.onErrorContainer
                },
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    top.yukonga.miuix.kmp.basic.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MiuixText(
                            text = title,
                            style = MiuixTheme.textStyles.title2,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        if (!summary.isNullOrBlank()) {
                            MiuixText(
                            text = summary,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        }
                    }
                }
                if (diagnostics.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        diagnostics.forEach { (label, value) ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                MiuixText(
                                    text = label,
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                                MiuixText(
                                    text = value,
                                    style = MiuixTheme.textStyles.body2,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    val contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer
    Card(
        modifier = modifier
            .fillMaxWidth()
            .uiKitSurfaceBlur(
                shape = MaterialTheme.shapes.extraLarge,
                surfaceColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = surfaceBlurContainerColor(
                if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer,
            ),
            contentColor = contentColor,
        ),
        onClick = { resolvedOnClick?.invoke() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
                Column {
                    Text(
                        text = title,
                        color = contentColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!summary.isNullOrBlank()) {
                        Text(
                            text = summary,
                            color = contentColor.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            if (diagnostics.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    diagnostics.forEach { (label, value) ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = contentColor.copy(alpha = 0.75f),
                            )
                            Text(
                                text = value,
                                color = contentColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricGrid(
    metrics: List<MetricSpec>,
    modifier: Modifier = Modifier,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2,
        ) {
            metrics.forEach { metric ->
                Box(modifier = Modifier.fillMaxWidth(METRIC_GRID_ITEM_WIDTH_FRACTION)) {
                    MetricCard(
                        label = metric.label,
                        value = metric.value,
                        modifier = Modifier.fillMaxWidth(),
                        accent = metric.accent ?: MiuixTheme.colorScheme.primary,
                    )
                }
            }
        }
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        maxItemsInEachRow = 2,
    ) {
        metrics.forEach { metric ->
            Box(modifier = Modifier.fillMaxWidth(METRIC_GRID_ITEM_WIDTH_FRACTION)) {
                MetricCard(
                    label = metric.label,
                    value = metric.value,
                    modifier = Modifier.fillMaxWidth(),
                    accent = metric.accent ?: MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixDetailSectionCard(
            title = title,
            summary = summary,
            modifier = modifier,
            content = content,
        )
    } else {
        ExpressiveSectionCard(
            title = title,
            summary = summary,
            modifier = modifier,
        ) {
            content()
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixSummaryRow(
            icon = null,
            label = label,
            value = value,
            modifier = modifier,
            onClick = onClick,
        )
        return
    }

    ListItem(
        leadingContent = leadingContent?.let {
            {
                it()
            }
        },
        supportingContent = null,
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
fun DetailDivider() {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixHorizontalDivider(
            modifier = Modifier.padding(horizontal = 18.dp),
            color = MiuixTheme.colorScheme.dividerLine,
        )
    } else {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        )
    }
}

@Composable
fun SummarySectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixCard(
            modifier = modifier
                .fillMaxWidth()
                .uiKitSurfaceBlur(
                    shape = RoundedCornerShape(12.dp),
                    surfaceColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                ),
            colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
                color = surfaceBlurContainerColor(MiuixTheme.colorScheme.surfaceContainer),
            ),
            insideMargin = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                content = content,
            )
        }
    } else {
        ElevatedCard(
            modifier = modifier
                .uiKitSurfaceBlur(
                    shape = RoundedCornerShape(12.dp),
                    surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = surfaceBlurContainerColor(MaterialTheme.colorScheme.surfaceContainerLow),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SummaryRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
    valueColorRole: AppColorRole? = null,
    badgeColorRole: AppColorRole? = null,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixSummaryRow(
            icon = icon,
            label = label,
            value = value,
            modifier = modifier,
            hasBadge = hasBadge,
            onClick = onClick,
        )
        return
    }

    ListItem(
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = value,
                    color = valueColorRole?.let { appColor(it) } ?: MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (hasBadge) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColorRole?.let { appColor(it) } ?: MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixFeatureEntryCard(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MiuixCard(
        modifier = modifier
            .fillMaxWidth()
            .uiKitSurfaceBlur(
                shape = RoundedCornerShape(12.dp),
                surfaceColor = MiuixTheme.colorScheme.surfaceContainerHigh,
            ),
        colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
            color = surfaceBlurContainerColor(MiuixTheme.colorScheme.surfaceContainer),
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun MiuixDetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MiuixCard(
        modifier = modifier
            .fillMaxWidth()
            .uiKitSurfaceBlur(
                shape = RoundedCornerShape(12.dp),
                surfaceColor = MiuixTheme.colorScheme.surfaceContainerHigh,
            ),
        colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
            color = surfaceBlurContainerColor(MiuixTheme.colorScheme.surfaceContainer),
        ),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        MiuixHorizontalDivider(
            modifier = Modifier.padding(horizontal = 18.dp),
            color = MiuixTheme.colorScheme.dividerLine,
        )
        UiKitRowInsideCard {
            Column(content = content)
        }
    }
}

@Composable
private fun MiuixSummaryRow(
    icon: ImageVector?,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    top.yukonga.miuix.kmp.basic.BasicComponent(
        modifier = modifier.fillMaxWidth(),
        title = label,
        summary = value,
        startAction = if (icon != null) {
            {
                top.yukonga.miuix.kmp.basic.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        onClick = onClick,
    )
}

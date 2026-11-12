package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.spacing
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OverlayHeaderScaffold(
    modifier: Modifier = Modifier,
    fallbackTopPadding: Dp,
    bottomPadding: Dp = 0.dp,
    headerOffsetY: Float = 0f,
    onHeaderHeightChanged: (Int) -> Unit = {},
    overlayModifier: Modifier = Modifier,
    overlay: @Composable ColumnScope.() -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    var overlayHeightPx by remember { mutableIntStateOf(0) }
    val clampedOffset = headerOffsetY.coerceAtMost(0f)
    val offsetDp = with(density) { clampedOffset.toDp() }
    val rawTopPadding = if (overlayHeightPx > 0) {
        with(density) { overlayHeightPx.toDp() } + offsetDp
    } else {
        fallbackTopPadding + offsetDp
    }
    val topPadding = rawTopPadding.coerceAtLeast(0.dp)
    val safeBottomPadding = bottomPadding.coerceAtLeast(0.dp)

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        content(PaddingValues(top = topPadding, bottom = safeBottomPadding))

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .offset(y = offsetDp),
        ) {
            Column(
                modifier = overlayModifier
                    .fillMaxWidth()
                    .onSizeChanged {
                        overlayHeightPx = it.height
                        onHeaderHeightChanged(it.height)
                    },
                content = overlay,
            )
        }
    }
}

@Composable
fun OverlayHeaderPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!title.isNullOrBlank() || !subtitle.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (!title.isNullOrBlank()) {
                            MiuixText(
                                text = title,
                                style = MiuixTheme.textStyles.title2,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        if (!subtitle.isNullOrBlank()) {
                            MiuixText(
                                text = subtitle,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }

            content()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.small,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.small,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        if (!title.isNullOrBlank() || !subtitle.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }

        content()
    }
}

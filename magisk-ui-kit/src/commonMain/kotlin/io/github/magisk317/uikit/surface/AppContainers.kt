package io.github.magisk317.uikit.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun Color.orElse(fallback: Color): Color =
    if (this == Color.Unspecified) fallback else this

@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    shape: Shape = MaterialTheme.shapes.medium,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        // Miuix Surface supports shape + shadowElevation, preserving the AppSurface contract
        // (Miuix Card only has cornerRadius and no elevation). Miuix has no separate
        // tonal-tint concept, so an explicit shadowElevation wins and the legacy
        // tonalElevation-as-shadow mapping is kept as the fallback.
        MiuixSurface(
            modifier = modifier,
            shape = shape,
            color = color.orElse(MiuixTheme.colorScheme.surfaceContainer),
            contentColor = contentColor.orElse(MiuixTheme.colorScheme.onSurface),
            shadowElevation = if (shadowElevation > 0.dp) shadowElevation else tonalElevation,
            content = content,
        )
        return
    }

    Surface(
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.surface),
        contentColor = contentColor.orElse(MaterialTheme.colorScheme.onSurface),
        shape = shape,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        content = content,
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    shape: Shape = MaterialTheme.shapes.medium,
    tonalElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        // Miuix has no outlined card variant; the border slot is Material-only
        // and intentionally ignored so both tracks keep the same AppCard contract.
        if (onClick == null) {
            MiuixSurface(
                modifier = modifier,
                shape = shape,
                color = color.orElse(MiuixTheme.colorScheme.surfaceContainer),
                contentColor = contentColor.orElse(MiuixTheme.colorScheme.onSurface),
                shadowElevation = tonalElevation,
                content = content,
            )
        } else {
            MiuixSurface(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                color = color.orElse(MiuixTheme.colorScheme.surfaceContainer),
                contentColor = contentColor.orElse(MiuixTheme.colorScheme.onSurface),
                shadowElevation = tonalElevation,
                content = content,
            )
        }
        return
    }

    val resolvedColor = color.orElse(MaterialTheme.colorScheme.surfaceContainerLow)
    val resolvedContentColor = contentColor.orElse(MaterialTheme.colorScheme.onSurface)
    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = resolvedColor,
                contentColor = resolvedContentColor,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = tonalElevation),
            border = border,
        ) {
            Column { content() }
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = resolvedColor,
                contentColor = resolvedContentColor,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = tonalElevation),
            border = border,
        ) {
            Column { content() }
        }
    }
}

package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.Badge as MiuixBadge
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.Text

private fun Color.orElse(fallback: Color): Color =
    if (this == Color.Unspecified) fallback else this

/**
 * Compact text status label shared by Material/Expressive and Miuix.
 * The caller owns the status meaning and colors; the UI Kit owns toolkit-specific badge chrome.
 */
@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    shape: Shape = MaterialTheme.shapes.small,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        // Miuix Badge is always circular (shape is not configurable); contentPadding is honored by
        // padding the content. The Material/Expressive branch keeps the shape-bearing Surface badge.
        MiuixBadge(
            modifier = modifier,
            containerColor = containerColor.orElse(MiuixTheme.colorScheme.secondaryContainer),
            contentColor = contentColor.orElse(MiuixTheme.colorScheme.onSecondaryContainer),
            content = {
                MiuixText(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(contentPadding),
                )
            },
        )
        return
    }

    Surface(
        modifier = modifier,
        color = containerColor.orElse(MaterialTheme.colorScheme.secondaryContainer),
        contentColor = contentColor.orElse(MaterialTheme.colorScheme.onSecondaryContainer),
        shape = shape,
    ) {
        // No explicit colour here: the text must follow the Surface contentColor so a
        // status badge renders the caller's status colour exactly like the Miuix branch.
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(contentPadding),
        )
    }
}

package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.spacing
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class DialogActionStyle {
    Primary,
    Secondary,
    Danger,
}

data class DialogAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val style: DialogActionStyle = DialogActionStyle.Primary,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DialogActionRow(
    actions: List<DialogAction>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            actions.forEach { action ->
                val buttonColors = when (action.style) {
                    DialogActionStyle.Primary -> ButtonDefaults.textButtonColorsPrimary()
                    DialogActionStyle.Danger -> ButtonDefaults.textButtonColors(
                        textColor = MiuixTheme.colorScheme.error,
                    )
                    DialogActionStyle.Secondary -> ButtonDefaults.textButtonColors()
                }
                TextButton(
                    text = action.label,
                    onClick = action.onClick,
                    modifier = Modifier.weight(1f),
                    enabled = action.enabled,
                    colors = buttonColors,
                )
            }
        }
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        actions.forEach { action ->
            val colors = when (action.style) {
                DialogActionStyle.Primary -> MaterialButtonDefaults.textButtonColors()
                DialogActionStyle.Secondary -> MaterialButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DialogActionStyle.Danger -> MaterialButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                )
            }
            TextButton(
                onClick = action.onClick,
                enabled = action.enabled,
                modifier = Modifier.weight(1f),
                colors = colors,
            ) {
                Text(action.label)
            }
        }
    }
}

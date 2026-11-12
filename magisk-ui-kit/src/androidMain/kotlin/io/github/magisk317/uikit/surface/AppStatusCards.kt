package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Displays app version information with commit hash.
 *
 * @param versionName The version name (e.g., "3.3.2")
 * @param commitHash The git commit hash (e.g., "f1aa14a9")
 * @param modifier Modifier for the composable
 */
@Composable
fun VersionInfoCard(
    versionName: String,
    commitHash: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .uiKitSurfaceBlur(
                shape = MaterialTheme.shapes.medium,
                surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = surfaceBlurContainerColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Version",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$versionName ($commitHash)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Displays module activation status.
 *
 * @param status The current activation status
 * @param modifier Modifier for the composable
 */
@Composable
fun ActivationStatusCard(
    status: ActivationStatus,
    modifier: Modifier = Modifier,
) {
    val (label, detail, color) = when (status) {
        is ActivationStatus.Loading -> Triple(
            "Module Status",
            "Checking...",
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is ActivationStatus.NotActivated -> Triple(
            "Module Status",
            "Not Activated — open settings to activate",
            MaterialTheme.colorScheme.error,
        )
        is ActivationStatus.Activated -> Triple(
            "Module Status",
            buildString {
                append("Activated by ${status.frameworkName}")
                if (status.frameworkVersion.isNotBlank()) {
                    append(" ${status.frameworkVersion}")
                }
                if (status.apiVersion > 0) {
                    append(" API ${status.apiVersion}")
                }
            },
            MaterialTheme.colorScheme.primary,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .uiKitSurfaceBlur(
                shape = MaterialTheme.shapes.medium,
                surfaceColor = if (status is ActivationStatus.NotActivated) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = surfaceBlurContainerColor(
                if (status is ActivationStatus.NotActivated) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
            ),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
        }
    }
}

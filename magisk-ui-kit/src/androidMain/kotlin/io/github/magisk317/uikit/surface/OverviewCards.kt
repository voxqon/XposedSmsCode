package io.github.magisk317.uikit.surface

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.R
import io.github.magisk317.uikit.theme.AppColorRole
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.appColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Transient state rendered inside an overview row's value slot (KernelSU/Magisk
 * row-state model): the row shows what a check/update is doing instead of
 * spawning a separate hint line below. [colorRole]/[badgeColorRole] are style
 * roles resolved through [appColor]; `null` keeps the default value styling.
 */
data class OverviewValueState(
    val text: String,
    val colorRole: AppColorRole? = null,
    val badge: Boolean = false,
    val badgeColorRole: AppColorRole? = null,
)

@Composable
fun OverviewInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    hasBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
    valueColorRole: AppColorRole? = null,
    badgeColorRole: AppColorRole? = null,
) {
    SummaryRow(
        icon = icon,
        label = label,
        value = value,
        hasBadge = hasBadge,
        onClick = onClick,
        valueColorRole = valueColorRole,
        badgeColorRole = badgeColorRole,
    )
}

@Composable
fun OverviewAppInfoCard(
    appVersionName: String,
    appVersionCode: String,
    appVersionCodeLabel: String? = null,
    runtimeVersionName: String? = null,
    frameworkType: String? = null,
    frameworkVersion: String? = null,
    interactive: Boolean = true,
    hasUpdateBadge: Boolean = false,
    onVersionClick: (() -> Unit)? = null,
    onRuntimeVersionClick: (() -> Unit)? = null,
    managerVersionHint: String? = null,
    runtimeVersionHint: String? = null,
    onRootHint: (() -> Unit)? = null,
    /** Inline state for the manager row; when set it owns the row's value text, colour and badge. */
    managerValueState: OverviewValueState? = null,
    /** Inline state for the runtime row, same contract as [managerValueState]. */
    runtimeValueState: OverviewValueState? = null,
) {
    SummarySectionCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (runtimeVersionName == null) {
            OverviewInfoItem(
                icon = Icons.AutoMirrored.Filled.Label,
                label = stringResource(id = R.string.uikit_version_name),
                value = managerValueState?.text ?: appVersionName,
                hasBadge = hasUpdateBadge || managerValueState?.badge == true,
                onClick = if (interactive) onVersionClick else null,
                valueColorRole = managerValueState?.colorRole,
                badgeColorRole = managerValueState?.badgeColorRole,
            )
            managerVersionHint?.let { hint -> OverviewRowHint(text = hint) }
        } else {
            OverviewInfoItem(
                icon = Icons.AutoMirrored.Filled.Label,
                label = stringResource(id = R.string.uikit_manager_version),
                value = managerValueState?.text ?: appVersionName,
                hasBadge = hasUpdateBadge || managerValueState?.badge == true,
                onClick = if (interactive) onVersionClick else null,
                valueColorRole = managerValueState?.colorRole,
                badgeColorRole = managerValueState?.badgeColorRole,
            )
            managerVersionHint?.let { hint -> OverviewRowHint(text = hint) }
            OverviewInfoItem(
                icon = Icons.Default.Verified,
                label = stringResource(id = R.string.uikit_runtime_version),
                value = runtimeValueState?.text ?: runtimeVersionName,
                onClick = if (interactive) onRuntimeVersionClick else null,
                hasBadge = runtimeValueState?.badge == true,
                valueColorRole = runtimeValueState?.colorRole,
                badgeColorRole = runtimeValueState?.badgeColorRole,
            )
            runtimeVersionHint?.let { hint -> OverviewRowHint(text = hint) }
        }
        OverviewInfoItem(
            icon = Icons.Default.Numbers,
            label = appVersionCodeLabel ?: stringResource(id = R.string.uikit_version_code),
            value = appVersionCode,
        )
        if (frameworkType != null) {
            OverviewInfoItem(
                icon = Icons.Default.Extension,
                label = stringResource(id = R.string.uikit_framework_type),
                value = frameworkType,
                onClick = if (!interactive) null else onRootHint,
            )
        }
        if (frameworkVersion != null) {
            OverviewInfoItem(
                icon = Icons.Default.Verified,
                label = stringResource(id = R.string.uikit_framework_version),
                value = frameworkVersion,
                onClick = if (!interactive) null else onRootHint,
            )
        }
    }
}

/**
 * Secondary line rendered under an [OverviewInfoItem] row, e.g. an update-check
 * hint ("up to date" / "new version X"). Style-aware so the card reads the same
 * under both the Miuix and Material UI kits.
 */
@Composable
private fun OverviewRowHint(text: String) {
    val isMiuix = LocalUiKitStyle.current == UiKitStyle.Miuix
    Text(
        text = text,
        style = if (isMiuix) {
            MiuixTheme.textStyles.footnote1
        } else {
            MaterialTheme.typography.bodySmall
        },
        color = appColor(AppColorRole.OnSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 16.dp, bottom = 4.dp),
    )
}

@Composable
fun OverviewDeviceInfoCard(
    deviceModel: String = io.github.magisk317.uikit.platform.resolveAndroidDeviceDisplayName(),
) {
    SummarySectionCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        OverviewInfoItem(
            icon = Icons.Default.Android,
            label = stringResource(id = R.string.uikit_android_version),
            value = Build.VERSION.RELEASE,
        )
        OverviewInfoItem(
            icon = Icons.Default.Terminal,
            label = stringResource(id = R.string.uikit_android_codename),
            value = Build.VERSION.CODENAME,
        )
        OverviewInfoItem(
            icon = Icons.Default.Code,
            label = stringResource(id = R.string.uikit_api_level),
            value = Build.VERSION.SDK_INT.toString(),
        )
        OverviewInfoItem(
            icon = Icons.Default.Business,
            label = stringResource(id = R.string.uikit_manufacturer),
            value = Build.MANUFACTURER,
        )
        OverviewInfoItem(
            icon = Icons.Default.Smartphone,
            label = stringResource(id = R.string.uikit_model),
            value = deviceModel,
        )
    }
}

@Composable
fun OverviewLinksCard(
    onJoinTelegram: (() -> Unit)?,
    onSourceCode: (() -> Unit)?,
    onJoinQqChannel: (() -> Unit)? = null,
    interactive: Boolean = true,
) {
    SummarySectionCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        OverviewInfoItem(
            icon = Icons.AutoMirrored.Filled.Send,
            label = stringResource(id = R.string.uikit_pref_join_telegram_group_title),
            value = stringResource(id = R.string.uikit_pref_join_telegram_group_summary),
            onClick = if (interactive) onJoinTelegram else null,
        )
        if (onJoinQqChannel != null) {
            OverviewInfoItem(
                icon = Icons.Default.Groups,
                label = stringResource(id = R.string.uikit_pref_join_qq_channel_title),
                value = stringResource(id = R.string.uikit_pref_join_qq_channel_summary),
                onClick = if (interactive) onJoinQqChannel else null,
            )
        }
        OverviewInfoItem(
            icon = Icons.Default.Code,
            label = stringResource(id = R.string.uikit_pref_source_code_title),
            value = stringResource(id = R.string.uikit_pref_source_code_summary),
            onClick = if (interactive) onSourceCode else null,
        )
    }
}

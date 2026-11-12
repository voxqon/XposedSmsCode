package io.github.magisk317.uikit.preference

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.magisk317.uikit.R

data class RuntimeLogDiagnosticsLabels(
    val retentionTitle: String,
    val retentionSummary: String,
    val clearLogTitle: String,
    val clearLogSummary: String,
    val shareLogTitle: String? = null,
    val shareLogSummary: String = "",
)

data class RuntimeLogDiagnosticsState(
    val analyticsEnabled: Boolean = false,
)

data class RuntimeLogDiagnosticsCallbacks(
    val onShareLog: () -> Unit,
    val onRetentionClick: () -> Unit,
    val onClearLogClick: () -> Unit,
    val onAnalyticsEnabledChange: ((Boolean) -> Unit)? = null,
)

enum class RuntimeLogDiagnosticsItem {
    SHARE_LOG,
    RETENTION,
    CLEAR_LOG,
    ANALYTICS,
}

enum class RuntimeLogShareEntryMode {
    SEPARATE_ITEM,
    VERBOSE_TITLE,
    VERBOSE_ROW,
}

data class RuntimeLogDiagnosticsLayout(
    val shareEntryMode: RuntimeLogShareEntryMode = RuntimeLogShareEntryMode.VERBOSE_TITLE,
    val itemOrder: List<RuntimeLogDiagnosticsItem> = RuntimeLogDiagnosticsItem.entries,
)

/**
 * Stateless preference rows shared by apps that expose runtime-log diagnostics.
 * Storage, export, cleanup, and feedback remain owned by the embedding app.
 *
 * When [enabled] is false (release builds) every log-related row is hidden and the
 * share-log entry degrades into a non-interactive placeholder that points at CI
 * builds; only the analytics toggle stays fully available.
 */
@Composable
fun RuntimeLogDiagnosticsItems(
    labels: RuntimeLogDiagnosticsLabels,
    state: RuntimeLogDiagnosticsState,
    callbacks: RuntimeLogDiagnosticsCallbacks,
    layout: RuntimeLogDiagnosticsLayout = RuntimeLogDiagnosticsLayout(),
    enabled: Boolean = true,
) {
    layout.itemOrder.distinct().forEach { item ->
        when (item) {
            RuntimeLogDiagnosticsItem.SHARE_LOG -> if (enabled) {
                if (layout.shareEntryMode == RuntimeLogShareEntryMode.SEPARATE_ITEM) {
                    labels.shareLogTitle?.let { title ->
                        AppArrowItem(
                            title = title,
                            summary = labels.shareLogSummary,
                            onClick = callbacks.onShareLog,
                        )
                    }
                }
            } else {
                AppArrowItem(
                    title = stringResource(id = R.string.uikit_log_export_placeholder_title),
                    summary = stringResource(id = R.string.uikit_log_export_placeholder_summary),
                    enabled = false,
                    onClick = {},
                )
            }


            RuntimeLogDiagnosticsItem.RETENTION -> if (enabled) AppArrowItem(
                title = labels.retentionTitle,
                summary = labels.retentionSummary,
                onClick = callbacks.onRetentionClick,
            )

            RuntimeLogDiagnosticsItem.CLEAR_LOG -> if (enabled) AppArrowItem(
                title = labels.clearLogTitle,
                summary = labels.clearLogSummary,
                onClick = callbacks.onClearLogClick,
            )

            RuntimeLogDiagnosticsItem.ANALYTICS -> {
                if (callbacks.onAnalyticsEnabledChange != null) {
                    StateSwitchItem(
                        title = stringResource(id = R.string.pref_enable_analytics_title),
                        summary = stringResource(id = R.string.pref_enable_analytics_summary),
                        checked = state.analyticsEnabled,
                        onCheckedChange = callbacks.onAnalyticsEnabledChange,
                    )
                }
            }
        }
    }
}



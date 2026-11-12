package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState as rememberMaterialPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.foundation.LoadingIndicatorTokens
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState as rememberMiuixPullToRefreshState

/**
 * Shared pull-to-refresh surface. The caller owns refresh state and data loading;
 * only the gesture and indicator implementation changes with the selected UI style.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            pullToRefreshState = rememberMiuixPullToRefreshState(),
            refreshTexts = with(LocalContext.current) {
                listOf(
                    getString(io.github.magisk317.uikit.R.string.pull_to_refresh_pull),
                    getString(io.github.magisk317.uikit.R.string.pull_to_refresh_release),
                    getString(io.github.magisk317.uikit.R.string.pull_to_refresh_refreshing),
                    getString(io.github.magisk317.uikit.R.string.pull_to_refresh_done),
                )
            },
            contentPadding = contentPadding,
            content = { content() },
        )
        return
    }

    val state = rememberMaterialPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = contentPadding.calculateTopPadding() + LoadingIndicatorTokens.OverlayTopSpacing,
                    ),
                isRefreshing = isRefreshing,
                state = state,
            )
        },
        content = { content() },
    )
}

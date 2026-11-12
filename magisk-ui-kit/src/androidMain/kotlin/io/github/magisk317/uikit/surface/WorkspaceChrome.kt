package io.github.magisk317.uikit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.windowInsetsPadding
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.LocalUiKitSurfaceBlur
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.spacing
import top.yukonga.miuix.kmp.basic.ScrollBehavior as MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

@Composable
fun SearchWorkspaceScaffold(
    fallbackTopPadding: Dp,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    overlayModifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    searchField: @Composable ColumnScope.() -> Unit,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    OverlayHeaderScaffold(
        modifier = modifier,
        fallbackTopPadding = fallbackTopPadding,
        bottomPadding = bottomPadding,
        overlayModifier = overlayModifier,
        overlay = {
            OverlayHeaderPanel(
                title = title,
                subtitle = subtitle,
                actions = actions,
            ) {
                searchField()
                supportingContent()
            }
        },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTopBarSearchOverlay(
    title: String,
    searchQuery: String,
    searchPlaceholder: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    // When true the caller actions render after the search icon (search leftmost,
    // actions rightmost) — for pages whose trailing action is the settings entry.
    actionsAfterSearch: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    miuixScrollBehavior: MiuixScrollBehavior? = null,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    searchVisible: Boolean = true,
    searchActionContentDescription: String? = null,
    onSearchActionClick: (() -> Unit)? = null,
    glassTopBar: UiKitGlassTopBarScope? = null,
    preSearchContent: @Composable ColumnScope.() -> Unit = {},
    supportingContent: @Composable ColumnScope.() -> Unit = {},
    onSearchChange: (String) -> Unit,
) {
    val glassOn = glassTopBar != null && LocalUiKitSurfaceBlur.current.usesBackdrop
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixWorkspaceTopBarSearchOverlay(
            title = title,
            searchQuery = searchQuery,
            searchPlaceholder = searchPlaceholder,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            actionsAfterSearch = actionsAfterSearch,
            miuixScrollBehavior = miuixScrollBehavior,
            searchVisible = searchVisible,
            searchActionContentDescription = searchActionContentDescription,
            onSearchActionClick = onSearchActionClick,
            glassTopBar = glassTopBar,
            preSearchContent = preSearchContent,
            supportingContent = supportingContent,
            onSearchChange = onSearchChange,
        )
        return
    }

    ColumnScopeInstance(modifier) {
        if (searchVisible) {
            WorkspaceExpandedSearchRow(
                searchQuery = searchQuery,
                searchPlaceholder = searchPlaceholder,
                windowInsets = windowInsets,
                onClose = onSearchActionClick ?: {},
                onSearchChange = onSearchChange,
            )
        } else {
            TopAppBar(
                modifier = if (glassOn) Modifier.uiKitSurfaceGlassSample(glassTopBar) else Modifier,
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    navigationIcon?.invoke()
                },
                actions = {
                    if (actionsAfterSearch && onSearchActionClick != null) {
                        WorkspaceSearchAction(
                            active = false,
                            contentDescription = searchActionContentDescription,
                            onClick = onSearchActionClick,
                        )
                    }
                    actions()
                    if (!actionsAfterSearch && onSearchActionClick != null) {
                        WorkspaceSearchAction(
                            active = false,
                            contentDescription = searchActionContentDescription,
                            onClick = onSearchActionClick,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                windowInsets = windowInsets,
                colors = if (glassOn) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    )
                } else {
                    chromeTopAppBarColors()
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chromeSurfaceColor()),
            ) {
                preSearchContent()
                supportingContent()
            }
        }
    }
}

@Composable
private fun WorkspaceExpandedSearchRow(
    searchQuery: String,
    searchPlaceholder: String,
    windowInsets: WindowInsets,
    onClose: () -> Unit,
    onSearchChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(chromeSurfaceColor())
            .windowInsetsPadding(windowInsets)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WorkspaceSearchField(
            query = searchQuery,
            placeholder = searchPlaceholder,
            modifier = Modifier.weight(1f),
            autoFocus = true,
            onValueChange = onSearchChange,
        )
    }
}

@Composable
private fun ColumnScopeInstance(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun MiuixWorkspaceTopBarSearchOverlay(
    title: String,
    searchQuery: String,
    searchPlaceholder: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    actionsAfterSearch: Boolean = false,
    miuixScrollBehavior: MiuixScrollBehavior? = null,
    searchVisible: Boolean = true,
    searchActionContentDescription: String? = null,
    onSearchActionClick: (() -> Unit)? = null,
    glassTopBar: UiKitGlassTopBarScope? = null,
    preSearchContent: @Composable ColumnScope.() -> Unit = {},
    supportingContent: @Composable ColumnScope.() -> Unit = {},
    onSearchChange: (String) -> Unit,
) {
    val glassOn = glassTopBar != null && LocalUiKitSurfaceBlur.current.usesBackdrop
    ColumnScopeInstance(modifier) {
        if (searchVisible) {
            WorkspaceExpandedSearchRow(
                searchQuery = searchQuery,
                searchPlaceholder = searchPlaceholder,
                windowInsets = WindowInsets.statusBars,
                onClose = onSearchActionClick ?: {},
                onSearchChange = onSearchChange,
            )
        } else {
            SmallTopAppBar(
                modifier = if (glassOn) Modifier.uiKitSurfaceGlassSample(glassTopBar) else Modifier,
                title = title,
                navigationIcon = { navigationIcon?.invoke() },
                actions = {
                    if (actionsAfterSearch && onSearchActionClick != null) {
                        WorkspaceSearchAction(
                            active = false,
                            contentDescription = searchActionContentDescription,
                            onClick = onSearchActionClick,
                        )
                    }
                    actions()
                    if (!actionsAfterSearch && onSearchActionClick != null) {
                        WorkspaceSearchAction(
                            active = false,
                            contentDescription = searchActionContentDescription,
                            onClick = onSearchActionClick,
                        )
                    }
                },
                scrollBehavior = miuixScrollBehavior,
                color = if (glassOn) Color.Transparent else chromeSurfaceColor(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chromeSurfaceColor()),
            ) {
                preSearchContent()
                supportingContent()
            }
        }
    }
}

@Composable
fun WorkspaceSearchAction(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = contentDescription,
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

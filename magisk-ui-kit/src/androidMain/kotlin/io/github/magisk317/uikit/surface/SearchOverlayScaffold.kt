package io.github.magisk317.uikit.surface

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import top.yukonga.miuix.kmp.basic.ScrollBehavior as MiuixScrollBehavior

/**
 * State holder for a togglable search overlay.
 *
 * Manages expanded/collapsed state and the current search query.
 * The query updates in real time as the user type; callers should
 * react to [query] changes to filter their data.
 */
@Stable
class SearchOverlayState(
    initialQuery: String,
    private val onSearchChange: (String) -> Unit,
) {
    var expanded by mutableStateOf(initialQuery.isNotBlank())
        private set

    var query by mutableStateOf(initialQuery)
        private set

    fun updateQuery(value: String) {
        query = value
        onSearchChange(value)
    }

    /** Toggle between expanded (searching) and collapsed (normal top bar). */
    fun toggle() {
        if (expanded) close() else expanded = true
    }

    /** Collapse the search overlay and clear the query. */
    fun close() {
        expanded = false
        if (query.isNotEmpty()) {
            query = ""
            onSearchChange("")
        }
    }
}

/**
 * Create and remember a [SearchOverlayState].
 *
 * @param initialQuery pre-filled search query (e.g. from a saved state).
 *   If non-blank the overlay starts expanded.
 * @param onSearchChange called in real time as the user types.
 */
@Composable
fun rememberSearchOverlayState(
    initialQuery: String = "",
    onSearchChange: (String) -> Unit = {},
): SearchOverlayState {
    // Persist only the primitives (query + expanded) — lambdas cannot
    // be saved in a Bundle.
    val savedQuery = rememberSaveable { mutableStateOf(initialQuery) }
    val callbackRef = remember { mutableStateOf(onSearchChange) }
    callbackRef.value = onSearchChange
    val stableCallback: (String) -> Unit = { callbackRef.value(it) }
    return remember(savedQuery.value) {
        SearchOverlayState(savedQuery.value, stableCallback).also {
            // Sync back so the state holder stays consistent across
            // recompositions that update savedQuery (e.g. config change).
            if (it.query != savedQuery.value) it.updateQuery(savedQuery.value)
        }
    }
}

/**
 * Drop-in replacement for [WorkspaceTopBarSearchOverlay] with built-in
 * [SearchOverlayState] management.
 *
 * Use inside [OverlayHeaderScaffold]'s `overlay` lambda:
 * ```kotlin
 * val searchState = rememberSearchOverlayState(onSearchChange = { viewModel.doFilter(it) })
 * BackHandler(enabled = searchState.expanded) { searchState.close() }
 * LaunchedEffect(isActive) { if (!isActive) searchState.close() }
 *
 * OverlayHeaderScaffold(
 *     fallbackTopPadding = if (searchState.expanded) expandedTop else collapsedTop,
 *     overlay = {
 *         SearchOverlayContent(
 *             state = searchState,
 *             title = "...",
 *             searchPlaceholder = "...",
 *             glassTopBar = topGlass, // optional: frost the collapsed bar over recorded content
 *             actions = { /* settings button etc. */ },
 *         )
 *     },
 *     content = { /* list */ },
 * )
 *
 * When `glassTopBar` is supplied the collapsed bar samples the page content
 * recorded via `topGlass.contentRecorder()`; the expanded search row stays
 * opaque on purpose (mirrors the workspace app-list pages).
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlayContent(
    state: SearchOverlayState,
    title: String,
    searchPlaceholder: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    // When true the caller actions render after the search icon (search leftmost,
    // actions rightmost) — for pages whose trailing action is the settings entry.
    actionsAfterSearch: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    miuixScrollBehavior: MiuixScrollBehavior? = null,
    glassTopBar: UiKitGlassTopBarScope? = null,
    preSearchContent: @Composable ColumnScope.() -> Unit = {},
    supportingContent: @Composable ColumnScope.() -> Unit = {},
) {
    WorkspaceTopBarSearchOverlay(
        title = title,
        searchQuery = state.query,
        searchPlaceholder = searchPlaceholder,
        searchVisible = state.expanded,
        searchActionContentDescription = searchPlaceholder,
        onSearchActionClick = { state.toggle() },
        navigationIcon = navigationIcon,
        actions = actions,
        actionsAfterSearch = actionsAfterSearch,
        scrollBehavior = scrollBehavior,
        miuixScrollBehavior = miuixScrollBehavior,
        glassTopBar = glassTopBar,
        preSearchContent = preSearchContent,
        supportingContent = supportingContent,
        onSearchChange = { state.updateQuery(it) },
    )
}

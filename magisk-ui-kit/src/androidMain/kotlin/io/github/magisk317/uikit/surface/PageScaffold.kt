@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import io.github.magisk317.uikit.theme.AppColorRole
import io.github.magisk317.uikit.theme.LocalUiKitSurfaceBlur
import io.github.magisk317.uikit.theme.appColor
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * Secondary-page chrome pair (Architecture A): the single source for
 * `PageScaffoldMiuix` / `PageScaffoldExpressive`. Consumer pages own their state
 * and their body lambda and dispatch on `LocalUiKitStyle` themselves, so both
 * variants stay public.
 *
 * `content` receives the resolved list padding and the style-owned scroll
 * modifier that the page body must apply to its scrollable container
 * (end-of-scroll haptics + nested-scroll connection on Miuix, nested-scroll
 * connection on Expressive).
 *
 * Container colours go through [chromeSurfaceColor], the kit-wide chrome
 * surface (opaque `MiuixTheme.colorScheme.surface` on Miuix,
 * `surfaceContainerHigh` at alpha on Expressive).
 */

/**
 * Miuix variant: page-owned miuix `Scaffold` + collapsing `TopAppBar` driven by
 * `MiuixScrollBehavior`, mirroring the KernelSU `HomeMiuix` model.
 *
 * `overScrollVertical()` + `overscrollEffect = null` are intentionally omitted:
 * the global `MiuixOverscrollFactory` under `MagiskUiKitTheme` already provides
 * the bounce (copying KernelSU verbatim would stack a second overscroll).
 */
@Composable
fun PageScaffoldMiuix(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (listPadding: PaddingValues, scrollModifier: Modifier) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val topGlass = rememberUiKitGlassTopBar()
    val glassOn = LocalUiKitSurfaceBlur.current.usesBackdrop
    MiuixScaffold(
        topBar = {
            val contentColor = appColor(AppColorRole.OnSurface)
            val wrappedActions: @Composable RowScope.() -> Unit = {
                androidx.compose.runtime.CompositionLocalProvider(
                    top.yukonga.miuix.kmp.theme.LocalContentColor provides contentColor,
                    androidx.compose.material3.LocalContentColor provides contentColor,
                ) {
                    actions()
                }
            }
            TopAppBar(
                modifier = Modifier.uiKitSurfaceGlassSample(topGlass),
                title = title,
                navigationIcon = {
                    onBack?.let { callback ->
                        androidx.compose.runtime.CompositionLocalProvider(
                            top.yukonga.miuix.kmp.theme.LocalContentColor provides contentColor,
                            androidx.compose.material3.LocalContentColor provides contentColor,
                        ) {
                            AppIconButton(onClick = callback) {
                                AppIcon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                },
                actions = wrappedActions,
                scrollBehavior = scrollBehavior,
                color = if (glassOn) Color.Transparent else chromeSurfaceColor(),
                defaultWindowInsetsPadding = true,
            )
        },
        contentWindowInsets = WindowInsets.systemBars
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().then(if (glassOn) topGlass.contentRecorder() else Modifier).nestedScroll(scrollBehavior.nestedScrollConnection)) {
            content(
                PaddingValues(top = innerPadding.calculateTopPadding()),
                Modifier.scrollEndHaptic(),
            )
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                floatingActionButton()
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                snackbarHost()
            }
        }
    }
}

/**
 * Expressive/Material variant: material3 `Scaffold` + collapsing top app bar
 * driven by `exitUntilCollapsedScrollBehavior` (the Overview/KernelSU
 * `HomeMaterial` template). The bar itself goes through [AppTopBar] (which, on
 * this guaranteed path, is the material3 bar plus chrome colors and surface
 * blur).
 */
@Composable
fun PageScaffoldExpressive(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (listPadding: PaddingValues, scrollModifier: Modifier) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val topGlass = rememberUiKitGlassTopBar()
    val glassOn = LocalUiKitSurfaceBlur.current.usesBackdrop
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                modifier = Modifier.uiKitSurfaceGlassSample(topGlass),
                containerColor = if (glassOn) Color.Transparent else chromeSurfaceColor(),
                title = title,
                navigationIcon = {
                    onBack?.let { callback ->
                        AppIconButton(onClick = callback) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.statusBars,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().then(if (glassOn) topGlass.contentRecorder() else Modifier)) {
            content(
                PaddingValues(top = innerPadding.calculateTopPadding()),
                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            )
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                floatingActionButton()
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                snackbarHost()
            }
        }
    }
}

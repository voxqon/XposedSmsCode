package io.github.magisk317.uikit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge as MaterialBadge
import androidx.compose.material3.BadgedBox as MaterialBadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.theme.AppColorRole
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.appColor
import top.yukonga.miuix.kmp.basic.Badge as MiuixBadge
import top.yukonga.miuix.kmp.basic.BadgedBox as MiuixBadgedBox
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar as MiuixFloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem as MiuixFloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.ScrollBehavior as MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

data class AppNavigationItemSpec(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
    val testTag: String? = null,
    val badgeLabel: String? = null,
)

private fun Modifier.staticChromeBackdrop(
    backdrop: Backdrop?,
    shape: RoundedCornerShape,
): Modifier = if (backdrop == null) {
    this
} else {
    drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            blur(
                radiusX = 18.dp.toPx(),
                radiusY = 18.dp.toPx(),
            )
        },
    )
}

@Composable
fun AppBottomNavigationBar(
    items: List<AppNavigationItemSpec>,
    modifier: Modifier = Modifier,
    alwaysShowLabel: Boolean = false,
    containerColor: Color = Color.Transparent,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixNavigationBar(
            modifier = modifier,
            color = containerColor,
            mode = if (alwaysShowLabel) {
                NavigationBarDisplayMode.IconAndText
            } else {
                NavigationBarDisplayMode.IconWithSelectedLabel
            },
        ) {
            items.forEach { item ->
                MiuixNavigationBarItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = item.icon,
                    label = item.label,
                    modifier = item.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                    badge = item.badgeLabel?.let { label ->
                        {
                            MiuixBadge(content = { MiuixText(label) })
                        }
                    },
                )
            }
        }
        return
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
    )
    NavigationBar(
        modifier = modifier.navigationBarsPadding(),
        containerColor = containerColor,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                modifier = item.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                icon = {
                    val badgeLabel = item.badgeLabel
                    if (badgeLabel.isNullOrBlank()) {
                        Icon(item.icon, contentDescription = item.label)
                    } else {
                        MaterialBadgedBox(
                            badge = { MaterialBadge { Text(badgeLabel) } },
                        ) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    }
                },
                label = { Text(item.label) },
                selected = item.selected,
                colors = navItemColors,
                alwaysShowLabel = alwaysShowLabel,
                onClick = item.onClick,
            )
        }
    }
}

@Composable
fun AppFloatingNavigationBar(
    items: List<AppNavigationItemSpec>,
    modifier: Modifier = Modifier,
    alwaysShowLabel: Boolean = false,
    blurred: Boolean = false,
    backdrop: Backdrop? = null,
    onDragProgress: ((fractionalIndex: Float) -> Unit)? = null,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        // KernelSU-grade liquid glass pill: refraction lens over the live screen backdrop,
        // gravity specular highlight, damped drag indicator (replaces the earlier HyperIsland
        // rounded-rect floating bar, which read small next to stock MIUI chrome).
        io.github.magisk317.uikit.glass.AppLiquidGlassNavigationBar(
            items = items,
            modifier = modifier,
            alwaysShowLabel = alwaysShowLabel,
            blurred = blurred,
            backdrop = backdrop,
            onDragProgress = onDragProgress,
        )
        return
    }
    val floatingShape = RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .staticChromeBackdrop(backdrop, floatingShape),
        shape = floatingShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (blurred) 0.86f else 1f),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    icon = {
                        val badgeLabel = item.badgeLabel
                        if (badgeLabel.isNullOrBlank()) {
                            Icon(item.icon, contentDescription = item.label)
                        } else {
                            MaterialBadgedBox(
                                badge = { MaterialBadge { Text(badgeLabel) } },
                            ) {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        }
                    },
                    label = { Text(item.label) },
                    selected = item.selected,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    ),
                    alwaysShowLabel = alwaysShowLabel,
                    onClick = item.onClick,
                )
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    items: List<AppNavigationItemSpec>,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    alwaysShowLabel: Boolean = false,
) {
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        val railState = rememberNavigationRailState(
            if (alwaysShowLabel) NavigationRailValue.Expanded else NavigationRailValue.Collapsed,
        )
        MiuixNavigationRail(
            modifier = modifier.fillMaxHeight(),
            state = railState,
            header = header,
        ) {
            items.forEach { item ->
                val badgeLabel = item.badgeLabel
                if (badgeLabel.isNullOrBlank()) {
                    MiuixNavigationRailItem(
                        selected = item.selected,
                        onClick = item.onClick,
                        icon = item.icon,
                        label = item.label,
                        modifier = item.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                    )
                } else {
                    MiuixBadgedBox(
                        badge = {
                            MiuixBadge(content = { MiuixText(badgeLabel) })
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MiuixNavigationRailItem(
                            selected = item.selected,
                            onClick = item.onClick,
                            icon = item.icon,
                            label = item.label,
                            modifier = item.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                        )
                    }
                }
            }
        }
        return
    }

    val railItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
    )
    NavigationRail(
        header = header,
        modifier = modifier.fillMaxHeight(),
    ) {
        items.forEach { item ->
            NavigationRailItem(
                modifier = item.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                icon = {
                    val badgeLabel = item.badgeLabel
                    if (badgeLabel.isNullOrBlank()) {
                        Icon(item.icon, contentDescription = item.label)
                    } else {
                        MaterialBadgedBox(
                            badge = { MaterialBadge { Text(badgeLabel) } },
                        ) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    }
                },
                label = { Text(item.label) },
                selected = item.selected,
                colors = railItemColors,
                alwaysShowLabel = alwaysShowLabel,
                onClick = item.onClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    miuixScrollBehavior: MiuixScrollBehavior? = null,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    containerColor: Color = Color.Transparent,
    scrolledContainerColor: Color = Color.Transparent,
) {
    val contentColor = appColor(AppColorRole.OnSurface)
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        val wrappedActions: @Composable RowScope.() -> Unit = {
            androidx.compose.runtime.CompositionLocalProvider(
                top.yukonga.miuix.kmp.theme.LocalContentColor provides contentColor,
                androidx.compose.material3.LocalContentColor provides contentColor,
            ) {
                actions()
            }
        }
        SmallTopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = {
                if (navigationIcon != null) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        top.yukonga.miuix.kmp.theme.LocalContentColor provides contentColor,
                        androidx.compose.material3.LocalContentColor provides contentColor,
                    ) {
                        navigationIcon()
                    }
                }
            },
            actions = wrappedActions,
            scrollBehavior = miuixScrollBehavior,
            color = containerColor,
            defaultWindowInsetsPadding = true,
        )
        return
    }

    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        scrollBehavior = scrollBehavior,
        windowInsets = windowInsets,
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = scrolledContainerColor,
            actionIconContentColor = contentColor,
            navigationIconContentColor = contentColor,
        ),
    )
}

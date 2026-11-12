package io.github.magisk317.uikit.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.foundation.PolygonMorphLoadingIndicator
import io.github.magisk317.uikit.preference.TextInputDialog
import io.github.magisk317.uikit.surface.AppBottomSheet
import io.github.magisk317.uikit.surface.ExpressiveHeroCard
import io.github.magisk317.uikit.surface.ExpressiveSectionCard
import io.github.magisk317.uikit.surface.MainTabScaffold
import io.github.magisk317.uikit.surface.MainTabSpec
import io.github.magisk317.uikit.surface.rememberMainChromeController
import io.github.magisk317.uikit.theme.MagiskThemeMode
import io.github.magisk317.uikit.theme.MagiskUiKitTheme
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.spacing

/**
 * Android-only chrome preview: the parts of the kit sample that depend on system bars,
 * blur backdrops and the tab scaffold live here (androidMain). The shared sample and the
 * [MagiskMultiPreview] annotation live in commonMain so the same annotation serves every
 * target, including the desktop module.
 */

@Composable
private fun AndroidChromeSample() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        ExpressiveHeroCard(
            title = "Hero card",
            subtitle = "Magisk UI kit preview",
        ) {
            Text(
                text = "Content slot",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ExpressiveSectionCard(
            title = "Section card",
            summary = "Section summary text",
        ) {
            Text(
                text = "Section body",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        PolygonMorphLoadingIndicator()
        TextInputDialog(
            title = "Preview input",
            initialValue = "Existing value",
            selectAllOnOpen = true,
            onDismiss = {},
            onConfirm = {},
        )
        AppBottomSheet(show = true, onDismissRequest = {}, title = "Bottom sheet") {
            Text("Sheet content", modifier = Modifier.padding(16.dp))
        }
        val chromeController = rememberMainChromeController(
            isCompact = true,
            compactChromeRouteAvailable = true,
            keepVisible = true,
            allowScrollHide = false,
            resetKey = Unit,
        )
        MainTabScaffold(
            tabs = listOf(MainTabSpec("Home", Icons.Filled.Home)),
            selectedIndex = 0,
            isCompact = true,
            chromeController = chromeController,
            onTabSelected = {},
            onTabReselected = {},
        ) { padding ->
            Text("Main tab content", modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun AndroidThemedPreview(mode: MagiskThemeMode, style: UiKitStyle, content: @Composable () -> Unit) {
    MagiskUiKitTheme(
        themeMode = mode.value,
        dynamicColor = false,
        uiKitStyle = style,
        content = content,
    )
}

@Preview(name = "Light · Expressive chrome", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AndroidPreviewLightExpressive() = AndroidThemedPreview(MagiskThemeMode.Light, UiKitStyle.Expressive) { AndroidChromeSample() }

@Preview(name = "Dark · Expressive chrome", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AndroidPreviewDarkExpressive() = AndroidThemedPreview(MagiskThemeMode.Dark, UiKitStyle.Expressive) { AndroidChromeSample() }

@Preview(name = "PureBlack · Expressive chrome", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AndroidPreviewPureBlackExpressive() = AndroidThemedPreview(MagiskThemeMode.PureBlack, UiKitStyle.Expressive) { AndroidChromeSample() }

@Preview(name = "Light · Miuix chrome", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AndroidPreviewLightMiuix() = AndroidThemedPreview(MagiskThemeMode.Light, UiKitStyle.Miuix) { AndroidChromeSample() }

@Preview(name = "Dark · Miuix chrome", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AndroidPreviewDarkMiuix() = AndroidThemedPreview(MagiskThemeMode.Dark, UiKitStyle.Miuix) { AndroidChromeSample() }

@Preview(name = "PureBlack · Miuix chrome", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AndroidPreviewPureBlackMiuix() = AndroidThemedPreview(MagiskThemeMode.PureBlack, UiKitStyle.Miuix) { AndroidChromeSample() }

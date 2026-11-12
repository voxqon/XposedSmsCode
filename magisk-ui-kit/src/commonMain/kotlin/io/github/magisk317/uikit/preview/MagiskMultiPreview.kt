package io.github.magisk317.uikit.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import io.github.magisk317.uikit.preference.SectionHeader
import io.github.magisk317.uikit.surface.AppCard
import io.github.magisk317.uikit.surface.AppPrimaryButton
import io.github.magisk317.uikit.surface.AppSecondaryButton
import io.github.magisk317.uikit.surface.AppSurface
import io.github.magisk317.uikit.text.AppText
import io.github.magisk317.uikit.text.AppTextRole
import io.github.magisk317.uikit.theme.MagiskThemeMode
import io.github.magisk317.uikit.theme.MagiskUiKitTheme
import io.github.magisk317.uikit.theme.UiKitStyle
import io.github.magisk317.uikit.theme.spacing

/**
 * Shared preview package for the UI kit.
 *
 * Compose's `@PreviewWrapper` lets this annotation be used by downstream previews; the six
 * meta previews fan out one mode x style combination. Each delegates to [MagiskUiKitTheme]
 * with `dynamicColor = false` so the preview is stable across devices, then renders a small
 * sample surface ([PreviewSample]) that exercises the shared App* component contracts in the
 * active track.
 *
 * The six variants:
 *  - Light / Expressive
 *  - Dark / Expressive
 *  - PureBlack / Expressive
 *  - Light / Miuix
 *  - Dark / Miuix
 *  - PureBlack / Miuix
 *
 * `System` mode is intentionally omitted from previews: it would just resolve to whichever
 * `uiMode` the host process runs in, duplicating Light or Dark.
 *
 * The sample only calls commonMain components so the previews render on every target,
 * including the Compose Multiplatform desktop module. Android-only chrome (system bars,
 * blur, tab scaffolds) keeps its own sample in `AndroidChromePreview.kt` under androidMain.
 */

@Composable
private fun PreviewSample() {
    AppSurface {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            SectionHeader(text = "UI kit sample")
            AppCard {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                    AppText(text = "Card title", role = AppTextRole.Title)
                    AppText(
                        text = "Body copy rendered in the active track typography.",
                        role = AppTextRole.Body,
                    )
                }
            }
            AppPrimaryButton(text = "Primary action", onClick = {})
            AppSecondaryButton(text = "Secondary action", onClick = {})
        }
    }
}

private class MagiskMultiPreviewProvider : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) = content()
}

@PreviewWrapper(wrapper = MagiskMultiPreviewProvider::class)
@Preview(name = "Light · Expressive", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "Dark · Expressive", showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "PureBlack · Expressive", showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "Light · Miuix", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "Dark · Miuix", showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "PureBlack · Miuix", showBackground = true, backgroundColor = 0xFF000000)
annotation class MagiskMultiPreview

@Composable
private fun ThemedPreview(mode: MagiskThemeMode, style: UiKitStyle, content: @Composable () -> Unit) {
    MagiskUiKitTheme(
        themeMode = mode.value,
        dynamicColor = false,
        uiKitStyle = style,
        content = content,
    )
}

@Preview(name = "Light · Expressive", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewLightExpressive() = ThemedPreview(MagiskThemeMode.Light, UiKitStyle.Expressive) { PreviewSample() }

@Preview(name = "Dark · Expressive", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewDarkExpressive() = ThemedPreview(MagiskThemeMode.Dark, UiKitStyle.Expressive) { PreviewSample() }

@Preview(name = "PureBlack · Expressive", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewPureBlackExpressive() = ThemedPreview(MagiskThemeMode.PureBlack, UiKitStyle.Expressive) { PreviewSample() }

@Preview(name = "Light · Miuix", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewLightMiuix() = ThemedPreview(MagiskThemeMode.Light, UiKitStyle.Miuix) { PreviewSample() }

@Preview(name = "Dark · Miuix", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewDarkMiuix() = ThemedPreview(MagiskThemeMode.Dark, UiKitStyle.Miuix) { PreviewSample() }

@Preview(name = "PureBlack · Miuix", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewPureBlackMiuix() = ThemedPreview(MagiskThemeMode.PureBlack, UiKitStyle.Miuix) { PreviewSample() }

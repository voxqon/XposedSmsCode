package io.github.magisk317.uikit.preference

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.magisk317.uikit.R

/**
 * Per-app locale storage owned by the UI Kit. On T+ this uses the platform
 * LocaleManager (persisted by the system itself); below T it is a no-op,
 * matching the behavior hosts already shipped.
 */
object UiKitLocale {
    fun currentTag(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context
                .getSystemService(android.app.LocaleManager::class.java)
                ?.applicationLocales
            if (locales == null || locales.isEmpty) "" else locales.get(0)?.toLanguageTag().orEmpty()
        } else {
            ""
        }

    fun apply(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val manager = context.getSystemService(android.app.LocaleManager::class.java) ?: return
        manager.applicationLocales = if (tag.isEmpty()) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
    }
}

/**
 * Unified general-settings card shared by all UI Kit hosts: master switch
 * first, then host-specific rows through [extraContent], the theme chooser
 * (tapped position is reported so the host can run the reveal animation from
 * the click point), and the language chooser below the theme. The theme and
 * language dialogs and the language persistence live here, so hosts only wire
 * their own state and storage.
 */
@Composable
fun GeneralSettingsSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    accordionMode: Boolean,
    moduleEnabled: Boolean,
    onModuleEnabledChange: (Boolean) -> Unit,
    themeMode: Int?,
    onThemeSelected: ((index: Int, x: Float, y: Float) -> Unit)? = null,
    moduleTitle: String = stringResource(R.string.uikit_module_enable_title),
    moduleSummary: String = stringResource(R.string.uikit_module_enable_summary),
    onLanguageSelected: ((String) -> Unit)? = null,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val themeOptions = listOf(
        stringResource(R.string.uikit_theme_follow_system),
        stringResource(R.string.uikit_theme_light),
        stringResource(R.string.uikit_theme_dark),
        stringResource(R.string.uikit_theme_black),
    )
    var showThemeDialog by remember { mutableStateOf(false) }
    var themeDialogSelected by remember { mutableIntStateOf(themeMode ?: 0) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    // Applying a locale recreates the activity, so a plain remember is enough.
    val currentLanguageTag = remember { UiKitLocale.currentTag(context) }
    val languageOptions = listOf(
        stringResource(R.string.uikit_language_follow_system) to "",
        stringResource(R.string.uikit_language_en) to "en",
        stringResource(R.string.uikit_language_zh_cn) to "zh-CN",
        stringResource(R.string.uikit_language_zh_tw) to "zh-TW",
    )

    SectionCard(
        title = title,
        summary = summary,
        accordionMode = accordionMode,
        sectionExpanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        StateSwitchItem(
            title = moduleTitle,
            summary = moduleSummary,
            checked = moduleEnabled,
            onCheckedChange = onModuleEnabledChange,
        )
        extraContent?.invoke(this)
        if (themeMode != null) {
            AppArrowItem(
                title = stringResource(R.string.uikit_theme_title),
                summary = stringResource(
                    id = when (themeMode) {
                        1 -> R.string.uikit_theme_light
                        2 -> R.string.uikit_theme_dark
                        3 -> R.string.uikit_theme_black
                        else -> R.string.uikit_theme_follow_system
                    },
                ),
                onClick = {
                    themeDialogSelected = themeMode
                    showThemeDialog = true
                },
            )
        }
        AppArrowItem(
            title = stringResource(R.string.uikit_language_title),
            summary = stringResource(
                id = when {
                    currentLanguageTag.startsWith("zh-CN") -> R.string.uikit_language_zh_cn
                    currentLanguageTag.startsWith("zh-TW") -> R.string.uikit_language_zh_tw
                    currentLanguageTag.startsWith("en") -> R.string.uikit_language_en
                    else -> R.string.uikit_language_follow_system
                },
            ),
            onClick = { showLanguageDialog = true },
        )
    }

    if (showThemeDialog && themeMode != null) {
        SingleChoicePositionDialog(
            title = stringResource(R.string.uikit_theme_title),
            options = themeOptions,
            selectedIndex = themeDialogSelected.coerceIn(themeOptions.indices),
            onDismissRequest = { showThemeDialog = false },
            onSelectionChange = { index, position ->
                themeDialogSelected = index
                onThemeSelected?.invoke(index, position.x, position.y)
            },
        )
    }

    if (showLanguageDialog) {
        SingleChoiceOptionDialog(
            title = stringResource(R.string.uikit_language_title),
            options = languageOptions.map { it.first },
            selectedIndex = languageOptions.indexOfFirst { (_, tag) ->
                if (tag.isEmpty()) currentLanguageTag.isEmpty() else currentLanguageTag.startsWith(tag)
            }.coerceAtLeast(0),
            onDismissRequest = { showLanguageDialog = false },
            onSelectionChange = { index ->
                languageOptions.getOrNull(index)?.second?.let { tag ->
                    UiKitLocale.apply(context, tag)
                    onLanguageSelected?.invoke(tag)
                }
                showLanguageDialog = false
            },
        )
    }
}

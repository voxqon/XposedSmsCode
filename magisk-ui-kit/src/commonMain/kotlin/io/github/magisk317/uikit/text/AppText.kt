package io.github.magisk317.uikit.text

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Semantic text roles that let one call site serve both UI kit styles.
 *
 * Consumers that render a shared body inside a dual-track container (the
 * "Shared single implementation" shape used by XposedSmsCode and xinyi-relay)
 * cannot reach for `Text` or `MiuixText` directly: `MaterialTheme` stays
 * mounted under the Miuix theme, so a bare material3 `Text` keeps the M3
 * scale inside a Miuix chrome. Picking a role instead of a raw `TextStyle`
 * keeps the visual hierarchy owned by the active style.
 */
enum class AppTextRole {
    Title,
    Subtitle,

    /**
     * Leading prose step: material `bodyLarge`. Miuix has no second body step
     * above `body1`, so it collapses onto [Body] there - the pair still keeps
     * the material hierarchy without forcing a miuix size it does not have.
     */
    BodyLarge,

    /** Secondary prose step: material `bodyMedium`, miuix `body1`. */
    Body,
    BodySmall,
    Footnote,
}

@Composable
private fun miuixStyleFor(role: AppTextRole): TextStyle = when (role) {
    AppTextRole.Title -> MiuixTheme.textStyles.title2
    AppTextRole.Subtitle -> MiuixTheme.textStyles.title3
    AppTextRole.BodyLarge -> MiuixTheme.textStyles.body1
    AppTextRole.Body -> MiuixTheme.textStyles.body1
    AppTextRole.BodySmall -> MiuixTheme.textStyles.body2
    AppTextRole.Footnote -> MiuixTheme.textStyles.footnote1
}

@Composable
private fun materialStyleFor(role: AppTextRole): TextStyle = when (role) {
    AppTextRole.Title -> MaterialTheme.typography.titleLarge
    AppTextRole.Subtitle -> MaterialTheme.typography.titleMedium
    AppTextRole.BodyLarge -> MaterialTheme.typography.bodyLarge
    AppTextRole.Body -> MaterialTheme.typography.bodyMedium
    AppTextRole.BodySmall -> MaterialTheme.typography.bodySmall
    AppTextRole.Footnote -> MaterialTheme.typography.labelMedium
}

/**
 * Resolves [role] to the active style's own [TextStyle].
 *
 * Use when a raw [TextStyle] is required (e.g. `AppTextField(textStyle = ...)`);
 * composable text should go through [AppText] instead.
 */
@Composable
fun appTextStyle(role: AppTextRole): TextStyle =
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) miuixStyleFor(role) else materialStyleFor(role)

@Composable
private fun onSurfaceColor(): Color =
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }

/**
 * Resolves a text colour the same way `AppIcon` resolves its tint: an explicit [color] wins,
 * otherwise the host's local content colour is honoured, and only when that is unspecified do
 * we fall back to the style's [onSurfaceColor].
 *
 * Reading the local content colour is what keeps text legible inside containers that override
 * `contentColor` (e.g. the overview status card's `primary` surface carrying `onPrimary` copy).
 * An onSurface-only rule painted that copy with the page's `onSurface` instead — white-on-light,
 * which was unreadable in dark themes.
 */
@Composable
private fun resolveAppTextColor(color: Color): Color {
    if (color != Color.Unspecified) return color
    val localContentColor = if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        top.yukonga.miuix.kmp.theme.LocalContentColor.current
    } else {
        LocalContentColor.current
    }
    return if (localContentColor != Color.Unspecified) localContentColor else onSurfaceColor()
}

private fun TextStyle.withFontFamily(fontFamily: FontFamily?): TextStyle =
    if (fontFamily != null) copy(fontFamily = fontFamily) else this

/**
 * Apply a call-site [size] on top of the role's style, rescaling `lineHeight` by the
 * same ratio. Compose clips a line whose `lineHeight` is below its `fontSize`, so a
 * bare `copy(fontSize = ...)` on a smaller role (e.g. 22sp over `bodyMedium`) would
 * cut the descenders; keeping the role's leading ratio avoids that without picking a
 * line height the active style does not use.
 */
private fun TextStyle.withFontSize(size: TextUnit): TextStyle {
    if (size == TextUnit.Unspecified) return this
    if (fontSize.value <= 0f || lineHeight == TextUnit.Unspecified) return copy(fontSize = size)
    return copy(fontSize = size, lineHeight = lineHeight * (size.value / fontSize.value))
}

/**
 * Text that follows the active [UiKitStyle]: Miuix renders through
 * `MiuixTheme.textStyles`, Material keeps `MaterialTheme.typography`.
 *
 * `Color.Unspecified` follows the host's local content colour so copy inside a container that
 * overrides `contentColor` stays legible, and falls back to the style's own `onSurface` when no
 * content colour is provided.
 *
 * [fontFamily] and [fontSize] are track-independent content overrides (monospace for
 * code/token previews, a wordmark or a dialog title that sits between two role
 * steps) applied on top of the role's style; they do not add hierarchy levels for
 * the active style to own.
 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    role: AppTextRole = AppTextRole.Body,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    val resolvedColor = resolveAppTextColor(color)
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixText(
            text = text,
            modifier = modifier,
            color = resolvedColor,
            fontWeight = fontWeight,
            textAlign = textAlign,
            style = miuixStyleFor(role).withFontFamily(fontFamily).withFontSize(fontSize),
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap,
        )
        return
    }

    Text(
        text = text,
        modifier = modifier,
        color = resolvedColor,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = materialStyleFor(role).withFontFamily(fontFamily).withFontSize(fontSize),
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
    )
}

/**
 * Spanned-text variant of [AppText]: one call site for content assembled with
 * `buildAnnotatedString`, so a shared body inside a dual-track container does
 * not have to fall back to a bare material3 `Text` (which under the Miuix
 * theme renders as `Color.Black` - unreadable on dark surfaces).
 *
 * Span styles inside the [AnnotatedString] win over [color]; the parameter
 * only covers unstyled runs, exactly like the framework composables.
 */
@Composable
fun AppText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    role: AppTextRole = AppTextRole.Body,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    val resolvedColor = resolveAppTextColor(color)
    if (LocalUiKitStyle.current == UiKitStyle.Miuix) {
        MiuixText(
            text = text,
            modifier = modifier,
            color = resolvedColor,
            fontWeight = fontWeight,
            textAlign = textAlign,
            style = miuixStyleFor(role).withFontFamily(fontFamily).withFontSize(fontSize),
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap,
        )
        return
    }

    Text(
        text = text,
        modifier = modifier,
        color = resolvedColor,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = materialStyleFor(role).withFontFamily(fontFamily).withFontSize(fontSize),
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
    )
}

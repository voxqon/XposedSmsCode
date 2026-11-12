package io.github.magisk317.uikit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.magisk317.uikit.theme.LocalMagiskDarkTheme
import top.yukonga.miuix.kmp.basic.Card
import io.github.magisk317.uikit.theme.LocalMagiskDarkTheme
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * Miuix status hero card with the KernelSU-style oversized corner check mark.
 *
 * Ported from MiPush `OverviewMiuix.MiuixStatusCard` (itself ported from KernelSU's
 * `HomeMiuix.StatusCard`) so parent apps share one implementation. The pass branch takes
 * KernelSU's "working" look — a green-tinted [Card] with tilt press feedback and an oversized
 * [Icons.Rounded.CheckCircleOutline] anchored bottom-end, deliberately offset past the card
 * bounds so it is clipped into a corner motif. The fail branch reuses the same geometry with
 * KernelSU's `WarningLevel.Notice` amber palette and [Icons.Rounded.ErrorOutline], the outlined
 * circle-with-bang counterpart of the check so the corner motif keeps its weight.
 *
 * Status basis is decided by the caller: MiPush feeds the runtime connection state, XposedSmsCode
 * and xinyi-relay feed the mobile automation entitlement (auth) state. Tapping the card is wired
 * by the caller too — both auth-based apps resolve [rememberStatusCardClickHandler] so a single
 * tap opens the entitlement page while entitlement is missing.
 *
 * Deviation from KernelSU: KernelSU switches these tints on `MiuixTheme.isDynamicColor`. The
 * `MagiskUiKitTheme` miuix theme is built through the `MiuixTheme(colors = ...)` overload, which
 * never publishes `LocalColorSchemeMode`, so `isDynamicColor` is always false. The fixed palettes
 * (KernelSU's own non-dynamic paths) are used instead, with the light/dark variant picked from the
 * resolved miuix background luminance so it tracks the app's theme mode, pure black included.
 * Expressive hosts don't configure MiuixTheme, so the flag comes from
 * [LocalMagiskDarkTheme] when the kit theme provides it.
 *
 * @param passed true renders the green "working" branch with the check mark; false renders the
 *   amber notice branch with the error mark.
 * @param title headline rendered top-start (e.g. the auth status verdict).
 * @param badge secondary label rendered bottom-start (e.g. the module activation state).
 * @param summary optional tertiary line under [badge] (e.g. an activation hint).
 * @param diagnostics optional key/value rows rendered under [badge]; empty hides the block.
 * @param onClick tap handler; null makes the card non-interactive.
 */
@Composable
fun MiuixStatusCheckCard(
    passed: Boolean,
    title: String,
    badge: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    diagnostics: List<Pair<String, String>> = emptyList(),
    onClick: (() -> Unit)? = null,
) {
    // Miuix-style hosts shape MiuixTheme so the background tracks the theme mode; Expressive
    // hosts leave it at the light defaults, so prefer the theme's own resolved flag.
    val darkSurface = LocalMagiskDarkTheme.current
        ?: (MiuixTheme.colorScheme.background.luminance() < 0.5f)
    val container = when {
        passed && darkSurface -> STATUS_CHECK_CARD_PASS_DARK
        passed -> STATUS_CHECK_CARD_PASS_LIGHT
        darkSurface -> STATUS_CHECK_CARD_FAIL_DARK
        else -> STATUS_CHECK_CARD_FAIL_LIGHT
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        colors = CardDefaults.defaultColors(color = container),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(27.dp, 31.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier.size(110.dp),
                    imageVector = if (passed) {
                        Icons.Rounded.CheckCircleOutline
                    } else {
                        Icons.Rounded.ErrorOutline
                    },
                    tint = if (passed) STATUS_CHECK_CARD_PASS_MARK else STATUS_CHECK_CARD_FAIL_MARK,
                    contentDescription = null,
                )
            }
            // Single top-down column: the former overlay boxes let the diagnostics block
            // printed after a 5-tap reveal run over the title/badge. Keeping one flow also
            // preserves the corner motif — the oversized mark stays bottom-end and clipped.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = badge,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                diagnostics.forEach { (label, value) ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            text = value,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

// KernelSU's "working" palette, reused so the pass branch matches the ported look.
private val STATUS_CHECK_CARD_PASS_DARK = Color(0xFF1A3825)
private val STATUS_CHECK_CARD_PASS_LIGHT = Color(0xFFDFFAE4)
private val STATUS_CHECK_CARD_PASS_MARK = Color(0xFF36D167)

// KernelSU's WarningLevel.Notice palette, reused so the fail branch matches the ported look.
private val STATUS_CHECK_CARD_FAIL_DARK = Color(0xFF3E2F1B)
private val STATUS_CHECK_CARD_FAIL_LIGHT = Color(0xFFFFF0DB)
private val STATUS_CHECK_CARD_FAIL_MARK = Color(0xFFF5A623)

package io.github.magisk317.uikit.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.surface.AppCard
import io.github.magisk317.uikit.surface.AppCircularProgressIndicator
import io.github.magisk317.uikit.text.AppText
import io.github.magisk317.uikit.text.AppTextRole
import io.github.magisk317.uikit.theme.AppColorRole
import io.github.magisk317.uikit.theme.appColor

/**
 * Generic building blocks for step-based permission / setup onboarding flows.
 *
 * Extracted from the root-first setup wizard shared by the Magisk317 app
 * family (xinyi-relay, MiPushFramework). Every component is stateless
 * presentation: callers own all copy, state and navigation. Rendering goes
 * through the dual-track [AppCard] / [AppText] / [AppCircularProgressIndicator]
 * contracts, so one implementation serves both UI kit styles.
 *
 * [OnboardingItemState] is deliberately a presentation state, not a domain one:
 * it only decides the colour of a trailing label, so a parent app can map its own
 * "granted / not granted / could not read" model onto it without the kit learning
 * anything about root, binders or permissions.
 */

/**
 * Title + optional summary card, used for step intros and static notices.
 *
 * [summary] is a [CharSequence] so a caller can hand over an [AnnotatedString]
 * (spans, links) instead of a bare sentence; the kit still owns no copy and no
 * platform link handling - it only picks the matching [AppText] variant.
 */
@Composable
fun OnboardingInfoCard(
    title: String,
    modifier: Modifier = Modifier,
    summary: CharSequence? = null,
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppText(text = title, role = AppTextRole.Subtitle)
            if (!summary.isNullOrBlank()) {
                if (summary is AnnotatedString) {
                    AppText(text = summary, role = AppTextRole.BodySmall)
                } else {
                    AppText(text = summary.toString(), role = AppTextRole.BodySmall)
                }
            }
        }
    }
}

/** Card with an indeterminate spinner beside a status line, for in-flight steps. */
@Composable
fun OnboardingProgressRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppCircularProgressIndicator(modifier = Modifier.size(24.dp))
            AppText(text = text, role = AppTextRole.Body)
        }
    }
}

/** Card rendering grant / attempt log lines as footnote text, oldest first. */
@Composable
fun OnboardingLogCard(
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            lines.forEach { line ->
                AppText(text = line, role = AppTextRole.Footnote)
            }
        }
    }
}

/**
 * Clickable row for one manual-grant item: [label] names the requirement,
 * [desc] optionally explains what it unlocks and [actionText] is the trailing
 * hint (e.g. "open" or the current grant state). Tapping fires [onClick];
 * the caller resolves the target intent and refreshes state on return.
 *
 * [state] only tints [actionText]; leaving it null keeps the previous uncoloured
 * rendering, so existing callers are unaffected.
 */
@Composable
fun OnboardingActionRowCard(
    label: String,
    actionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    desc: String? = null,
    state: OnboardingItemState? = null,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        val trailingColor = if (state == null) Color.Unspecified else onboardingItemStateColor(state)
        OnboardingRowContent(
            label = label,
            desc = desc,
            trailing = actionText,
            trailingColor = trailingColor,
        )
    }
}

/** How an onboarding item should be coloured; the caller keeps owning the wording. */
enum class OnboardingItemState {
    /** Requirement satisfied. */
    Granted,

    /** Requirement unmet, or its state could not be read. */
    Outstanding,

    /** A grant attempt is in flight. */
    Pending,
}

@Composable
private fun onboardingItemStateColor(state: OnboardingItemState): Color = appColor(
    when (state) {
        OnboardingItemState.Granted -> AppColorRole.Primary
        OnboardingItemState.Outstanding -> AppColorRole.Error
        OnboardingItemState.Pending -> AppColorRole.OnSurfaceVariant
    },
)

/**
 * Headline block for the end-of-flow report: [title] above the caller-composed
 * count lines. Pass the "all ready" wording as [grantedText] and leave
 * [missingText] null when nothing is outstanding; this only fixes the hierarchy.
 */
@Composable
fun OnboardingSummaryHeader(
    title: String,
    modifier: Modifier = Modifier,
    grantedText: String? = null,
    missingText: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppText(text = title, role = AppTextRole.Title)
        if (!grantedText.isNullOrBlank()) {
            AppText(text = grantedText, role = AppTextRole.Body)
        }
        if (!missingText.isNullOrBlank()) {
            AppText(text = missingText, role = AppTextRole.BodySmall)
        }
    }
}

/** Non-interactive counterpart of [OnboardingActionRowCard] for report lists. */
@Composable
fun OnboardingStatusRow(
    label: String,
    state: OnboardingItemState,
    stateText: String,
    modifier: Modifier = Modifier,
    desc: String? = null,
) {
    AppCard(modifier = modifier) {
        OnboardingRowContent(
            label = label,
            desc = desc,
            trailing = stateText,
            trailingColor = onboardingItemStateColor(state),
        )
    }
}

@Composable
private fun OnboardingRowContent(
    label: String,
    desc: String?,
    trailing: String,
    trailingColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = label, role = AppTextRole.Body)
            if (!desc.isNullOrBlank()) {
                AppText(text = desc, role = AppTextRole.BodySmall)
            }
        }
        AppText(text = trailing, role = AppTextRole.Footnote, color = trailingColor)
    }
}

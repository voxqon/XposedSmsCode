package io.github.magisk317.uikit.onboarding

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Source-text guard for the shared onboarding components.
 *
 * Fragile on purpose - the only regression guard keeping the onboarding
 * primitives on the dual-track contracts: a bare material3 `Text` or `Card`
 * would silently break the Miuix track, and the action row must keep routing
 * its click through `AppCard`'s onClick slot. Update in the same commit as any
 * layout change rather than deleting this test.
 */
class OnboardingComponentsContractTest {

    private fun readSource(): String {
        val override = System.getProperty("uikit.sourceRoot")
            ?: System.getenv("UIKIT_SOURCE_ROOT")
        val root = if (override.isNullOrBlank()) {
            File("src/commonMain/kotlin/io/github/magisk317/uikit/onboarding")
        } else {
            File(override)
        }
        return File(root, "OnboardingComponents.kt").absoluteFile.readText()
    }

    @Test
    fun onboardingComponentsRenderThroughDualTrackContracts() {
        val source = readSource()
        listOf(
            "OnboardingInfoCard",
            "OnboardingProgressRow",
            "OnboardingLogCard",
            "OnboardingActionRowCard",
            "OnboardingSummaryHeader",
            "OnboardingStatusRow",
        ).forEach { name ->
            assertTrue(source.contains("fun $name("), "$name must stay declared here")
        }
        assertFalse(
            Regex("""androidx\.compose\.material3\.Text\b""").containsMatchIn(source),
            "text must go through AppText, not a bare material3 Text",
        )
        assertFalse(
            Regex("""androidx\.compose\.material3\.Card\b""").containsMatchIn(source),
            "containers must go through AppCard, not a bare material3 Card",
        )
        assertTrue(source.contains("AppCard("), "cards must render through AppCard")
        assertTrue(source.contains("AppText("), "copy must render through AppText")
        assertTrue(source.contains("AppCircularProgressIndicator("), "spinner must go through the kit indicator")
    }

    @Test
    fun actionRowCardStaysClickableThroughAppCard() {
        val source = readSource()
        val regex = Regex("""fun OnboardingActionRowCard\(.*?\n\)\s*\{""", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(source)
        assertTrue(match != null, "OnboardingActionRowCard header not found")
        val body = source.substring(match!!.range.last)
        assertTrue(body.contains("onClick = onClick"), "click must route through AppCard's onClick slot")
    }

    @Test
    fun itemStateStaysPresentationOnlyAndOptional() {
        val source = readSource()
        val declaration = Regex("""enum class OnboardingItemState \{.*?\n\}""", RegexOption.DOT_MATCHES_ALL)
            .find(source)
        assertTrue(declaration != null, "OnboardingItemState must stay declared here")
        listOf("Granted", "Outstanding", "Pending").forEach { case ->
            assertTrue(
                declaration!!.value.contains(case),
                "$case must stay part of the shared item state",
            )
        }
        // Tinting is the only thing the kit decides from the state; an extra case
        // naming a parent concept would mean domain logic had been pushed down.
        listOf("root", "binder", "permission", "xmsf").forEach { token ->
            assertFalse(
                declaration!!.value.contains(token, ignoreCase = true),
                "item state must not name the parent domain concept '$token'",
            )
        }
        assertTrue(
            source.contains("state: OnboardingItemState? = null,"),
            "the action row's state stays optional and last so existing callers keep compiling",
        )
    }

    @Test
    fun stateColoursResolveThroughThePalette() {
        val source = readSource()
        assertTrue(
            Regex("""onboardingItemStateColor\(.*?appColor\(""").containsMatchIn(source),
            "state tints must resolve through appColor so both style tracks follow",
        )
        assertFalse(
            Regex("""Color\(0x""").containsMatchIn(source),
            "no hard-coded colour literals in onboarding",
        )
    }
}

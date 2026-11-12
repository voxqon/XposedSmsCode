package io.github.magisk317.uikit.surface

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainTabScaffoldTest {
    @Test
    fun contentBottomPaddingReportsInitialValue() {
        val reporter = ContentBottomPaddingReporter()
        val reportedValues = mutableListOf<Dp>()

        reporter.report(0.dp) { reportedValues += it }

        assertEquals(listOf(0.dp), reportedValues)
    }

    @Test
    fun contentBottomPaddingSuppressesUnchangedValues() {
        val reporter = ContentBottomPaddingReporter()
        val reportedValues = mutableListOf<Dp>()

        repeat(3) {
            reporter.report(72.dp) { reportedValues += it }
        }

        assertEquals(listOf(72.dp), reportedValues)
    }

    @Test
    fun contentBottomPaddingReportsEachChangeIncludingHiddenChrome() {
        val reporter = ContentBottomPaddingReporter()
        val reportedValues = mutableListOf<Dp>()

        reporter.report(0.dp) { reportedValues += it }
        reporter.report(72.dp) { reportedValues += it }
        reporter.report(64.dp) { reportedValues += it }
        reporter.report(0.dp) { reportedValues += it }

        assertEquals(listOf(0.dp, 72.dp, 64.dp, 0.dp), reportedValues)
    }

    @Test
    fun systemBarsScrimFollowsFeatureFlagOnly() {
        assertEquals(
            true,
            resolveSystemBarsScrimVisible(showSystemBarsScrim = true),
        )
        assertEquals(
            false,
            resolveSystemBarsScrimVisible(showSystemBarsScrim = false),
        )
    }

    @Test
    fun contentBottomPaddingUsesReplacementCallbackForNextChange() {
        val reporter = ContentBottomPaddingReporter()
        val firstCallbackValues = mutableListOf<Dp>()
        val replacementCallbackValues = mutableListOf<Dp>()

        reporter.report(0.dp) { firstCallbackValues += it }
        reporter.report(0.dp) { replacementCallbackValues += it }
        reporter.report(72.dp) { replacementCallbackValues += it }

        assertEquals(listOf(0.dp), firstCallbackValues)
        assertEquals(listOf(72.dp), replacementCallbackValues)
    }
}

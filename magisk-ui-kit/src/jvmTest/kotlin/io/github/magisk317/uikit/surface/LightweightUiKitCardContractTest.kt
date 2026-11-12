package io.github.magisk317.uikit.surface

import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Source-text guard: it reads StatusCheckCard.kt and asserts layout structure.
 * Fragile on purpose - if you refactor MiuixStatusCheckCard, update this test in the
 * same commit rather than deleting it (it is the only regression guard for the
 * 5-tap expanded-text overlap fix).
 *
 * Fix 4 static contract: xinyi overview 5-tap expanded text no longer overlaps the status
 * check card. Guards the single-column MiuixStatusCheckCard layout.
 */
class LightweightUiKitCardContractTest {

    private fun read(name: String): String {
        val override = System.getProperty("xinyi.uiKitSourceRoot")
            ?: System.getenv("XINYI_UI_KIT_SOURCE_ROOT")
        val root = if (override.isNullOrBlank()) {
            File("src/commonMain/kotlin/io/github/magisk317/uikit/surface")
        } else {
            File(override)
        }
        return File(root, name).absoluteFile.readText()
    }

    @Test
    fun miuixStatusCheckCard_usesSingleColumn() {
        val source = read("StatusCheckCard.kt")
        val regex = Regex(
            """fun MiuixStatusCheckCard\(.*?\n\)\s*\{""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val match = regex.find(source)
        assertTrue(match != null, "MiuixStatusCheckCard header not found")
        val body = source.substring(match!!.range.last)
        // The three stacked fillMaxSize overlay boxes are gone; the body opens with a Column.
        assertTrue(body.contains("Column("), "MiuixStatusCheckCard body must open with a Column")
        val overlayCount = Regex("""fillMaxSize\(\)""").findAll(body).count()
        assertTrue(
            overlayCount <= 1,
            "only the bottom-right badge box may keep fillMaxSize (found $overlayCount)",
        )
    }
}

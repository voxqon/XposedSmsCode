package io.github.magisk317.uikit.surface

import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Source-text guard: it reads AppControls.kt / AppOverlays.kt and asserts layout
 * structure. Fragile on purpose - if you refactor DialogButtonRow or AppAlertDialog,
 * update this test in the same commit rather than deleting it.
 *
 * Fix 1/4 static contract: xinyi usage-frequency dialog renders a centered two-button row in
 * both dialects. Source-text contract mirrors StringFormatTest in io/github/magisk317/smscode:
 * 0 runtime deps, executes wherever the gradle test task lands.
 *
 * KMP note: AppControls.kt lives in commonMain while AppOverlays.kt lives in androidMain,
 * so the source root is resolved per file rather than from a single directory.
 */
class LightweightUiKitSurfaceContractTest {

    private fun sourceFile(name: String): File {
        val override = System.getProperty("xinyi.uiKitSourceRoot")
            ?: System.getenv("XINYI_UI_KIT_SOURCE_ROOT")
        val path = when (name) {
            "AppControls.kt" -> "src/commonMain/kotlin/io/github/magisk317/uikit/surface/$name"
            else -> "src/androidMain/kotlin/io/github/magisk317/uikit/surface/$name"
        }
        val root = if (override.isNullOrBlank()) {
            File(path)
        } else {
            File(override, name)
        }
        return root.absoluteFile
    }

    private fun read(name: String): String = sourceFile(name).readText()

    @Test
    fun dialogButtonRow_defaultMiuix_keepsCenteredArrangement() {
        val source = read("AppControls.kt")
        val regex = Regex(
            """fun DialogButtonRow\(.*?\)\s*\{.*?Arrangement\.spacedBy\(([^,]+?),\s*[\w.]*CenterHorizontally\)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val match = regex.find(source)
        assertTrue(match != null, "DialogButtonRow must keep centered arrangement")
        val gapArg = match!!.groupValues[1].trim()
        assertTrue(
            gapArg == "12.dp" || gapArg == "MaterialTheme.spacing.small",
            "Unexpected gap: $gapArg",
        )
    }

    @Test
    fun alertDialog_confirmButton_wrappedInCenteredRow() {
        val source = read("AppOverlays.kt")
        val hasCenteredRow = source.contains("CenterHorizontally") &&
            source.contains("confirmButton = {")
        assertTrue(hasCenteredRow, "AppAlertDialog must wrap confirm row with CenterHorizontally")
    }
}

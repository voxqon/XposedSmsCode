package io.github.magisk317.uikit.preference

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Source-text guard for SectionCard's optional header.
 *
 * Fragile on purpose, mirroring LightweightUiKitCardContractTest: the header must appear only when
 * a title is passed, and the card frame must survive without it. A future edit that drops the frame
 * to remove the header (or that always renders an empty header row) silently reintroduces the two
 * shapes this component was collapsed to avoid. Update this test in the same commit as any intended
 * refactor.
 */
class SectionCardHeaderContractTest {

    private fun preferenceSource(): String {
        val override = System.getProperty("magisk.uiKitPreferenceRoot")
            ?: System.getenv("MAGISK_UI_KIT_PREFERENCE_ROOT")
        val root = if (override.isNullOrBlank()) {
            File("src/androidMain/kotlin/io/github/magisk317/uikit/preference")
        } else {
            File(override)
        }
        return File(root, "PreferenceItems.kt").absoluteFile.readText()
    }

    @Test
    fun `the single SectionCard takes an optional title and defaults the accordion params`() {
        val source = preferenceSource()
        val start = source.indexOf("fun SectionCard(")
        assertTrue(start >= 0, "SectionCard not found")
        val signature = source.substring(start, source.indexOf(") {", start))
        assertTrue(signature.contains("title: String? = null,"), "title must be an optional header")
        assertTrue(signature.contains("summary: String = \"\""), "summary must default to empty")
        assertTrue(
            signature.contains("accordionMode: Boolean = false,"),
            "a static group must not have to spell out the accordion params",
        )
        assertTrue(signature.contains("onExpandedChange: () -> Unit = {},"), "expand callback defaults to no-op")
        assertFalse(source.contains("fun SectionList("), "the frameless sibling was rejected; keep one component")
        assertEqualsCount(source, "fun SectionCard(", 1)
    }

    @Test
    fun `both style branches gate the header on the title and keep the frame`() {
        val source = preferenceSource()
        assertEqualsCount(source, "if (title != null) {", 2)
        assertTrue(
            source.contains("CardDefaults.cardColors("),
            "the Material branch must keep the card frame",
        )
        assertTrue(
            source.contains("MiuixCard("),
            "the Miuix branch must keep the grouped card",
        )
        val priv = source.indexOf("private fun MiuixSectionCard(")
        assertTrue(priv >= 0, "MiuixSectionCard missing")
        assertTrue(
            source.substring(priv).contains("title: String?,"),
            "MiuixSectionCard must accept a nullable title",
        )
    }

    private fun assertEqualsCount(source: String, needle: String, expected: Int) {
        val count = source.split(needle).size - 1
        assertTrue(count == expected, "expected $expected occurrence(s) of \"$needle\", found $count")
    }
}

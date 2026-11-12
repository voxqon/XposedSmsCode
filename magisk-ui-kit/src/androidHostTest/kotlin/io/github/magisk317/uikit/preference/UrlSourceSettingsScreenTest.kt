package io.github.magisk317.uikit.preference

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrlSourceSettingsScreenTest {
    @Test
    fun `blank and http sources are valid`() {
        assertTrue(isValidHttpUrlSource(""))
        assertTrue(isValidHttpUrlSource("https://rules.example.test/catalog"))
        assertTrue(isValidHttpUrlSource("http://10.0.0.2/rules/_meta/rules-index.json"))
    }

    @Test
    fun `unsupported or ambiguous sources are invalid`() {
        assertFalse(isValidHttpUrlSource("rules.example.test/catalog"))
        assertFalse(isValidHttpUrlSource("ftp://rules.example.test/catalog"))
        assertFalse(isValidHttpUrlSource("https://rules.example.test/catalog?token=secret"))
    }
}

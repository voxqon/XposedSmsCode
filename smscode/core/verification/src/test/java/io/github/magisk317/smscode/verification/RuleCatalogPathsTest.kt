package io.github.magisk317.smscode.verification

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RuleCatalogPathsTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `bundled paths and spaces are accepted`() {
        for (path in listOf("rules/cn/general/cn.general.default.json", "rules/acme rule.json")) {
            RuleCatalogPaths.requireRulePath(path)
            assertEquals(File(directory, path).canonicalFile, RuleCatalogPaths.resolveFile(directory, path))
        }
    }

    @Test
    fun `traversal absolute paths and encoded separators are rejected`() {
        for (path in listOf(
            "../prefs.json", "rules/../../prefs.json", "/rules/file.json",
            "C:/rules/file.json", "rules\\..\\file.json", "rules/%2e%2e/file.json",
            "rules//file.json", "rules/./file.json", "rules/file.json?token=x",
            "rules/a\u0000.json", "rules/dir. /file.json",
        )) {
            assertThrows(IllegalArgumentException::class.java, { RuleCatalogPaths.requireRulePath(path) }, path)
        }
    }

    @Test
    fun `a symlink cannot redirect writes outside the cache`() {
        val cache = File(directory, "cache").apply { mkdirs() }
        val outside = File(directory, "outside").apply { mkdirs() }
        try {
            Files.createSymbolicLink(File(cache, "rules").toPath(), outside.toPath())
        } catch (_: java.nio.file.FileSystemException) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symlinks require OS permission")
        } catch (_: UnsupportedOperationException) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symlinks unavailable")
        }
        assertThrows(IllegalArgumentException::class.java) {
            RuleCatalogPaths.resolveFile(cache, "rules/prefs.json")
        }
    }
}

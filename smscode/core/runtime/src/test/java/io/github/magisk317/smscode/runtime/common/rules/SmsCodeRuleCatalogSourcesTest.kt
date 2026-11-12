package io.github.magisk317.smscode.runtime.common.rules

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SmsCodeRuleCatalogSourcesTest {
    @Test
    fun `local catalogs round trip without a network source`() = runBlocking {
        val directory = Files.createTempDirectory("local-rule-test").toFile()
        try {
            val source = FileSmsCodeRuleCatalogSource(File(directory, "catalog"))
            source.writeCatalog("index", mapOf("rules/example.json" to "rule"))
            assertEquals("index", source.readIndex())
            assertEquals("rule", source.readRule("rules/example.json"))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `file source rejects traversal before modifying an existing cache`() = runBlocking {
        val directory = Files.createTempDirectory("rule-cache-test").toFile()
        try {
            val cache = FileSmsCodeRuleCatalogSource(File(directory, "cache"))
            cache.writeCatalog("old-index", mapOf("rules/old.json" to "old-rule"))
            val failure = runCatching {
                cache.writeCatalog("malicious-index", mapOf("../outside.json" to "bad"))
            }.exceptionOrNull()
            assertEquals(IllegalArgumentException::class.java, failure?.javaClass)
            assertEquals("old-index", cache.readIndex())
            assertEquals(false, File(directory, "outside.json").exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}

package io.github.magisk317.smscode.runtime.common.rules

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import io.github.magisk317.smscode.verification.BoundedInput
import io.github.magisk317.smscode.verification.RuleCatalogPaths
import java.nio.file.Files

interface SmsCodeRuleCatalogSource {
    suspend fun readIndex(): String
    suspend fun readRule(path: String): String
}

interface WritableSmsCodeRuleCatalogSource : SmsCodeRuleCatalogSource {
    suspend fun writeCatalog(indexText: String, ruleTexts: Map<String, String>)
}

class AssetSmsCodeRuleCatalogSource(
    context: Context,
    private val assetRoot: String = DEFAULT_SMS_CODE_RULES_ASSET_ROOT,
) : SmsCodeRuleCatalogSource {
    private val appContext = context.applicationContext ?: context

    override suspend fun readIndex(): String =
        readAssetOrNull(SMS_CODE_RULES_INDEX_PATH) ?: readAsset(SMS_CODE_RULES_BUNDLED_INDEX_PATH)

    override suspend fun readRule(path: String): String {
        RuleCatalogPaths.requireRulePath(path)
        return readAsset(path)
    }

    private suspend fun readAsset(path: String): String =
        readAssetOrNull(path) ?: throw FileNotFoundException("$assetRoot/$path")

    private suspend fun readAssetOrNull(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            appContext.assets.open("$assetRoot/$path").use {
                String(BoundedInput.readBytes(it, BoundedInput.MAX_RULE_FILE_BYTES), Charsets.UTF_8)
            }
        }.getOrNull()
    }
}

class FileSmsCodeRuleCatalogSource(
    private val rootDir: File,
) : WritableSmsCodeRuleCatalogSource {
    private val writeLock = Any()
    override suspend fun readIndex(): String = readFile(SMS_CODE_RULES_INDEX_PATH)

    override suspend fun readRule(path: String): String = readFile(path)

    override suspend fun writeCatalog(indexText: String, ruleTexts: Map<String, String>) = withContext(Dispatchers.IO) {
        synchronized(writeLock) {
            // Validate every path before creating any file.
            ruleTexts.keys.forEach(RuleCatalogPaths::requireRulePath)
            val parent = rootDir.absoluteFile.parentFile ?: throw IOException("Rule cache parent unavailable")
            if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create rule cache parent")
            val tempDir = Files.createTempDirectory(parent.toPath(), "${rootDir.name}.").toFile()
            val previous = File(parent, "${tempDir.name}.previous")
            try {
                writeFile(tempDir, SMS_CODE_RULES_INDEX_PATH, indexText)
                for ((path, text) in ruleTexts) writeFile(tempDir, path, text)
                if (rootDir.exists() && !rootDir.renameTo(previous)) {
                    throw IOException("Unable to preserve previous rule cache")
                }
                if (!tempDir.renameTo(rootDir)) {
                    if (previous.exists() && !previous.renameTo(rootDir)) {
                        throw IOException("Rule cache preserved at ${previous.name}; replacement failed")
                    }
                    throw IOException("Unable to replace rule cache")
                }
                if (previous.exists()) previous.deleteRecursively()
            } finally {
                if (tempDir.exists()) tempDir.deleteRecursively()
            }
        }
        Unit
    }

    private suspend fun readFile(path: String): String = withContext(Dispatchers.IO) {
        val file = RuleCatalogPaths.resolveFile(rootDir, path)
        if (!file.isFile) throw FileNotFoundException(file.absolutePath)
        file.inputStream().use {
            String(BoundedInput.readBytes(it, BoundedInput.MAX_RULE_FILE_BYTES), Charsets.UTF_8)
        }
    }

    private fun writeFile(root: File, path: String, text: String) {
        val file = RuleCatalogPaths.resolveFile(root, path)
        require(text.toByteArray(Charsets.UTF_8).size <= BoundedInput.MAX_RULE_FILE_BYTES) {
            "Rule file exceeds size limit"
        }
        file.parentFile?.mkdirs()
        file.writeText(text, Charsets.UTF_8)
    }
}


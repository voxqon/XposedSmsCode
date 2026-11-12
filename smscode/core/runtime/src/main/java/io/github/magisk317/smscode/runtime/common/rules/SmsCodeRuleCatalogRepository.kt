package io.github.magisk317.smscode.runtime.common.rules

import android.content.Context
import io.github.magisk317.smscode.rule.catalog.OfficialSmsCodeRule
import io.github.magisk317.smscode.rule.catalog.SmsCodeRuleCatalogFile
import io.github.magisk317.smscode.rule.catalog.SmsCodeRuleCatalogIndex
import io.github.magisk317.smscode.rule.catalog.SmsCodeRuleDocument
import io.github.magisk317.smscode.rule.catalog.SmsCodeRuleSetDocument
import io.github.magisk317.smscode.runtime.common.serialization.JsonConfig
import java.io.File
import java.security.MessageDigest
import java.util.regex.Pattern
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString
import io.github.magisk317.smscode.verification.BoundedInput
import io.github.magisk317.smscode.verification.RuleCatalogPaths

class SmsCodeRuleCatalogRepository(
    private val cacheSource: WritableSmsCodeRuleCatalogSource,
    private val bundledSource: SmsCodeRuleCatalogSource,
    private val remoteSource: SmsCodeRuleCatalogSource,
    private val remoteFallbackSources: List<SmsCodeRuleCatalogSource> = emptyList(),
    private val useBundledOnly: Boolean = false,
) {
    constructor(
        context: Context,
        assetRoot: String = DEFAULT_SMS_CODE_RULES_ASSET_ROOT,
        cacheDir: File = File((context.applicationContext ?: context).filesDir, DEFAULT_SMS_CODE_RULES_ASSET_ROOT),
    ) : this(
        cacheSource = FileSmsCodeRuleCatalogSource(cacheDir),
        bundledSource = AssetSmsCodeRuleCatalogSource(context, assetRoot),
        remoteSource = AssetSmsCodeRuleCatalogSource(context, assetRoot),
        useBundledOnly = true,
    )

    suspend fun loadOfficialRules(): SmsCodeRuleCatalogSnapshot {
        if (useBundledOnly) {
            return loadFrom(bundledSource, SmsCodeRuleCatalogSourceKind.BUNDLED).getOrElse {
                SmsCodeRuleCatalogSnapshot(SmsCodeRuleCatalogSourceKind.EMPTY, null, emptyList())
            }
        }
        return loadFrom(cacheSource, SmsCodeRuleCatalogSourceKind.CACHE).getOrElse {
            loadFrom(bundledSource, SmsCodeRuleCatalogSourceKind.BUNDLED).getOrElse {
                SmsCodeRuleCatalogSnapshot(
                    sourceKind = SmsCodeRuleCatalogSourceKind.EMPTY,
                    index = null,
                    rules = emptyList(),
                )
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun refreshOfficialRules(): SmsCodeRuleCatalogRefreshResult {
        if (useBundledOnly) {
            val snapshot = loadOfficialRules()
            return SmsCodeRuleCatalogRefreshResult(
                success = snapshot.sourceKind == SmsCodeRuleCatalogSourceKind.BUNDLED,
                snapshot = snapshot,
            )
        }
        var downloadedCatalog: DownloadedCatalog? = null
        var lastFailure: Exception? = null
        for (source in listOf(remoteSource) + remoteFallbackSources) {
            try {
                downloadedCatalog = downloadCatalog(source)
                break
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                lastFailure = failure
            }
        }

        val downloaded = downloadedCatalog ?: return refreshFailure(lastFailure)
        return try {
            cacheSource.writeCatalog(downloaded.indexText, downloaded.ruleTexts)
            val snapshot = buildSnapshot(
                sourceKind = SmsCodeRuleCatalogSourceKind.REMOTE,
                index = downloaded.index,
                ruleTexts = downloaded.ruleTexts,
            )
            SmsCodeRuleCatalogRefreshResult(success = true, snapshot = snapshot)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            refreshFailure(failure)
        }
    }

    private suspend fun downloadCatalog(source: SmsCodeRuleCatalogSource): DownloadedCatalog {
        val indexText = source.readIndex()
        val index = decodeIndex(indexText)
        val ruleTexts = linkedMapOf<String, String>()
        for (file in index.files) {
            val text = source.readRule(file.path)
            verifyFile(file, text)
            ruleTexts[file.path] = text
        }
        return DownloadedCatalog(indexText, index, ruleTexts)
    }

    private suspend fun refreshFailure(failure: Exception?): SmsCodeRuleCatalogRefreshResult =
        SmsCodeRuleCatalogRefreshResult(
            success = false,
            snapshot = loadOfficialRules(),
            errorMessage = failure?.message
                ?: failure?.javaClass?.simpleName
                ?: "Remote catalog unavailable",
        )

    private data class DownloadedCatalog(
        val indexText: String,
        val index: SmsCodeRuleCatalogIndex,
        val ruleTexts: Map<String, String>,
    )

    private suspend fun loadFrom(
        source: SmsCodeRuleCatalogSource,
        kind: SmsCodeRuleCatalogSourceKind,
    ): Result<SmsCodeRuleCatalogSnapshot> = try {
        val index = decodeIndex(source.readIndex())
        val ruleTexts = linkedMapOf<String, String>()
        for (file in index.files) {
            val text = source.readRule(file.path)
            verifyFile(file, text)
            ruleTexts[file.path] = text
        }
        Result.success(buildSnapshot(kind, index, ruleTexts))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    private fun buildSnapshot(
        sourceKind: SmsCodeRuleCatalogSourceKind,
        index: SmsCodeRuleCatalogIndex,
        ruleTexts: Map<String, String>,
    ): SmsCodeRuleCatalogSnapshot {
        val officialRules = mutableListOf<OfficialSmsCodeRule>()
        val rejected = mutableListOf<String>()
        for (file in index.files) {
            val text = ruleTexts[file.path] ?: continue
            val documentResult = runCatching { decodeRuleSet(text) }
            val document = documentResult.getOrNull()
            if (document == null) {
                val throwable = documentResult.exceptionOrNull()
                rejected += "${file.path}: ${throwable?.message ?: throwable?.javaClass?.simpleName ?: "decode failed"}"
                continue
            }
            for (rule in document.rules) {
                val rejectReason = validateRule(rule)
                if (rejectReason != null) {
                    rejected += "${file.path}#${rule.id}: $rejectReason"
                    continue
                }
                officialRules += OfficialSmsCodeRule(
                    setId = document.id,
                    setName = document.name,
                    setLocale = document.locale,
                    setUpdatedAt = document.updatedAt,
                    sourcePath = file.path,
                    id = rule.id,
                    company = rule.company,
                    codeKeyword = rule.codeKeyword,
                    codeRegex = rule.codeRegex,
                    senderRegex = rule.senderRegex,
                    packageNameHint = rule.packageNameHint,
                    priority = rule.priority,
                    rejectedSuffixes = rule.rejectedSuffixes,
                    notes = rule.notes,
                )
            }
        }
        return SmsCodeRuleCatalogSnapshot(
            sourceKind = sourceKind,
            index = index,
            rules = officialRules,
            rejectedRules = rejected,
        )
    }

    companion object {
        private val json = JsonConfig.json

        fun decodeIndex(text: String): SmsCodeRuleCatalogIndex {
            require(text.toByteArray(Charsets.UTF_8).size <= BoundedInput.MAX_RULE_FILE_BYTES) {
                "Rule index exceeds size limit"
            }
            val index = json.decodeFromString<SmsCodeRuleCatalogIndex>(text)
            require(index.files.size <= BoundedInput.MAX_RULE_FILES) { "Too many rule files" }
            val paths = mutableSetOf<String>()
            var totalBytes = 0L
            index.files.forEach { file ->
                RuleCatalogPaths.requireRulePath(file.path)
                require(paths.add(file.path.lowercase(java.util.Locale.ROOT))) { "Duplicate rule path" }
                require(file.size in 0..BoundedInput.MAX_RULE_FILE_BYTES) { "Invalid rule file size" }
                require(file.sha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid rule checksum" }
                require(file.ruleCount in 0..10_000) { "Invalid rule count" }
                totalBytes += file.size
                require(totalBytes <= BoundedInput.MAX_RULE_CATALOG_BYTES) { "Rule catalog exceeds size limit" }
            }
            return index
        }

        fun decodeRuleSet(text: String): SmsCodeRuleSetDocument {
            require(text.toByteArray(Charsets.UTF_8).size <= BoundedInput.MAX_RULE_FILE_BYTES) {
                "Rule file exceeds size limit"
            }
            return json.decodeFromString<SmsCodeRuleSetDocument>(text).also {
                require(it.rules.size <= 10_000) { "Too many rules" }
            }
        }

        fun verifyFile(file: SmsCodeRuleCatalogFile, text: String) {
            require(file.size == text.toByteArray(Charsets.UTF_8).size) {
                "Size mismatch for ${file.path}"
            }
            require(file.sha256.equals(sha256(text), ignoreCase = true)) {
                "SHA-256 mismatch for ${file.path}"
            }
        }

        private fun validateRule(rule: SmsCodeRuleDocument): String? {
            if (rule.codeKeyword.isBlank()) return "blank codeKeyword"
            if (rule.codeRegex.isBlank()) return "blank codeRegex"
            return runCatching {
                Pattern.compile(rule.codeRegex)
                null
            }.getOrElse { throwable ->
                throwable.message ?: throwable.javaClass.simpleName
            }
        }

        private fun sha256(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}

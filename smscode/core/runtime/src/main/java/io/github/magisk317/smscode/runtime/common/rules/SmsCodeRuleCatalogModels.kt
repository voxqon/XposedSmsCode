package io.github.magisk317.smscode.runtime.common.rules

import io.github.magisk317.smscode.rule.catalog.OfficialSmsCodeRule
import io.github.magisk317.smscode.rule.catalog.SmsCodeRuleCatalogIndex

const val DEFAULT_SMS_CODE_RULES_ASSET_ROOT = "smscode-rules"
const val SMS_CODE_RULES_INDEX_PATH = "_meta/rules-index.json"
const val SMS_CODE_RULES_BUNDLED_INDEX_PATH = "meta/rules-index.json"

enum class SmsCodeRuleCatalogSourceKind {
    CACHE,
    BUNDLED,
    REMOTE,
    EMPTY,
}

data class SmsCodeRuleCatalogSnapshot(
    val sourceKind: SmsCodeRuleCatalogSourceKind,
    val index: SmsCodeRuleCatalogIndex?,
    val rules: List<OfficialSmsCodeRule>,
    val rejectedRules: List<String> = emptyList(),
    val loadedAtMillis: Long = System.currentTimeMillis(),
)

data class SmsCodeRuleCatalogRefreshResult(
    val success: Boolean,
    val snapshot: SmsCodeRuleCatalogSnapshot?,
    val errorMessage: String? = null,
)

package io.github.magisk317.smscode.common.constant

object PreferenceBackupPolicy {
    val excludedKeys: Set<String> = setOf(
        PrefConst.KEY_IPC_TOKEN,
        PrefConst.KEY_MOBILE_ENTITLEMENT_TOKEN,
        PrefConst.KEY_MOBILE_ENTITLEMENT_AUTOMATION_ALLOWED,
        PrefConst.KEY_ENABLE_ANALYTICS,
        PrefConst.KEY_SMS_CODE_RULE_SOURCE_URL,
        "anonymous_installation_id",
    )

    fun isPortable(key: String): Boolean =
        !key.startsWith("internal_") && key !in excludedKeys
}

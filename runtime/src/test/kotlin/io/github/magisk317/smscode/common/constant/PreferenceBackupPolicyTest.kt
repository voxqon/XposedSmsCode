package io.github.magisk317.smscode.common.constant

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PreferenceBackupPolicyTest {
    @Test
    fun `backups cannot replace IPC identity or restore removed remote controls`() {
        listOf(
            PrefConst.KEY_IPC_TOKEN,
            PrefConst.KEY_MOBILE_ENTITLEMENT_TOKEN,
            PrefConst.KEY_MOBILE_ENTITLEMENT_AUTOMATION_ALLOWED,
            PrefConst.KEY_ENABLE_ANALYTICS,
            PrefConst.KEY_SMS_CODE_RULE_SOURCE_URL,
            "anonymous_installation_id",
            "internal_ipc_token_version",
        ).forEach { key -> assertFalse(PreferenceBackupPolicy.isPortable(key), key) }
    }

    @Test
    fun `local settings remain portable`() {
        assertTrue(PreferenceBackupPolicy.isPortable(PrefConst.KEY_ENABLE))
        assertTrue(PreferenceBackupPolicy.isPortable(PrefConst.KEY_AUTO_INPUT_CODE_DELAY))
        assertTrue(PreferenceBackupPolicy.isPortable(PrefConst.KEY_CHOOSE_THEME))
    }
}

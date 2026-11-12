package io.github.magisk317.smscode.xp.helper

import android.content.Context
import io.github.magisk317.smscode.common.constant.TransitionConst
import io.github.magisk317.smscode.runtime.verification.ModuleConflictArbiterCore

object ModuleConflictArbiter {
    val SUPPRESSION_REASON: String = ModuleConflictArbiterCore.SUPPRESSION_REASON
    val BYPASS_REASON_BUILD_FLAG: String = ModuleConflictArbiterCore.BYPASS_REASON_BUILD_FLAG
    const val TARGET_RELAY_PACKAGE = TransitionConst.TARGET_RELAY_PACKAGE

    // Installing another package must not silently turn off this module.
    @Suppress("UNUSED_PARAMETER")
    fun shouldSuppressByRelay(context: Context?, source: String): Boolean {
        return false
    }
}

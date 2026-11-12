package io.github.magisk317.uikit.surface

/**
 * Interface for detecting module activation status.
 * Each app implements this to provide its own detection logic.
 */
interface ModuleActivationDetector {
    fun detectActivation(): ActivationStatus
}

/**
 * Module activation status.
 */
sealed class ActivationStatus {
    /** Module is not activated by any Xposed framework. */
    data object NotActivated : ActivationStatus()

    /** Module is activated by an Xposed framework. */
    data class Activated(
        val frameworkName: String,
        val frameworkVersion: String = "",
        val apiVersion: Int = -1,
    ) : ActivationStatus()

    /** Activation status is being checked. */
    data object Loading : ActivationStatus()
}

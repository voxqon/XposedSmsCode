package io.github.magisk317.xposed.logging

import android.content.Context

/** Legacy entry point; no installation identifier is created. */
@JvmOverloads
@Suppress("UNUSED_PARAMETER")
fun MagiskOtel.configureForInstallation(
    context: Context,
    config: MagiskOtel.Config,
    preferencesName: String = AnonymousInstallationId.DEFAULT_PREFERENCES_NAME,
) {
    configure(config.copy(enabled = false))
}

/**
 * Mirrors the effective analytics switch into the plain SharedPreferences file that also holds
 * the anonymous installation id. Hook processes cannot read DataStore, but they can read this
 * file through libxposed remote preferences, so the hook side can follow the user's switch.
 */
@Suppress("UNUSED_PARAMETER")
fun MagiskOtel.publishSwitch(context: Context, key: String, enabled: Boolean) {
    runCatching {
        context
            .getSharedPreferences(
                AnonymousInstallationId.DEFAULT_PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )
            .edit()
            .putBoolean(key, false)
            .apply()
    }.onFailure {
        // Best-effort by necessity: the switch mirrors into the hook process, and reporting
        // this through XLog would route straight back into this exporter.
    }
}

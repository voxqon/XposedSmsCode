package io.github.magisk317.uikit.shell

import android.app.Application

/**
 * Interface for application initializers.
 *
 * Apps register implementations (typically via Koin) and run them on
 * [Application.onCreate], e.g.
 * `GlobalContext.get().getAll<AppInitializer>().forEach { it.init(app) }`.
 */
interface AppInitializer {
    fun init(application: Application)
}

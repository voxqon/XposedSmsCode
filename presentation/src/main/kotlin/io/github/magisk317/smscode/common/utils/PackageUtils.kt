package io.github.magisk317.smscode.common.utils

import io.github.magisk317.smscode.runtime.common.utils.ConfiguredPackageActions

/**
 * The values that make package queries app-specific.
 *
 * Everything the queries actually do lives in ConfiguredPackageActions, so callers
 * build one of these and use it directly.
 */
val xscPackageActions: ConfiguredPackageActions = ConfiguredPackageActions()

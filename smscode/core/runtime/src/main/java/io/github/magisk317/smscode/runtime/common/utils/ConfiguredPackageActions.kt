package io.github.magisk317.smscode.runtime.common.utils

import android.content.Context

/** Local package and framework queries used by the UI. */
open class ConfiguredPackageActions {
    fun isPackageInstalled(context: Context, packageName: String): Boolean =
        PackageActions.isPackageInstalled(context, packageName)

    fun isPackageEnabled(context: Context, packageName: String): Boolean =
        PackageActions.isPackageEnabled(context, packageName)

    fun getPackageVersion(context: Context, packageName: String): Pair<String, Long>? =
        PackageActions.getPackageVersion(context, packageName)

    fun resolveFrameworkInfo(context: Context): FrameworkInfo? =
        PackageActions.resolveFrameworkInfo(context)

    fun inspectFrameworkIssue(context: Context): FrameworkCompatibilityMonitor.FrameworkIssue? =
        PackageActions.inspectFrameworkIssue(context)

    fun getLsposedModuleVersion(context: Context): String? =
        PackageActions.getLsposedModuleVersion(context)

    fun getLsposedModuleInfo(context: Context): Pair<String, String>? =
        PackageActions.getLsposedModuleInfo(context)

    fun hasRootAccess(): Boolean = PackageActions.hasRootAccess()

    fun isInstalledFromPlay(context: Context): Boolean = PackageActions.isInstalledFromPlay(context)

}

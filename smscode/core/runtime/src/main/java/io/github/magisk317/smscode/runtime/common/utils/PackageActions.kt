package io.github.magisk317.smscode.runtime.common.utils

import android.content.Context
import io.github.magisk317.smscode.runtime.common.packageenv.PackageEnvCore

/**
 * Local package and framework probes backed by [PackageEnvCore].
 */
object PackageActions {

    @JvmStatic
    fun isPackageInstalled(context: Context, packageName: String): Boolean =
        PackageEnvCore.isPackageInstalled(context, packageName)

    @JvmStatic
    fun isPackageEnabled(context: Context, packageName: String): Boolean =
        PackageEnvCore.isPackageEnabled(context, packageName)

    @JvmStatic
    fun getPackageVersion(context: Context, packageName: String): Pair<String, Long>? {
        val info = PackageEnvCore.getPackageVersion(context, packageName) ?: return null
        return info.versionName to info.versionCode
    }

    @JvmStatic
    fun resolveFrameworkInfo(context: Context): FrameworkInfo? =
        PackageEnvCore.resolveFrameworkInfo(context)

    @JvmStatic
    fun inspectFrameworkIssue(context: Context): FrameworkCompatibilityMonitor.FrameworkIssue? =
        PackageEnvCore.inspectFrameworkIssue(context)

    @JvmStatic
    fun getLsposedModuleVersion(context: Context): String? =
        resolveFrameworkInfo(context)?.displayVersion

    @JvmStatic
    fun getLsposedModuleInfo(context: Context): Pair<String, String>? {
        val info = resolveFrameworkInfo(context) ?: return null
        return info.displayName to info.displayVersion
    }

    @JvmStatic
    fun hasRootAccess(): Boolean = PackageEnvCore.hasRootAccess()

    @JvmStatic
    fun isInstalledFromPlay(context: Context): Boolean = try {
        PackageEnvCore.isInstalledFromPlay(context)
    } catch (_: Exception) {
        false
    }

}

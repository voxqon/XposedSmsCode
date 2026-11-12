package io.github.magisk317.uikit.shell

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

data class AppInfoSnapshot(
    val packageName: String,
    val label: String,
)

object AppInfoHelper {
    fun getAppInfo(pm: PackageManager, packageInfo: PackageInfo): AppInfoSnapshot {
        val appInfo = packageInfo.applicationInfo
        val label = if (appInfo != null) pm.getApplicationLabel(appInfo).toString() else packageInfo.packageName
        val packageName = packageInfo.packageName
        return AppInfoSnapshot(packageName, label)
    }

    fun getAppInfo(pm: PackageManager, applicationInfo: ApplicationInfo): AppInfoSnapshot {
        val label = pm.getApplicationLabel(applicationInfo).toString()
        val packageName = applicationInfo.packageName
        return AppInfoSnapshot(packageName, label)
    }
}

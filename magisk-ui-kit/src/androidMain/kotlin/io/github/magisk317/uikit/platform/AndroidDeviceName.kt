package io.github.magisk317.uikit.platform

import android.annotation.SuppressLint
import android.os.Build

private val DEVICE_DISPLAY_NAME_PROPERTIES = listOf(
    "ro.product.marketname",
    "ro.product.odm.marketname",
    "ro.product.vendor.marketname",
    "ro.product.model",
    "ro.product.system.model",
)

/** Resolves the user-facing device market name with a stable Build.MODEL fallback. */
fun resolveAndroidDeviceDisplayName(): String {
    val propertyValues = DEVICE_DISPLAY_NAME_PROPERTIES.map(::readAndroidSystemProperty)
    return selectAndroidDeviceDisplayName(
        propertyValues = propertyValues,
        fallback = Build.MODEL?.trim().orEmpty(),
    )
}

internal fun selectAndroidDeviceDisplayName(
    propertyValues: List<String?>,
    fallback: String?,
): String = propertyValues
    .firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotEmpty) }
    ?: fallback?.trim()?.takeIf(String::isNotEmpty)
    ?: "Android"

@SuppressLint("PrivateApi")
private fun readAndroidSystemProperty(key: String): String? = runCatching {
    val clazz = Class.forName("android.os.SystemProperties")
    val method = clazz.getMethod("get", String::class.java, String::class.java)
    method.invoke(null, key, "") as? String
}.getOrNull()

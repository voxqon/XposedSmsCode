package io.github.magisk317.uikit.text

/**
 * Masks the middle of a sensitive identifier while preserving a visible prefix and suffix.
 * [maskLength] can shorten the display without changing how much source data remains hidden.
 */
fun maskSensitiveIdentifier(
    value: String?,
    visiblePrefix: Int = 4,
    visibleSuffix: Int = 4,
    expectedLength: Int? = null,
    maskChar: Char = '*',
    maskLength: Int? = null,
): String? {
    require(visiblePrefix >= 0 && visibleSuffix >= 0) { "visible lengths must not be negative" }
    require(visiblePrefix + visibleSuffix > 0) { "at least one visible character is required" }
    require(maskLength == null || maskLength >= 0) { "maskLength must not be negative" }

    val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (expectedLength != null && normalized.length != expectedLength) return null
    if (normalized.length < visiblePrefix + visibleSuffix) return null

    val prefix = normalized.take(visiblePrefix)
    val suffix = normalized.takeLast(visibleSuffix)
    if ((prefix + suffix).all { it == maskChar }) return null

    val hiddenLength = maskLength ?: (normalized.length - visiblePrefix - visibleSuffix)
    return prefix + maskChar.toString().repeat(hiddenLength) + suffix
}

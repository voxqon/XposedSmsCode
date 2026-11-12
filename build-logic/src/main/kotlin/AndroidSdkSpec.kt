internal sealed interface AndroidSdkSpec {
    data class ApiLevel(val value: Int) : AndroidSdkSpec

    data class Preview(val codename: String) : AndroidSdkSpec

    /** Release platform with a minor API level, e.g. `37.1` -> major 37, minor 1. */
    data class Minor(val apiLevel: Int, val minorApiLevel: Int) : AndroidSdkSpec

    /** Beta channel platform, e.g. `37.2-beta3` -> beta 37, minor 2, beta version 3. */
    data class Beta(val apiLevel: Int, val minorApiLevel: Int, val betaVersion: Int) : AndroidSdkSpec
}

private val minorRegex = Regex("^(\\d+)\\.(\\d+)$")
private val betaRegex = Regex("^(\\d+)\\.(\\d+)-beta(\\d+)$")

internal fun parseAndroidSdkSpec(rawValue: String): AndroidSdkSpec {
    val value = rawValue.trim().removePrefix("android-")
    require(value.isNotEmpty()) { "Android SDK value must not be blank" }
    value.toIntOrNull()?.let { return AndroidSdkSpec.ApiLevel(it) }
    minorRegex.find(value)?.groupValues?.let { (_, major, minor) ->
        return AndroidSdkSpec.Minor(major.toInt(), minor.toInt())
    }
    betaRegex.find(value)?.groupValues?.let { (_, major, minor, beta) ->
        return AndroidSdkSpec.Beta(major.toInt(), minor.toInt(), beta.toInt())
    }
    return AndroidSdkSpec.Preview(value)
}

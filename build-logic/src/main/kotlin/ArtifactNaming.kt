internal fun releaseBaseName(
    artifactBaseName: String,
    versionName: String,
): String {
    val base = artifactBaseName.trim().ifEmpty { "app" }.replace("\\s+".toRegex(), "_")
    val version = versionName.trim().replace("\\s+".toRegex(), "_")
    return "${base}_v$version"
}

internal fun releaseApkName(
    artifactBaseName: String,
    versionName: String,
    buildType: String,
    abiSuffix: String,
    flavorName: String = "",
): String {
    // Distribution flavors (play/github in xinyi-relay) share one standard
    // artifact name: the channel is carried by the release pipeline, not by
    // the file name. Non-distribution flavor dimensions (e.g. versionCode
    // tiers) still keep their label.
    val flavorTag = when (flavorName) {
        "", "play", "github" -> ""
        else -> "${flavorName}_"
    }
    return "${abiSuffix}_${flavorTag}${releaseBaseName(artifactBaseName, versionName)}_${buildType}.apk"
}

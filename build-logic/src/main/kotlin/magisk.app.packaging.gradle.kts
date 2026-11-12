import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.FilterConfiguration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun versionStringOrNull(name: String): String? {
    val version = libsCatalog.findVersion(name)
    return if (version.isPresent) version.get().requiredVersion else null
}

// -PversionName must win: local pinned builds and release flows pass it to keep
// the produced APK at an exact version, while the catalog only carries the
// in-development default. The root project resolves it the same way.
val versionNameStr = rootProject.providers.gradleProperty("versionName")
    .orElse(versionStringOrNull("versionName") ?: rootProject.version.toString())
    .get()
fun artifactBaseName(): String = sequenceOf(
    project.findProperty("artifactBaseName")?.toString(),
    project.extensions.extraProperties.properties["artifactBaseName"]?.toString(),
    rootProject.name,
).mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.first()

fun releaseTime(): String = SimpleDateFormat("yyyyMMdd_HHmmss").apply {
    timeZone = TimeZone.getDefault()
}.format(Date())

fun buildTimestampOverride(): String? = sequenceOf(
    project.findProperty("buildTs")?.toString(),
    System.getenv("MAGISK_BUILD_TIMESTAMP"),
).mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.firstOrNull()

/**
 * Normalize the build timestamp into yyyyMMdd_HHmmss so artifact names stay
 * consistent whether callers use Gradle's buildTs property or the CI environment.
 * Unrecognized values fall back to the live clock instead of being embedded raw.
 */
fun normalizeBuildTimestamp(raw: String): String? {
    val compact = raw.filter { it.isDigit() }
    if (compact.length != 14) return null
    val date = compact.substring(0, 8)
    val time = compact.substring(8, 14)
    if (!date.all(Char::isDigit) || !time.all(Char::isDigit)) return null
    return "${date}_${time}"
}

fun buildTimestamp(): String =
    buildTimestampOverride()?.let { normalizeBuildTimestamp(it) } ?: releaseTime()

val debugBuildTimestamp = buildTimestamp()
val includesBundleRequest = requestsApplicationBundle(gradle.startParameter.taskNames, project.path)
val isBundleOnlyInvocation = requestsOnlyApplicationBundles(gradle.startParameter.taskNames, project.path)

pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        splits {
            abi {
                isEnable = project.hasProperty("buildSplits") && !includesBundleRequest
                reset()
                include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                isUniversalApk = true
            }
        }
    }

    tasks.matching {
        it.name.startsWith("assemble") && it.name.contains("Play")
    }.configureEach {
        enabled = false
    }

    tasks.matching {
        it.name.startsWith("packagePlay") &&
            !it.name.contains("Bundle") &&
            !it.name.endsWith("Resources") &&
            !it.name.contains("AndroidTest") &&
            !it.name.contains("UnitTest")
    }.configureEach {
        enabled = false
    }

    tasks.matching {
        it.name.startsWith("createPlay") &&
            it.name.endsWith("ApkListingFileRedirect")
    }.configureEach {
        enabled = false
    }

    tasks.matching {
        it.name.startsWith("bundle")
    }.configureEach {
        if (!name.contains("Play") && !name.contains("ClassesTo") && !name.contains("Release")) {
            enabled = false
        }
    }

    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        beforeVariants(selector().all()) { variantBuilder ->
            val flavors = variantBuilder.productFlavors.toMap()
            val distribution = flavors["distribution"]
            if (isBundleOnlyInvocation && distribution != "play" && distribution != null) {
                variantBuilder.enable = false
            }
        }

        onVariants(selector().all()) { variant ->
            val isDebug = variant.buildType == "debug"
            val suffix = if (isDebug) "-$debugBuildTimestamp" else ""
            val resolvedVersionName = "$versionNameStr$suffix"
            
            variant.outputs.forEach { output ->
                if (isDebug) {
                    output.versionName.set(resolvedVersionName)
                }

                val abi = output.filters.find {
                    it.filterType == FilterConfiguration.FilterType.ABI
                }?.identifier ?: "universal"

                output.outputFileName.set(
                    releaseApkName(
                        artifactBaseName = artifactBaseName(),
                        versionName = resolvedVersionName,
                        buildType = variant.buildType ?: "",
                        abiSuffix = abi,
                        flavorName = variant.flavorName ?: "",
                    ),
                )
            }
        }
    }
}

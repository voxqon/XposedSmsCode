import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Marks a build as an internal/CI build: these keep the runtime-log diagnostics
 * available (log export, verbose logging), while public
 * release builds strip logging for performance and expose a placeholder entry.
 *
 * Enabled with -PisCiBuild=true or MAGISK_IS_CI_BUILD=true. Never inferred from
 * the version name, so public releases can never be misdetected as CI builds.
 */
val magiskIsCiBuildValue: String =
    (
        providers.gradleProperty("isCiBuild").orNull
            ?: providers.environmentVariable("MAGISK_IS_CI_BUILD").orNull
        )?.toBooleanStrictOrNull()?.toString() ?: "false"

/**
 * Keeps log text and exported bundles unsanitized.
 *
 * Enabled with -PallowPlaintextLogs=true or MAGISK_ALLOW_PLAINTEXT_LOGS=true. It exists for a
 * hand-built run that must show raw SMS or notification content while staying release-shaped;
 * distributed builds never set it, so their bundles keep redacting credentials and phone
 * numbers. It relaxes sanitization only — it does not re-enable stripped logging.
 */
val magiskAllowPlaintextLogsValue: String =
    (
        providers.gradleProperty("allowPlaintextLogs").orNull
            ?: providers.environmentVariable("MAGISK_ALLOW_PLAINTEXT_LOGS").orNull
        )?.toBooleanStrictOrNull()?.toString() ?: "false"

/**
 * Commit hash of the repository the module itself is checked out from.
 *
 * All local modules use this repository's commit for diagnostic provenance.
 */
val magiskModuleCommitHashValue: String = providers
    .of(GitCommitHashValueSource::class.java) {
        parameters.gitProjectDir.set(projectDir.absolutePath)
    }
    .get()

fun com.android.build.api.dsl.ApplicationExtension.configureMagiskCiBuildFlag() {
    buildFeatures { buildConfig = true }
    defaultConfig { buildConfigField("boolean", "IS_CI_BUILD", magiskIsCiBuildValue) }
    defaultConfig { buildConfigField("boolean", "ALLOW_PLAINTEXT_LOGS", magiskAllowPlaintextLogsValue) }
    defaultConfig { buildConfigField("String", "MODULE_COMMIT_HASH", "\"$magiskModuleCommitHashValue\"") }
}

fun com.android.build.api.dsl.LibraryExtension.configureMagiskCiBuildFlag() {
    buildFeatures { buildConfig = true }
    defaultConfig { buildConfigField("boolean", "IS_CI_BUILD", magiskIsCiBuildValue) }
    defaultConfig { buildConfigField("boolean", "ALLOW_PLAINTEXT_LOGS", magiskAllowPlaintextLogsValue) }
    defaultConfig { buildConfigField("String", "MODULE_COMMIT_HASH", "\"$magiskModuleCommitHashValue\"") }
}

fun com.android.build.api.dsl.DynamicFeatureExtension.configureMagiskCiBuildFlag() {
    buildFeatures { buildConfig = true }
    defaultConfig { buildConfigField("boolean", "IS_CI_BUILD", magiskIsCiBuildValue) }
    defaultConfig { buildConfigField("boolean", "ALLOW_PLAINTEXT_LOGS", magiskAllowPlaintextLogsValue) }
    defaultConfig { buildConfigField("String", "MODULE_COMMIT_HASH", "\"$magiskModuleCommitHashValue\"") }
}

fun CommonExtension.configureMagiskAndroidCommon() {
    compileSdk(project.magiskCompileSdk())
    project.catalogIntOrNull("compileSdkExtension")?.let { compileSdkExtension = it }
    project.catalogVersionOrNull("buildTools")?.let { buildToolsVersion = it }
    project.catalogVersionOrNull("ndk")?.let { ndkVersion = it }

    val javaVersion = JavaVersion.toVersion(project.magiskJavaTarget())
    compileOptions.sourceCompatibility = javaVersion
    compileOptions.targetCompatibility = javaVersion
}

configureMagiskKotlinJvmTarget()

pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        configureMagiskAndroidCommon()
        configureMagiskCiBuildFlag()
    }
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> {
        configureMagiskAndroidCommon()
        configureMagiskCiBuildFlag()
    }
}

pluginManager.withPlugin("com.android.dynamic-feature") {
    extensions.configure<DynamicFeatureExtension> {
        configureMagiskAndroidCommon()
        configureMagiskCiBuildFlag()
    }
}

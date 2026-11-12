import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.configure
import java.util.Properties

fun resolveSigningFile(
    explicitPath: String?,
    fallbackPaths: List<String>,
): java.io.File {
    if (!explicitPath.isNullOrBlank()) {
        return rootProject.file(explicitPath)
    }
    return fallbackPaths
        .asSequence()
        .map(rootProject::file)
        .firstOrNull { it.exists() }
        ?: rootProject.file(fallbackPaths.first())
}

val explicitKeystorePath = providers.gradleProperty("magisk.signing.keystore").orNull
val explicitPropertiesPath = providers.gradleProperty("magisk.signing.properties").orNull
val keyFile = resolveSigningFile(
    explicitPath = explicitKeystorePath ?: System.getenv("KEYSTORE_FILE"),
    fallbackPaths = listOf("release.jks", "app/release.jks"),
)
val propertyFile = resolveSigningFile(
    explicitPath = explicitPropertiesPath,
    fallbackPaths = listOf("signature.properties", "app/signature.properties"),
)

val keyProps = Properties().apply {
    if (propertyFile.exists()) {
        propertyFile.inputStream().use(::load)
    }
}

val useExplicitProperties = !explicitPropertiesPath.isNullOrBlank()
val storePasswordVal = if (useExplicitProperties) {
    keyProps.getProperty("STORE_PASSWORD")
} else {
    System.getenv("STORE_PASSWORD")?.takeIf(String::isNotBlank)
        ?: keyProps.getProperty("STORE_PASSWORD")
}
val keyAliasVal = if (useExplicitProperties) {
    keyProps.getProperty("KEY_ALIAS")
} else {
    System.getenv("KEY_ALIAS")?.takeIf(String::isNotBlank)
        ?: keyProps.getProperty("KEY_ALIAS")
}
val keyPasswordVal = if (useExplicitProperties) {
    keyProps.getProperty("KEY_PASSWORD")
} else {
    System.getenv("KEY_PASSWORD")?.takeIf(String::isNotBlank)
        ?: keyProps.getProperty("KEY_PASSWORD")
}
val isSigningInfoAvailable = keyFile.isFile &&
    !storePasswordVal.isNullOrBlank() &&
    !keyAliasVal.isNullOrBlank() &&
    !keyPasswordVal.isNullOrBlank()

pluginManager.withPlugin("com.android.application") {
    if (requiresReleaseSigning(gradle.startParameter.taskNames, project.path) && !isSigningInfoAvailable) {
        throw GradleException(
            "Release APK/AAB packaging requires complete release signing. " +
                "Configure KEYSTORE_FILE, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD."
        )
    }

    extensions.configure<ApplicationExtension> {
        val releaseSigningConfig = signingConfigs.maybeCreate("release").apply {
            storeFile = keyFile
            storePassword = storePasswordVal
            keyAlias = keyAliasVal
            keyPassword = keyPasswordVal
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }

        buildTypes.getByName("release") {
            signingConfig = releaseSigningConfig
        }
    }
}

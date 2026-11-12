import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.application")
    id("magisk.android.common")
}

extensions.configure<ApplicationExtension> {
    val magiskIsCiBuildForProguard: Boolean = (project.providers.gradleProperty("isCiBuild").orNull
        ?: project.providers.environmentVariable("MAGISK_IS_CI_BUILD").orNull)?.toBooleanStrictOrNull() ?: false
    val projectProguardFile = sequenceOf("proguard-rules.pro", "proguard-common.pro")
        .firstOrNull { project.file(it).isFile }

    defaultConfig {
        when (val spec = parseAndroidSdkSpec(project.propertyOrCatalogVersion("minSdk", "minSdk"))) {
            is AndroidSdkSpec.ApiLevel -> minSdk = spec.value
            is AndroidSdkSpec.Minor -> minSdk = spec.apiLevel
            is AndroidSdkSpec.Beta -> minSdk = spec.apiLevel
            is AndroidSdkSpec.Preview -> {
                @Suppress("DEPRECATION")
                minSdkPreview = spec.codename
            }
        }
        when (val spec = parseAndroidSdkSpec(project.catalogVersion("targetSdk"))) {
            is AndroidSdkSpec.ApiLevel -> targetSdk = spec.value
            is AndroidSdkSpec.Minor -> targetSdk = spec.apiLevel
            is AndroidSdkSpec.Beta -> targetSdk = spec.apiLevel
            is AndroidSdkSpec.Preview -> targetSdkPreview = spec.codename
        }
    }

    buildTypes {
        debug {
            val optimization = debugOptimization(
                providers.gradleProperty("minifyDebug").orNull,
            )
            isMinifyEnabled = optimization.isMinifyEnabled
            isShrinkResources = optimization.isShrinkResources
            isDebuggable = optimization.isDebuggable
            if (optimization.isMinifyEnabled) {
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                projectProguardFile?.let(::proguardFile)
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            projectProguardFile?.let(::proguardFile)
            // Release-only rules (e.g. assumenosideeffects for verbose logging)
            // live here so debug builds, including -PminifyDebug, keep them.
            // CI builds keep full verbosity on purpose (they are the log-capture
            // channel), so the verbose-stripping rules are skipped for them.
            if (!magiskIsCiBuildForProguard) {
                project.file("proguard-release.pro").takeIf { it.isFile }?.let(::proguardFile)
            }
        }
    }

    project.catalogVersionOrNull("cmake")?.let { cmakeVer ->
        externalNativeBuild {
            cmake {
                version = cmakeVer
            }
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

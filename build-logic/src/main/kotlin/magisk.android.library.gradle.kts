import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.library")
    id("magisk.android.common")
}

extensions.configure<LibraryExtension> {
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
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    project.catalogVersionOrNull("cmake")?.let { cmakeVer ->
        externalNativeBuild {
            cmake {
                version = cmakeVer
            }
        }
    }
}

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.android.")) {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
    repositories {
        if (providers.gradleProperty("useLocalMaven").orNull != "false") {
            maven { url = uri("../third_party/maven") }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (providers.gradleProperty("useLocalMaven").orNull != "false") {
            maven { url = uri("../third_party/maven") }
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            val catalog = file("../gradle/libs.versions.toml")
            check(catalog.isFile) {
                "Local build logic requires ../gradle/libs.versions.toml"
            }
            from(files(catalog))
        }
    }
}

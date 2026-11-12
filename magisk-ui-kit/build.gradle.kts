plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id(libs.plugins.kotlin.compose.get().pluginId)
}

kotlin {
    jvm()

    android {
        namespace = "io.github.magisk317.uikit"
        compileSdk(project.magiskCompileSdk())
        minSdk = libs.versions.minSdk.get().toInt()
        androidResources {
            enable = true
            resourcePrefix = "io.github.magisk317.uikit"
        }
        // Opt the android target's host (JVM-side unit test) compilation in
        // explicitly. Left out, the unit-test source set stays inert and no
        // unit-test task is registered for the android target at all -- KGP
        // then warns the source set is "configured but not added to any
        // Kotlin compilation".
        withHostTest { }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose artifacts carry android + jvm-stubs variants (the desktop
            // runtime comes from org.jetbrains.compose at the app level).
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.foundation)
            implementation(libs.androidx.compose.runtime)
            implementation(libs.androidx.compose.ui)
            implementation("androidx.compose.animation:animation")
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.material.icons.extended)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.materialKolor)
            api(libs.miuix)
            api(libs.miuixPreference)
        }
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.activity.compose)

            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.miuixBlur)
        }
        // The KMP move left the historical src/test tree inert: neither target wires it.
        // Pure-JVM suites sit in jvmTest; suites that touch androidMain declarations sit
        // in androidHostTest (the compilation opted in via withHostTest {} above). Both
        // need JUnit 5.
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            runtimeOnly(libs.junit.platform.launcher)
        }
        // withHostTest compiles a source set AGP names "androidHostTest"; unlike
        // KGP's own default source sets it has no typed accessor, so wire its
        // JUnit 5 dependencies through the container by name.
        getByName("androidHostTest").dependencies {
            implementation(libs.junit.jupiter)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

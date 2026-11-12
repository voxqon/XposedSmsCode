import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

val javaVersion = JavaVersion.toVersion(magiskJavaTarget())
extensions.configure<JavaPluginExtension> {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
configureMagiskKotlinJvmTarget()

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

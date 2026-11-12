import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

plugins {
    id("com.google.devtools.ksp")
}

extensions.configure<KspExtension> {
    arg("room.schemaLocation", "${project.projectDir}/schemas")
    arg("room.incremental", "true")
}

dependencies {
    add("implementation", project.catalogLibrary("androidx-room-runtime"))
    add("implementation", project.catalogLibrary("androidx-room-ktx"))
    add("ksp", project.catalogLibrary("androidx-room-compiler"))
}

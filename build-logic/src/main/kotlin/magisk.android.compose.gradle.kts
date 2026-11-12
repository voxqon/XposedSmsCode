import org.gradle.kotlin.dsl.dependencies

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    add("implementation", platform(project.catalogLibrary("androidx-compose-bom")))
    add("implementation", project.catalogLibrary("androidx-compose-ui"))
    add("implementation", project.catalogLibrary("androidx-compose-material3"))
    add("implementation", project.catalogLibrary("androidx-compose-ui-tooling-preview"))
    val includeDebugTooling = project.findProperty("includeComposeToolingInDebugApk")
        ?.toString()
        ?.toBooleanStrictOrNull()
        ?: false
    if (includeDebugTooling) {
        add("debugImplementation", project.catalogLibrary("androidx-compose-ui-tooling"))
    }
    add("implementation", project.catalogLibrary("androidx-lifecycle-runtime-ktx"))
    add("implementation", project.catalogLibrary("androidx-activity-compose"))
    add("implementation", project.catalogLibrary("androidx-navigation-compose"))
    add(
        "implementation",
        project.catalogLibrary("androidx-lifecycle-viewmodel-compose"),
    )
    add(
        "implementation",
        project.catalogLibrary("androidx-lifecycle-runtime-compose"),
    )
}

import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class KotlinLibraryPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `provides java library API semantics`() {
        projectDir.resolve("settings.gradle.kts").write(
            """
            rootProject.name = "kotlin-library-test"
            """.trimIndent(),
        )
        projectDir.resolve("gradle/libs.versions.toml").write(
            """
            [versions]
            javaBytecode = "21"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").write(
            """
            plugins {
                id("magisk.kotlin.library")
            }

            tasks.register("printLibraryModel") {
                doLast {
                    println("JAVA_LIBRARY ${'$'}{pluginManager.hasPlugin("java-library")}")
                    println("API_CONFIGURATION ${'$'}{configurations.findByName("api") != null}")
                    val java = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
                    println("JAVA_SOURCE ${'$'}{java.sourceCompatibility}")
                    println("JAVA_TARGET ${'$'}{java.targetCompatibility}")
                }
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("printLibraryModel", "--stacktrace")
            .build()

        assertTrue(result.output.contains("JAVA_LIBRARY true"), result.output)
        assertTrue(result.output.contains("API_CONFIGURATION true"), result.output)
        assertTrue(result.output.contains("JAVA_SOURCE 21"), result.output)
        assertTrue(result.output.contains("JAVA_TARGET 21"), result.output)
    }

    private fun Path.write(content: String) {
        parent?.let(Files::createDirectories)
        Files.writeString(this, content)
    }
}

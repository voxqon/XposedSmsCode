import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MaintenancePluginFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `configures native test logging for subprojects`() {
        projectDir.resolve("settings.gradle.kts").write(
            """
            rootProject.name = "maintenance-test"
            include(":sample")
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle").write(
            """
            plugins {
                id 'magisk.maintenance'
            }

            tasks.register('printTestLogging') {
                doLast {
                    def logging = project(':sample').tasks.named('test').get().testLogging
                    println("TEST_EVENTS ${'$'}{logging.events.toList().sort()}")
                    println("TEST_EXCEPTION_FORMAT ${'$'}{logging.exceptionFormat}")
                    def test = project(':sample').tasks.named('test').get()
                    println("TEST_HEAP ${'$'}{test.maxHeapSize}")
                    println("TEST_FORK_EVERY ${'$'}{test.forkEvery}")
                    println("TEST_JVM_ARGS ${'$'}{test.jvmArgs}")
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("sample/build.gradle").write(
            """
            plugins {
                id 'java'
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("printTestLogging", "--stacktrace")
            .build()

        assertTrue(result.output.contains("TEST_EVENTS [PASSED, SKIPPED, FAILED]"), result.output)
        assertTrue(result.output.contains("TEST_EXCEPTION_FORMAT FULL"), result.output)
        assertTrue(result.output.contains("TEST_HEAP 2g"), result.output)
        assertTrue(result.output.contains("TEST_FORK_EVERY 50"), result.output)
        assertTrue(result.output.contains("-XX:+EnableDynamicAgentLoading"), result.output)
    }

    @Test
    fun `cache cleanup is explicit and removes every stale Gradle version`() {
        projectDir.resolve("settings.gradle.kts").write("rootProject.name = \"maintenance-test\"")
        projectDir.resolve("build.gradle").write(
            """
            plugins {
                id 'base'
                id 'magisk.maintenance'
            }
            """.trimIndent(),
        )
        val staleCache = projectDir.resolve(".gradle/1.0")
        val currentCache = projectDir.resolve(".gradle/${GradleVersion.current().version}")
        Files.createDirectories(staleCache)
        Files.createDirectories(currentCache)

        runner("assemble").build()

        assertTrue(Files.isDirectory(staleCache))
        runner("cleanupGradleCaches").build()
        assertFalse(Files.exists(staleCache))
        assertTrue(Files.isDirectory(currentCache))
    }

    private fun runner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")

    private fun Path.write(content: String) {
        parent?.let(Files::createDirectories)
        Files.writeString(this, content)
    }
}

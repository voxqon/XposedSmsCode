import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

fun Test.configureMagiskTestRuntime() {
    useJUnitPlatform()
    maxHeapSize = "2g"
    forkEvery = 50
    jvmArgs(
        "-Xshare:off",
        "-XX:+TieredCompilation",
        "-XX:TieredStopAtLevel=1",
        "--enable-native-access=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
    )
    if (Runtime.version().feature() >= 24) {
        jvmArgs("--sun-misc-unsafe-memory-access=allow")
    }
    testLogging {
        events(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = false
    }
}

tasks.register("cleanupGradleCaches") {
    group = "maintenance"
    description = "Remove stale Gradle version caches under .gradle directories."
    val workspaceDir = layout.projectDirectory.asFile
    val currentGradleVersion = gradle.gradleVersion

    doLast {
        val gradleDirs = workspaceDir.walk()
            .maxDepth(3)
            .filter { it.isDirectory && it.name == ".gradle" }
            .toList()

        if (gradleDirs.isEmpty()) {
            println("No .gradle directories found to clean.")
            return@doLast
        }

        gradleDirs.forEach { dir ->
            val staleVersions = dir.listFiles { file ->
                file.isDirectory && file.name.firstOrNull()?.isDigit() == true && file.name != currentGradleVersion
            }.orEmpty()

            staleVersions.forEach { staleVersion ->
                val relativePath = staleVersion.parentFile.relativeTo(workspaceDir)
                println("Cleaning up old Gradle cache in $relativePath: ${staleVersion.name}")
                staleVersion.deleteRecursively()
            }
        }
    }
}

if (project == rootProject) {
    allprojects {
        tasks.withType<Test>().configureEach {
            configureMagiskTestRuntime()
        }
    }
}

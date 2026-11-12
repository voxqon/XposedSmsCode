import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AndroidApplicationPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @BeforeEach
    fun createConsumerProject() {
        projectDir.resolve("gradle.properties").write(
            """
            android.builder.sdkDownload=false
            org.gradle.java.installations.auto-download=false
            """.trimIndent(),
        )
        projectDir.resolve("settings.gradle.kts").write(
            """
            pluginManagement {
                repositories {
                    maven { url = uri("${System.getProperty("offline.maven").replace("\\", "/")}") }
                }
            }
            dependencyResolutionManagement {
                repositories {
                    maven { url = uri("${System.getProperty("offline.maven").replace("\\", "/")}") }
                }
            }
            rootProject.name = "debug-policy-test"
            include("library")
            """.trimIndent(),
        )
        projectDir.resolve("gradle/libs.versions.toml").write(
            """
            [versions]
            compileSdk = "37.2"
            minSdk = "26"
            targetSdk = "37"
            javaBytecode = "26"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle").write(
            """
            plugins {
                id 'magisk.android.application'
                id 'magisk.app.signing'
            }

            android {
                namespace = 'io.github.magisk317.debugpolicytest'
            }

            tasks.register('printDebugPolicy') {
                doLast {
                    def debug = android.buildTypes.getByName('debug')
                    println("DEBUG_POLICY minify=${'$'}{debug.minifyEnabled} " +
                        "shrink=${'$'}{debug.shrinkResources} debuggable=${'$'}{debug.debuggable}")
                }
            }

            tasks.register('printReleasePolicy') {
                doLast {
                    def release = android.buildTypes.getByName('release')
                    println("RELEASE_POLICY minify=${'$'}{release.minifyEnabled} " +
                        "shrink=${'$'}{release.shrinkResources}")
                }
            }

            android.buildTypes.create('alpha') {
                initWith(android.buildTypes.getByName('release'))
                matchingFallbacks = ['release']
            }

            tasks.register('printSigningPolicy') {
                doLast {
                    def debug = android.buildTypes.getByName('debug')
                    def alpha = android.buildTypes.getByName('alpha')
                    def release = android.buildTypes.getByName('release')
                    println("SIGNING_POLICY debug=${'$'}{debug.signingConfig.name} " +
                        "release=${'$'}{release.signingConfig.name} " +
                        "alpha=${'$'}{alpha.signingConfig.name}")
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("src/main/AndroidManifest.xml").write(
            "<manifest />",
        )
        projectDir.resolve("proguard-rules.pro").write("")
        projectDir.resolve("library/build.gradle").write(
            """
            plugins {
                id 'magisk.android.library'
            }

            android {
                namespace = 'io.github.magisk317.debugpolicytest.library'
            }
            """.trimIndent(),
        )
        projectDir.resolve("library/src/main/AndroidManifest.xml").write("<manifest />")
        createSigningConfig()
    }

    @Test
    fun `ordinary debug remains debuggable`() {
        val result = runner("printDebugPolicy").build()

        assertTrue(
            result.output.contains(
                "DEBUG_POLICY minify=false shrink=false debuggable=true",
            ),
        )
    }

    @Test
    fun `release enables code and resource shrinking`() {
        val result = runner("printReleasePolicy").build()

        assertTrue(
            result.output.contains(
                "RELEASE_POLICY minify=true shrink=true",
            ),
        )
    }

    @Test
    fun `debug uses local debug signing and release retains release signing`() {
        val result = runner("printSigningPolicy").build()

        assertTrue(
            result.output.contains(
                "SIGNING_POLICY debug=debug release=release",
            ),
        )
    }

    @Test
    fun `minified debug is shrunk and non-debuggable without AGP warning`() {
        val result = runner(
            "assembleDebug",
            "printDebugPolicy",
            "-PminifyDebug=true",
            "--warning-mode=all",
        ).build()

        assertTrue(
            result.output.contains(
                "DEBUG_POLICY minify=true shrink=true debuggable=false",
            ),
        )
        assertFalse(result.output.contains("DEBUGGABLE_AND_MINIFIED_ENABLED"))
        assertTrue(Files.isRegularFile(projectDir.resolve("build/outputs/apk/debug/debug-policy-test-debug.apk")))
    }

    @Test
    fun `debug packaging works without release signing`() {
        deleteSigningConfig()

        val result = unsignedRunner("assembleDebug").build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `release packaging refuses missing release signing`() {
        deleteSigningConfig()

        val result = unsignedRunner("assembleRelease").buildAndFail()

        assertTrue(result.output.contains("APK/AAB packaging requires complete release signing"))
    }

    @Test
    fun `library packaging does not require application signing`() {
        deleteSigningConfig()

        val result = runner(":library:assembleDebug").build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    private fun runner(vararg arguments: String): GradleRunner {
        val effectiveArguments = arguments.toMutableList()
        System.getenv("MAGISK_ANDROID_AAPT2_OVERRIDE")
            ?.takeIf(String::isNotBlank)
            ?.let { aapt2Path ->
                effectiveArguments += "-Pandroid.aapt2FromMavenOverride=$aapt2Path"
            }

        effectiveArguments += "-Pmagisk.signing.keystore=${projectDir.resolve("release.jks")}"
        effectiveArguments += "-Pmagisk.signing.properties=${projectDir.resolve("signature.properties")}"

        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(*effectiveArguments.toTypedArray(), "--offline", "--stacktrace")
            .forwardOutput()
    }

    private fun unsignedRunner(vararg arguments: String): GradleRunner = runner(*arguments)

    private fun createSigningConfig() {
        val keytool = Path.of(
            System.getProperty("java.home"),
            "bin",
            if (System.getProperty("os.name").startsWith("Windows")) "keytool.exe" else "keytool",
        )
        val process = ProcessBuilder(
            keytool.toString(),
            "-genkeypair",
            "-alias", "test",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "3650",
            "-dname", "CN=Signing Policy Test",
            "-keystore", projectDir.resolve("release.jks").toString(),
            "-storetype", "PKCS12",
            "-storepass", "changeit",
            "-keypass", "changeit",
            "-noprompt",
        )
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { "Failed to create test keystore: $output" }
        projectDir.resolve("signature.properties").write(
            """
            STORE_PASSWORD=changeit
            KEY_ALIAS=test
            KEY_PASSWORD=changeit
            """.trimIndent(),
        )
    }

    private fun deleteSigningConfig() {
        Files.deleteIfExists(projectDir.resolve("release.jks"))
        Files.deleteIfExists(projectDir.resolve("signature.properties"))
    }

    private fun Path.write(content: String) {
        parent?.let(Files::createDirectories)
        Files.writeString(this, content)
    }
}

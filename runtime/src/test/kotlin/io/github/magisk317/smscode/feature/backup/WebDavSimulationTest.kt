package io.github.magisk317.smscode.feature.backup

import io.github.magisk317.smscode.runtime.WebDavConfig
import io.github.magisk317.smscode.runtime.WebDavHttpException
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class WebDavSimulationTest {
    @TempDir
    lateinit var directory: Path
    private lateinit var simulationUrl: String

    @BeforeEach
    fun requireSimulation() {
        val url = System.getenv("WEBDAV_SIMULATION_URL")
        assumeTrue(!url.isNullOrEmpty(), "Start the local Postman WebDAV simulation to run this test")
        simulationUrl = requireNotNull(url)
    }

    private fun client(scope: String, password: String = "test-password"): WebDavClient =
        WebDavClient(WebDavConfig("$simulationUrl/$scope", "test-user", password), allowLocalHttp = true)

    @Test
    fun `real client creates uploads lists and downloads through simulation`() {
        val client = client("roundtrip")
        val name = "simulation-${java.util.UUID.randomUUID()}.scebak"
        val file = directory.resolve("source").toFile().apply { writeText("""{"schemaVersion":1}""") }
        client.upload(file, name)
        assertEquals(name, client.list().first { it.name == name }.name)
        val downloaded = directory.resolve("downloaded").toFile()
        client.download(name, downloaded)
        assertEquals(file.readText(), downloaded.readText())
        // A second unique file also exercises MKCOL 405 and Depth 0 collection verification.
        client.upload(file, "second-$name")
        assertEquals(2, client.list().count { it.name.endsWith(name) })
    }

    @Test
    fun `authentication and server write failures remain failures`() {
        assertEquals(401, assertThrows(WebDavHttpException::class.java) { client("roundtrip", "wrong").list() }.statusCode)
        val source = directory.resolve("source").toFile().apply { writeText("backup") }
        assertEquals(507, assertThrows(WebDavHttpException::class.java) {
            client("quota").upload(source, "full.scebak")
        }.statusCode)
        val destination = directory.resolve("download").toFile()
        assertEquals(500, assertThrows(WebDavHttpException::class.java) {
            client("broken-download").download("backup.zip", destination)
        }.statusCode)
        assertFalse(destination.exists())
    }
}

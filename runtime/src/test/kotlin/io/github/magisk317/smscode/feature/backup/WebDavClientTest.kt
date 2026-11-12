package io.github.magisk317.smscode.feature.backup

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.magisk317.smscode.runtime.WebDavConfig
import io.github.magisk317.smscode.runtime.WebDavHttpException
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.Base64
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class WebDavClientTest {
    @TempDir
    lateinit var directory: Path
    private lateinit var server: HttpServer
    private val requests = mutableListOf<String>()
    private var handler: (HttpExchange) -> Unit = { it.reply(501) }
    private val base get() = "http://127.0.0.1:${server.address.port}/dav"
    private fun client() = WebDavClient(WebDavConfig(base, "user", "secret"), allowLocalHttp = true)

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests += "${exchange.requestMethod} ${exchange.requestURI.rawPath}"
            try {
                handler(exchange)
            } finally {
                exchange.close()
            }
        }
        server.start()
    }

    @AfterEach
    fun stop() = server.stop(0)

    @Test
    fun `uploads into new folder with encoded name authentication and overwrite protection`() {
        val source = directory.resolve("backup").toFile().apply { writeText("backup-data") }
        handler = {
            assertEquals(
                "Basic " + Base64.getEncoder().encodeToString("user:secret".toByteArray()),
                it.requestHeaders.getFirst("Authorization"),
            )
            when (it.requestMethod) {
                "MKCOL" -> it.reply(201)
                "PUT" -> {
                    assertEquals("*", it.requestHeaders.getFirst("If-None-Match"))
                    assertEquals("backup-data", it.requestBody.readBytes().toString(Charsets.UTF_8))
                    it.reply(201)
                }
                else -> it.reply(501)
            }
        }
        client().upload(source, "a + b.scebak")
        assertEquals(listOf(
            "MKCOL /dav/XposedSmsCode/",
            "PUT /dav/XposedSmsCode/a%20+%20b.scebak",
        ), requests)
    }

    @Test
    fun `existing directory must be confirmed as a collection before upload`() {
        handler = {
            when (it.requestMethod) {
                "MKCOL" -> it.reply(405)
                "PROPFIND" -> {
                    assertEquals("0", it.requestHeaders.getFirst("Depth"))
                    it.reply(207, multistatus(entry("/dav/XposedSmsCode", collection = true)))
                }
                "PUT" -> it.reply(204)
                else -> it.reply(501)
            }
        }
        client().upload(directory.resolve("source").toFile().apply { writeText("data") }, "backup.zip")
        assertEquals(listOf("MKCOL", "PROPFIND", "PUT"), requests.map { it.substringBefore(' ') })
    }

    @Test
    fun `405 for a non collection does not permit upload`() {
        handler = {
            if (it.requestMethod == "MKCOL") it.reply(405)
            else it.reply(207, multistatus(entry("/dav/XposedSmsCode")))
        }
        assertThrows(IOException::class.java) {
            client().upload(directory.resolve("source").toFile(), "backup.zip")
        }
        assertFalse(requests.any { it.startsWith("PUT") })
    }

    @Test
    fun `authentication and write failures propagate without upload or automatic retry`() {
        handler = { it.reply(401) }
        val error = assertThrows(WebDavHttpException::class.java) {
            client().upload(directory.resolve("source").toFile(), "backup.scebak")
        }
        assertEquals(401, error.statusCode)
        assertEquals(1, requests.size)
    }

    @Test
    fun `list resolves names skips invalid entries and sorts newest first`() {
        handler = {
            assertEquals("PROPFIND", it.requestMethod)
            assertEquals("1", it.requestHeaders.getFirst("Depth"))
            assertTrue(it.requestBody.readBytes().toString(Charsets.UTF_8).contains("getlastmodified"))
            it.reply(207, multistatus(
                entry("/dav/XposedSmsCode/", collection = true) +
                    entry("old.scebak", modified = "Thu, 29 Oct 2026 00:00:00 GMT") +
                    entry("new%20%2B%20backup.zip", modified = "Fri, 30 Oct 2026 00:00:00 GMT") +
                    entry("subfolder.scebak/", collection = true) +
                    entry("nested/file.scebak") + entry("../escape.scebak") +
                    entry("https://other.example/dav/XposedSmsCode/foreign.scebak") +
                    entry("invalid%2Fname.scebak") + entry("notes.txt") +
                    entry("failed.scebak", status = 404),
            ))
        }
        val files = client().list()
        assertEquals(listOf("new + backup.zip", "old.scebak"), files.map { it.name })
        assertEquals(42L, files.first().size)
        assertTrue(files.first().modifiedAt!! > files.last().modifiedAt!!)
    }

    @Test
    fun `missing folder is an empty list while permission failure is not`() {
        handler = { it.reply(404) }
        assertTrue(client().list().isEmpty())
        handler = { it.reply(403) }
        assertEquals(403, assertThrows(WebDavHttpException::class.java) { client().list() }.statusCode)
    }

    @Test
    fun `malformed xml and external entities are rejected`() {
        handler = { it.reply(207, "<multistatus>broken") }
        assertThrows(IOException::class.java) { client().list() }
        handler = {
            it.reply(207, """<!DOCTYPE d:multistatus [<!ENTITY e SYSTEM "file:///never-read">]>
                <d:multistatus xmlns:d="DAV:">&e;</d:multistatus>""")
        }
        assertThrows(IOException::class.java) { client().list() }
    }

    @Test
    fun `oversized file listing is rejected`() {
        handler = { it.reply(207, " ".repeat(4 * 1024 * 1024 + 1)) }
        assertThrows(IOException::class.java) { client().list() }
    }

    @Test
    fun `download streams bytes and rejects paths outside backup directory`() {
        handler = { it.reply(200, "downloaded-backup") }
        val destination = directory.resolve("download").toFile()
        client().download("backup.scebak", destination)
        assertEquals("downloaded-backup", destination.readText())
        assertThrows(IllegalArgumentException::class.java) { client().download("../backup.scebak", destination) }
        assertThrows(IllegalArgumentException::class.java) { client().download("sub/backup.scebak", destination) }
        assertEquals(1, requests.size)
    }

    @Test
    fun `failed and oversized downloads remove partial local files`() {
        val destination = directory.resolve("download").toFile().apply { writeText("partial") }
        handler = { it.reply(500) }
        assertThrows(WebDavHttpException::class.java) { client().download("backup.zip", destination) }
        assertFalse(destination.exists())
        handler = {
            it.sendResponseHeaders(200, 256L * 1024 * 1024 + 1)
        }
        assertThrows(IOException::class.java) { client().download("backup.zip", destination) }
        assertFalse(destination.exists())
    }

    @Test
    fun `redirect responses are not followed with credentials`() {
        handler = {
            it.responseHeaders.add("Location", "$base/redirected")
            it.reply(302)
        }
        assertEquals(302, assertThrows(WebDavHttpException::class.java) { client().list() }.statusCode)
        assertEquals(1, requests.size)
    }

    @Test
    fun `production settings require https and do not reveal credentials`() {
        assertTrue(WebDavConfig("https://example.com/dav/", "user", "secret").isValid())
        listOf(base, "ftp://example.com", "https://user:pass@example.com", "https://example.com/?token=secret",
            "https://example.com/#fragment", "https://example.com:99999").forEach {
            assertFalse(WebDavConfig(it).isValid(), it)
        }
        assertFalse(WebDavConfig("https://example.com", "user:name").isValid())
        assertFalse(WebDavConfig("https://example.com", "user", "secret").toString().contains("secret"))
        assertThrows(IllegalArgumentException::class.java) {
            WebDavClient(WebDavConfig(base), OkHttpClient())
        }
    }

    private fun HttpExchange.reply(status: Int, text: String = "") {
        val bytes = text.toByteArray(Charsets.UTF_8)
        sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) responseBody.write(bytes)
    }

    private fun multistatus(entries: String) =
        """<d:multistatus xmlns:d="DAV:">$entries</d:multistatus>"""

    private fun entry(
        href: String,
        collection: Boolean = false,
        modified: String = "Fri, 30 Oct 2026 00:00:00 GMT",
        status: Int = 200,
    ) = """<d:response><d:href>$href</d:href><d:propstat><d:prop>
        <d:resourcetype>${if (collection) "<d:collection/>" else ""}</d:resourcetype>
        <d:getcontentlength>42</d:getcontentlength><d:getlastmodified>$modified</d:getlastmodified>
        </d:prop><d:status>HTTP/1.1 $status Status</d:status></d:propstat></d:response>"""
}

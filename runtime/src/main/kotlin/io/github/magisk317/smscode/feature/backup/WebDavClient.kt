package io.github.magisk317.smscode.feature.backup

import io.github.magisk317.smscode.runtime.WebDavBackupFile
import io.github.magisk317.smscode.runtime.WebDavConfig
import io.github.magisk317.smscode.runtime.WebDavHttpException
import io.github.magisk317.smscode.verification.BoundedInput
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.w3c.dom.Element
import org.xml.sax.SAXException

internal class WebDavClient(
    private val config: WebDavConfig,
    private val client: OkHttpClient = defaultClient,
    allowLocalHttp: Boolean = false,
) {
    private val folder: HttpUrl

    init {
        val base = config.url.trim().toHttpUrl()
        require(config.isValid() || (
            allowLocalHttp && base.scheme == "http" && base.host in setOf("localhost", "127.0.0.1") &&
                base.username.isEmpty() && base.password.isEmpty() && base.query == null && base.fragment == null
            )) { "Invalid WebDAV configuration" }
        folder = base.newBuilder()
            .apply { if (!base.encodedPath.endsWith('/')) addPathSegment("") }
            .addPathSegment(FOLDER_NAME)
            .addPathSegment("")
            .build()
    }

    fun upload(file: File, name: String) {
        val target = fileUrl(name)
        request("MKCOL", folder, EMPTY_BODY).use { response ->
            when (response.code) {
                201 -> Unit
                405 -> {
                    if (readCollection("0").none {
                        it.url.newBuilder().encodedPath(it.url.encodedPath.trimEnd('/') + "/").build() == folder &&
                            it.isCollection
                    }) {
                        throw IOException("WebDAV backup directory is not a collection")
                    }
                }
                else -> throw WebDavHttpException(response.code)
            }
        }
        val mime = if (name.endsWith(".zip", ignoreCase = true)) "application/zip" else "application/json"
        request("PUT", target, file.asRequestBody(mime.toMediaType()), mapOf("If-None-Match" to "*")).use {
            if (it.code !in setOf(200, 201, 204)) throw WebDavHttpException(it.code)
        }
    }

    fun list(): List<WebDavBackupFile> = readCollection("1")
        .filter { !it.isCollection && isDirectChild(it.url) && isBackupName(it.name) }
        .distinctBy { it.name }
        .map { WebDavBackupFile(it.name, it.size, it.modifiedAt) }
        .sortedWith(compareByDescending<WebDavBackupFile> { it.modifiedAt ?: 0L }.thenByDescending { it.name })

    fun download(name: String, destination: File) {
        try {
            request("GET", fileUrl(name)).use { response ->
                if (response.code != 200) throw WebDavHttpException(response.code)
                val body = response.body ?: throw IOException("Empty WebDAV response")
                if (body.contentLength() > BoundedInput.MAX_BACKUP_ARCHIVE_BYTES) {
                    throw IOException("WebDAV backup exceeds size limit")
                }
                body.byteStream().use { input ->
                    destination.outputStream().use { output ->
                        BoundedInput.copyTo(input, output, BoundedInput.MAX_BACKUP_ARCHIVE_BYTES)
                    }
                }
            }
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
    }

    private fun fileUrl(name: String): HttpUrl {
        require(isBackupName(name) && name !in setOf(".", "..") && '/' !in name && '\\' !in name &&
            name.none { it.isISOControl() }) { "Invalid backup filename" }
        return folder.newBuilder().removePathSegment(folder.pathSize - 1).addPathSegment(name).build()
    }

    private fun isDirectChild(url: HttpUrl): Boolean =
        url.scheme == folder.scheme && url.host == folder.host && url.port == folder.port &&
            url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null &&
            url.pathSegments.dropLast(1) == folder.pathSegments.dropLast(1) &&
            url.pathSegments.last().isNotEmpty()

    private data class Entry(
        val url: HttpUrl,
        val name: String,
        val isCollection: Boolean,
        val size: Long?,
        val modifiedAt: Long?,
    )

    private fun readCollection(depth: String): List<Entry> {
        request("PROPFIND", folder, PROPERTIES.toRequestBody(XML), mapOf("Depth" to depth)).use { response ->
            if (response.code == 404 && depth == "1") return emptyList()
            if (response.code != 207) throw WebDavHttpException(response.code)
            val body = response.body ?: throw IOException("Empty WebDAV response")
            val bytes = body.byteStream().use { BoundedInput.readBytes(it, MAX_LIST_BYTES) }
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isExpandEntityReferences = false
                // Android and host JVM parsers expose different optional security features.
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            val builder = factory.newDocumentBuilder().apply {
                setEntityResolver { _, _ -> throw SAXException("External XML entities are forbidden") }
            }
            val root = try {
                builder.parse(bytes.inputStream()).documentElement
            } catch (error: Exception) {
                throw IOException("Invalid WebDAV file list", error)
            }
            if (root.namespaceURI != DAV || root.localName != "multistatus") {
                throw IOException("Invalid WebDAV multistatus")
            }
            return root.children("response").mapNotNull { item ->
                val href = item.children("href").firstOrNull()?.textContent?.trim() ?: return@mapNotNull null
                val url = folder.resolve(href) ?: return@mapNotNull null
                val props = item.children("propstat").filter {
                    it.children("status").firstOrNull()?.textContent?.trim()
                        ?.split(Regex("\\s+"))?.getOrNull(1) == "200"
                }.flatMap { it.children("prop") }
                if (props.isEmpty()) return@mapNotNull null
                val collection = props.any { prop ->
                    prop.children("resourcetype").any { it.children("collection").isNotEmpty() }
                }
                val size = props.flatMap { it.children("getcontentlength") }.firstOrNull()
                    ?.textContent?.trim()?.toLongOrNull()?.takeIf { it >= 0L }
                val modified = props.flatMap { it.children("getlastmodified") }.firstOrNull()?.textContent
                    ?.let { raw ->
                        runCatching {
                            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).parse(raw.trim())?.time
                        }.getOrNull()
                    }
                Entry(url, url.pathSegments.last(), collection, size, modified)
            }
        }
    }

    private fun request(
        method: String,
        url: HttpUrl,
        body: RequestBody? = null,
        headers: Map<String, String> = emptyMap(),
    ): Response {
        val request = Request.Builder().url(url).method(method, body).apply {
            if (config.username.isNotEmpty()) {
                header("Authorization", Credentials.basic(config.username, config.password, Charsets.UTF_8))
            }
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        return client.newCall(request).execute()
    }

    private fun Element.children(name: String): List<Element> = buildList {
        val nodes = childNodes
        for (index in 0 until nodes.length) {
            val child = nodes.item(index)
            if (child is Element && child.namespaceURI == DAV && child.localName == name) add(child)
        }
    }

    companion object {
        const val FOLDER_NAME = "XposedSmsCode"
        private const val DAV = "DAV:"
        private const val MAX_LIST_BYTES = 4 * 1024 * 1024
        private val XML = "application/xml; charset=utf-8".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
        private const val PROPERTIES = """<?xml version="1.0" encoding="utf-8"?>
            <d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/><d:getcontentlength/>
            <d:getlastmodified/></d:prop></d:propfind>"""
        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .build()

        private fun isBackupName(name: String): Boolean =
            (name.endsWith(".scebak", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)) &&
                '/' !in name && '\\' !in name && name.none { it.isISOControl() }
    }
}

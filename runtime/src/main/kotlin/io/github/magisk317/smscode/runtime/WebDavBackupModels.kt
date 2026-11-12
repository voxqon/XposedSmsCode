package io.github.magisk317.smscode.runtime

import java.net.URI
import kotlinx.serialization.Serializable

@Serializable
data class WebDavConfig(
    val url: String = "",
    val username: String = "",
    val password: String = "",
) {
    fun isValid(): Boolean = runCatching {
        val uri = URI(url.trim())
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null &&
            (uri.port == -1 || uri.port in 1..65535) &&
            ':' !in username && (username.isNotEmpty() || password.isEmpty())
    }.getOrDefault(false)

    override fun toString(): String = "WebDavConfig(configured=${url.isNotBlank()})"
}

data class WebDavBackupFile(
    val name: String,
    val size: Long? = null,
    val modifiedAt: Long? = null,
)

class WebDavHttpException(val statusCode: Int) : java.io.IOException("WebDAV HTTP $statusCode")

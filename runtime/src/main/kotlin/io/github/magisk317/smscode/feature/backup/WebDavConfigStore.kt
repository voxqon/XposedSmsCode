package io.github.magisk317.smscode.feature.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import io.github.magisk317.smscode.runtime.WebDavConfig
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.json.Json

internal object WebDavConfigStore {
    private const val KEY_ALIAS = "xposedsmscode.webdav"
    private const val CIPHER = "AES/GCM/NoPadding"

    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, "webdav-settings"))

    @Synchronized
    fun load(context: Context): WebDavConfig {
        val file = file(context)
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return WebDavConfig()
        val encrypted = file.readFully()
        if (encrypted.size < 29 || encrypted[0].toInt() != 12) throw IOException("Invalid WebDAV settings")
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, encrypted.copyOfRange(1, 13)))
        return Json.decodeFromString<WebDavConfig>(
            cipher.doFinal(encrypted.copyOfRange(13, encrypted.size)).toString(Charsets.UTF_8),
        )
    }

    @Synchronized
    fun save(context: Context, config: WebDavConfig) {
        require(config.isValid())
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(Json.encodeToString(WebDavConfig.serializer(), config).toByteArray(Charsets.UTF_8))
        val file = file(context)
        val output = file.startWrite()
        try {
            output.write(cipher.iv.size)
            output.write(cipher.iv)
            output.write(encrypted)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }
}

package io.github.magisk317.smscode.runtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.magisk317.smscode.feature.backup.BackupManager
import io.github.magisk317.smscode.feature.backup.WebDavClient
import io.github.magisk317.smscode.feature.backup.WebDavConfigStore
import io.github.magisk317.smscode.runtime.bridge.UiBackupAccess
import java.io.File

object RuntimeBackupFacade : UiBackupAccess {
    override fun loadWebDavConfig(context: Context): WebDavConfig = WebDavConfigStore.load(context)

    override fun saveWebDavConfig(context: Context, config: WebDavConfig) = WebDavConfigStore.save(context, config)

    override fun listWebDavBackups(config: WebDavConfig): List<WebDavBackupFile> = WebDavClient(config).list()

    override fun uploadWebDavBackup(config: WebDavConfig, file: File, name: String) =
        WebDavClient(config).upload(file, name)

    override fun createTemporaryBackup(context: Context, includeDatabase: Boolean): File =
        File.createTempFile("webdav-", if (includeDatabase) ".zip" else ".scebak", context.cacheDir)

    override fun downloadWebDavBackup(context: Context, config: WebDavConfig, name: String): File {
        val file = createTemporaryBackup(context, name.endsWith(".zip", ignoreCase = true))
        try {
            WebDavClient(config).download(name, file)
            return file
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    override fun getExportRuleListSAFIntent(
        context: Context,
        includeDatabase: Boolean,
    ): Intent {
        return BackupManager.getExportRuleListSAFIntent(context, includeDatabase)
    }

    override fun getImportRuleListSAFIntent(context: Context): Intent {
        return BackupManager.getImportRuleListSAFIntent(context)
    }

    override fun exportBackup(
        context: Context,
        uri: Uri,
        ruleList: List<RuntimeBackupRule>,
        preferences: Map<String, String?>?,
        records: List<RuntimeBackupSmsRecord>?,
        appVersion: String,
        includeDatabase: Boolean,
    ): RuntimeBackupExportResult {
        return BackupManager.exportBackup(
            context = context,
            uri = uri,
            ruleList = ruleList.map(RuntimeBackupRule::toInternal),
            preferences = preferences,
            records = records?.map(RuntimeBackupSmsRecord::toInternal),
            appVersion = appVersion,
            includeDatabase = includeDatabase,
        ).toRuntime()
    }

    override fun importRuleList(
        context: Context,
        uri: Uri,
        currentAppVersion: String,
    ): RuntimeBackupImportResult {
        return BackupManager.importRuleList(context, uri, currentAppVersion).toRuntime()
    }

    override fun restoreDatabaseFromBackup(context: Context, uri: Uri): Boolean {
        return BackupManager.restoreDatabaseFromBackup(context, uri)
    }
}

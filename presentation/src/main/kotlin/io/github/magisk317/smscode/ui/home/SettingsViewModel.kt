package io.github.magisk317.smscode.ui.home

import io.github.magisk317.smscode.runtime.common.prefs.AppearancePreferences
import io.github.magisk317.smscode.runtime.common.prefs.AppPreferencesDataStore
import io.github.magisk317.smscode.runtime.common.prefs.SharedPreferenceKeys
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.magisk317.smscode.presentation.BuildConfig
import io.github.magisk317.smscode.common.constant.Const
import io.github.magisk317.smscode.common.constant.PrefConst
import io.github.magisk317.smscode.common.constant.PrefRestoreTypeRegistry
import io.github.magisk317.smscode.common.constant.PrefValueType
import io.github.magisk317.smscode.common.constant.PreferenceBackupPolicy
import io.github.magisk317.smscode.common.utils.*
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import android.content.Intent
import io.github.magisk317.smscode.presentation.R
import io.github.magisk317.smscode.runtime.RuntimeBackupExportResult
import io.github.magisk317.smscode.runtime.RuntimeBackupImportResult
import io.github.magisk317.smscode.runtime.RuntimeBackupImportStatus
import io.github.magisk317.smscode.runtime.RuntimeBackupRule
import io.github.magisk317.smscode.runtime.RuntimeBackupSmsRecord
import io.github.magisk317.smscode.runtime.WebDavBackupFile
import io.github.magisk317.smscode.runtime.WebDavConfig
import io.github.magisk317.smscode.runtime.WebDavHttpException
import io.github.magisk317.smscode.runtime.bridge.UiBackupAccess
import io.github.magisk317.smscode.runtime.bridge.UiStorageAccess
import io.github.magisk317.xposed.logging.XLog
import io.github.magisk317.smscode.runtime.common.utils.StorageUtils
import io.github.magisk317.smscode.runtime.common.utils.StringUtils
import io.github.magisk317.smscode.rule.model.SmsCodeMatchedRule
import io.github.magisk317.smscode.rule.model.SmsCodeMatchedRuleSource
import io.github.magisk317.smscode.runtime.contract.prefs.PreferenceCommitResult
import io.github.magisk317.smscode.runtime.contract.prefs.PreferenceSpec
import io.github.magisk317.uikit.theme.UiKitStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class SettingsEvent {
    data object ShowPrivacyPolicy : SettingsEvent()
    data class SmsCodeTestResult(
        val code: String,
        val matchedRuleLabel: String? = null,
    ) : SettingsEvent()
    data object NavigateToRules : SettingsEvent()
    data object NavigateToRecords : SettingsEvent()
    data object NavigateToSettings : SettingsEvent()
    data class ShowSnackbar(val message: String) : SettingsEvent()
    data class BackupResultEvent(val success: Boolean) : SettingsEvent()
    data class RestoreResultEvent(val result: RuntimeBackupImportResult) : SettingsEvent()
    data class ImportDialogConfirm(val uri: android.net.Uri) : SettingsEvent()
}

class SettingsViewModel(
    application: Application,
    private val storage: UiStorageAccess,
    private val backup: UiBackupAccess,
) : AndroidViewModel(application) {
    data class CoercedRestoreValue(
        val type: PrefValueType,
        val booleanValue: Boolean? = null,
        val intValue: Int? = null,
        val floatValue: Float? = null,
        val stringValue: String? = null,
    ) {
        val shouldWrite: Boolean
            get() = when (type) {
                PrefValueType.BOOLEAN -> booleanValue != null
                PrefValueType.INT -> intValue != null
                PrefValueType.FLOAT -> floatValue != null
                PrefValueType.STRING -> true
            }
    }

    private val _eventsFlow = MutableSharedFlow<SettingsEvent>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventsFlow = _eventsFlow.asSharedFlow()

    data class WebDavState(
        val config: WebDavConfig = WebDavConfig(),
        val loaded: Boolean = false,
        val busy: Boolean = false,
        val listed: Boolean = false,
        val files: List<WebDavBackupFile> = emptyList(),
        val error: String? = null,
        val restoreUri: android.net.Uri? = null,
    )

    private val _webDavState = MutableStateFlow(WebDavState())
    val webDavState = _webDavState.asStateFlow()
    private var webDavRestoreFile: File? = null

    fun loadWebDavConfig() {
        if (_webDavState.value.loaded) return
        runWebDavOperation {
            val config = withContext(Dispatchers.IO) { backup.loadWebDavConfig(getApplication()) }
            _webDavState.value = _webDavState.value.copy(config = config, loaded = true)
            if (config.isValid()) {
                val files = withContext(Dispatchers.IO) { backup.listWebDavBackups(config) }
                _webDavState.value = _webDavState.value.copy(files = files, listed = true)
            }
        }
    }

    fun saveWebDavConfig(config: WebDavConfig) {
        if (!config.isValid()) return
        runWebDavOperation {
            withContext(Dispatchers.IO) { backup.saveWebDavConfig(getApplication(), config) }
            _webDavState.value = _webDavState.value.copy(
                config = config, loaded = true, files = emptyList(), listed = false,
            )
            _eventsFlow.tryEmit(SettingsEvent.ShowSnackbar(getApplication<Application>().getString(R.string.webdav_saved)))
            val files = withContext(Dispatchers.IO) { backup.listWebDavBackups(config) }
            _webDavState.value = _webDavState.value.copy(files = files, listed = true)
        }
    }

    fun refreshWebDavBackups() {
        val config = _webDavState.value.config
        if (!config.isValid()) return
        runWebDavOperation {
            val files = withContext(Dispatchers.IO) { backup.listWebDavBackups(config) }
            _webDavState.value = _webDavState.value.copy(files = files, listed = true)
        }
    }

    fun performWebDavBackup(
        includeConfig: Boolean,
        includeRules: Boolean,
        includeRecords: Boolean,
        includeDatabase: Boolean,
    ) {
        val config = _webDavState.value.config
        if (!config.isValid()) return
        runWebDavOperation {
            val context = getApplication<Application>()
            val file = withContext(Dispatchers.IO) { backup.createTemporaryBackup(context, includeDatabase) }
            try {
                val result = writeBackup(
                    android.net.Uri.fromFile(file), includeConfig, includeRules, includeRecords, includeDatabase,
                )
                if (result != RuntimeBackupExportResult.SUCCESS) throw IOException("Backup export failed")
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
                val extension = if (includeDatabase) "-db.zip" else ".scebak"
                val name = "XposedSmsCode-$timestamp-${UUID.randomUUID()}$extension"
                withContext(Dispatchers.IO) { backup.uploadWebDavBackup(config, file, name) }
                _eventsFlow.tryEmit(SettingsEvent.BackupResultEvent(true))
                val files = withContext(Dispatchers.IO) { backup.listWebDavBackups(config) }
                _webDavState.value = _webDavState.value.copy(files = files, listed = true)
            } finally {
                file.delete()
            }
        }
    }

    fun prepareWebDavRestore(file: WebDavBackupFile) {
        val config = _webDavState.value.config
        if (!config.isValid()) return
        runWebDavOperation {
            val downloaded = withContext(Dispatchers.IO) {
                backup.downloadWebDavBackup(getApplication(), config, file.name)
            }
            webDavRestoreFile?.delete()
            webDavRestoreFile = downloaded
            _webDavState.value = _webDavState.value.copy(restoreUri = android.net.Uri.fromFile(downloaded))
        }
    }

    fun discardWebDavRestore(uri: android.net.Uri?) {
        val file = webDavRestoreFile ?: return
        if (uri == android.net.Uri.fromFile(file)) {
            file.delete()
            webDavRestoreFile = null
            _webDavState.value = _webDavState.value.copy(restoreUri = null)
        }
    }

    private fun runWebDavOperation(block: suspend () -> Unit) {
        if (_webDavState.value.busy) return
        _webDavState.value = _webDavState.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val context = getApplication<Application>()
                val message = when {
                    error is WebDavHttpException && error.statusCode in setOf(401, 403) ->
                        context.getString(R.string.webdav_auth_failed)
                    error is WebDavHttpException -> context.getString(R.string.webdav_http_error, error.statusCode)
                    else -> context.getString(R.string.webdav_operation_failed)
                }
                _webDavState.value = _webDavState.value.copy(error = message)
            } finally {
                _webDavState.value = _webDavState.value.copy(busy = false)
            }
        }
    }

    override fun onCleared() {
        webDavRestoreFile?.delete()
        super.onCleared()
    }

    data class ThemeState(
        val mode: Int,
        val uiKitStyle: Int = UiKitStyle.Expressive.value,
        val layoutScale: Int = SharedPreferenceKeys.Appearance.DEFAULT_LAYOUT_SCALE,
        val paletteStyle: Int = SharedPreferenceKeys.Appearance.DEFAULT_PALETTE_STYLE,
        val colorSpec: Int = SharedPreferenceKeys.Appearance.DEFAULT_COLOR_SPEC,
        val accentColor: Int = SharedPreferenceKeys.Appearance.DEFAULT_ACCENT_COLOR,
        val monetEnabled: Boolean = SharedPreferenceKeys.Appearance.DEFAULT_MONET_ENABLED,
        val surfaceBlur: Boolean = SharedPreferenceKeys.Appearance.DEFAULT_SURFACE_BLUR,
        val dynamicColor: Boolean = SharedPreferenceKeys.Appearance.DEFAULT_DYNAMIC_COLOR,
        val floatingBottomBar: Boolean = SharedPreferenceKeys.Appearance.DEFAULT_FLOATING_BOTTOM_BAR,
        val bottomBarBlur: Boolean = SharedPreferenceKeys.Appearance.DEFAULT_BOTTOM_BAR_BLUR,
        val bottomBarBackdrop: Boolean = SharedPreferenceKeys.Appearance.DEFAULT_BOTTOM_BAR_BACKDROP,
        val centerX: Float = -1f,
        val centerY: Float = -1f,
    )

    private val _themeState = MutableStateFlow(ThemeState(0, UiKitStyle.Expressive.value))
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    val smsRecordCount: StateFlow<Long> = storage.dbManager(application)
        .queryAllSmsMsgCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Const.FLOW_STOP_TIMEOUT_MS),
            initialValue = 0L,
        )

    init {
        viewModelScope.launch {
            val mode = AppPreferences.getThemeMode(getApplication())
            val uiKitStyle = AppPreferences.getUiKitStyle(getApplication())
            _themeState.value = ThemeState(
                mode = mode,
                uiKitStyle = uiKitStyle,
                layoutScale = AppearancePreferences.layoutScale(getApplication()),
                paletteStyle = AppearancePreferences.paletteStyle(getApplication()),
                colorSpec = AppearancePreferences.colorSpec(getApplication()),
                accentColor = AppearancePreferences.accentColor(getApplication()),
                monetEnabled = AppearancePreferences.monetEnabled(getApplication()),
                surfaceBlur = AppearancePreferences.surfaceBlurEnabled(getApplication()),
                dynamicColor = AppearancePreferences.dynamicColorEnabled(getApplication()),
                floatingBottomBar = AppearancePreferences.floatingBottomBarEnabled(getApplication()),
                bottomBarBlur = AppearancePreferences.bottomBarBlurEnabled(getApplication()),
                bottomBarBackdrop = AppearancePreferences.bottomBarBackdropEnabled(getApplication()),
            )
        }
        viewModelScope.launch {
            HookPreferenceMirror.publish(getApplication())
        }
    }

    fun setThemeMode(mode: Int, x: Float = -1f, y: Float = -1f) {
        viewModelScope.launch {
            AppPreferences.setThemeMode(getApplication(), mode)
            _themeState.value = _themeState.value.copy(mode = mode, centerX = x, centerY = y)
        }
    }

    fun setUiKitStyle(style: Int) {
        viewModelScope.launch {
            AppPreferences.setUiKitStyle(getApplication(), style)
            _themeState.value = _themeState.value.copy(uiKitStyle = style)
        }
    }

    fun setLayoutScale(value: Int) {
        viewModelScope.launch {
            AppearancePreferences.setLayoutScale(getApplication(), value)
            _themeState.value = _themeState.value.copy(layoutScale = value)
        }
    }

    fun setPaletteStyle(value: Int) {
        viewModelScope.launch {
            AppearancePreferences.setPaletteStyle(getApplication(), value)
            _themeState.value = _themeState.value.copy(paletteStyle = value)
        }
    }

    fun setColorSpec(value: Int) {
        viewModelScope.launch {
            AppearancePreferences.setColorSpec(getApplication(), value)
            _themeState.value = _themeState.value.copy(colorSpec = value)
        }
    }

    fun setAccentColor(argb: Int) {
        viewModelScope.launch {
            AppearancePreferences.setAccentColor(getApplication(), argb)
            _themeState.value = _themeState.value.copy(accentColor = argb)
        }
    }

    fun setMonetEnabled(enabled: Boolean) {
        viewModelScope.launch {
            AppearancePreferences.setMonetEnabled(getApplication(), enabled)
            _themeState.value = _themeState.value.copy(monetEnabled = enabled)
        }
    }

    fun setSurfaceBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            AppearancePreferences.setSurfaceBlurEnabled(getApplication(), enabled)
            _themeState.value = _themeState.value.copy(surfaceBlur = enabled)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            AppearancePreferences.setDynamicColorEnabled(getApplication(), enabled)
            _themeState.value = _themeState.value.copy(dynamicColor = enabled)
        }
    }

    fun setFloatingBottomBarEnabled(enabled: Boolean) {
        viewModelScope.launch {
            AppearancePreferences.setFloatingBottomBarEnabled(getApplication(), enabled)
            _themeState.value = _themeState.value.copy(floatingBottomBar = enabled)
        }
    }

    fun setBottomBarBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            AppearancePreferences.setBottomBarBlurEnabled(getApplication(), enabled)
            _themeState.value = _themeState.value.copy(bottomBarBlur = enabled)
        }
    }

    fun setBottomBarBackdropEnabled(enabled: Boolean) {
        viewModelScope.launch {
            AppearancePreferences.setBottomBarBackdropEnabled(getApplication(), enabled)
            _themeState.value = _themeState.value.copy(bottomBarBackdrop = enabled)
        }
    }

    fun handleArguments(args: Bundle?) {
        if (args == null) return

        viewModelScope.launch {
            if (!AppPreferences.isPrivacyPolicyAccepted(getApplication())) {
                _eventsFlow.tryEmit(SettingsEvent.ShowPrivacyPolicy)
            } else {
                val extraAction = args.getString(Const.EXTRA_ACTION)
                if (Const.ACTION_OPEN_RECORDS == extraAction) {
                    args.remove(Const.EXTRA_ACTION)
                    _eventsFlow.tryEmit(SettingsEvent.NavigateToRecords)
                } else if (Const.ACTION_OPEN_SETTINGS == extraAction) {
                    args.remove(Const.EXTRA_ACTION)
                    _eventsFlow.tryEmit(SettingsEvent.NavigateToSettings)
                } else if (Const.ACTION_OPEN_RULES == extraAction) {
                    args.remove(Const.EXTRA_ACTION)
                    _eventsFlow.tryEmit(SettingsEvent.NavigateToRules)
                }
            }
        }
    }

    fun pinShortcutToDesktop() {
        val context = getApplication<Application>()
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
            }
            // 使用挂载了 CATEGORY_INFO 的主入口强行注册
            val mainActivity = android.content.ComponentName(context, MainActivity::class.java)
            val shortcut = ShortcutInfoCompat.Builder(context, "shortcut_main")
                .setShortLabel(context.getString(R.string.app_name))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .setActivity(mainActivity)
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        } else {
            _eventsFlow.tryEmit(SettingsEvent.ShowSnackbar("当前系统不支持创建快捷方式"))
        }
    }

    fun openSmsCodeRules() {
        viewModelScope.launch {
            _eventsFlow.tryEmit(SettingsEvent.NavigateToRules)
        }
    }

    fun isLauncherIconVisible(): Boolean {
        val context = getApplication<Application>()
        val component = ComponentName(context, LauncherActivity::class.java)
        val pm = context.packageManager
        return when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
            -> false

            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> runCatching {
                pm.getActivityInfo(component, 0).enabled
            }.getOrDefault(false)

            else -> false
        }
    }

    fun setLauncherIconVisible(visible: Boolean): Boolean {
        val context = getApplication<Application>()
        val component = ComponentName(context, LauncherActivity::class.java)
        val pm = context.packageManager
        val newState = if (visible) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        return runCatching {
            pm.setComponentEnabledSetting(
                component,
                newState,
                PackageManager.DONT_KILL_APP,
            )
            true
        }.onFailure {
            XLog.e(message = "Failed to set launcher icon visible=$visible", it)
        }.getOrElse { false }
    }

    fun performSmsCodeTest(msgBody: String) {
        viewModelScope.launch {
            val result = try {
                XLog.i(message = "Sms code test start: length=%d", msgBody.length)
                withContext(Dispatchers.IO) {
                    if (TextUtils.isEmpty(msgBody)) {
                        null
                    } else {
                        val keywords = AppPreferencesDataStore.getString(
                            getApplication(),
                            PrefConst.KEY_SMSCODE_KEYWORDS,
                            PrefConst.SMSCODE_KEYWORDS_DEFAULT,
                        )
                        SmsCodeUtils.parseSmsCodeResultIfExists(
                            context = getApplication(),
                            content = msgBody,
                            keywordsRegex = keywords,
                        )
                    }
                }
            } catch (e: Exception) {
                XLog.e(message = "Sms code test failed", e)
                e.printStackTrace()
                null
            }
            val code = result?.code.orEmpty()
            val matchedRuleLabel = result?.matchedRule?.let(::formatMatchedRuleLabel)
            val safeCode = if (BuildConfig.DEBUG || BuildConfig.IS_CI_BUILD) {
                StringUtils.escape(code)
            } else {
                StringUtils.summarizeCode(code)
            }
            XLog.i(message = "Sms code test finished: code=%s", safeCode)
            _eventsFlow.tryEmit(SettingsEvent.SmsCodeTestResult(code, matchedRuleLabel))
        }
    }

    private fun formatMatchedRuleLabel(matchedRule: SmsCodeMatchedRule): String {
        val app = getApplication<Application>()
        return when (matchedRule.source) {
            SmsCodeMatchedRuleSource.BUILTIN ->
                app.getString(R.string.builtin_rule_badge_format, matchedRule.ordinal)

            SmsCodeMatchedRuleSource.OFFICIAL ->
                app.getString(R.string.official_rule_badge_format, matchedRule.ordinal)

            SmsCodeMatchedRuleSource.CUSTOM ->
                app.getString(R.string.user_rule_badge_format, matchedRule.ordinal)
        }
    }

    fun setInternalFilesWritable() {
        // Repair releases that widened Android/data/<package> and files/ to
        // 0777. Hook IPC now goes through the app-owned provider.
        StorageUtils.repairExternalAppDataPermissions(getApplication())
        viewModelScope.launch {
            HookPreferenceMirror.publish(getApplication())
        }
    }

    fun handleBackupArguments(uri: android.net.Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _eventsFlow.tryEmit(SettingsEvent.ImportDialogConfirm(uri))
        }
    }

    fun performBackup(
        uri: android.net.Uri,
        includeConfig: Boolean,
        includeRules: Boolean,
        includeRecords: Boolean,
        includeDatabase: Boolean,
    ) {
        viewModelScope.launch {
            try {
                val result = writeBackup(uri, includeConfig, includeRules, includeRecords, includeDatabase)
                _eventsFlow.tryEmit(SettingsEvent.BackupResultEvent(result == RuntimeBackupExportResult.SUCCESS))
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                XLog.e(message = "Backup failed", e)
                _eventsFlow.tryEmit(SettingsEvent.BackupResultEvent(false))
            }
        }
    }

    private suspend fun writeBackup(
        uri: android.net.Uri,
        includeConfig: Boolean,
        includeRules: Boolean,
        includeRecords: Boolean,
        includeDatabase: Boolean,
    ): RuntimeBackupExportResult {
        val context = getApplication<Application>()
        val rules = if (includeRules) {
            withContext(Dispatchers.IO) {
                storage.dbManager(context).queryAllSmsCodeRules()
                    .map { RuntimeBackupRule(it.company, it.codeKeyword, it.codeRegex) }
            }
        } else {
            emptyList()
        }
        val records = if (includeRecords) {
            withContext(Dispatchers.IO) {
                storage.dbManager(context).queryAllSmsMsg().map {
                    RuntimeBackupSmsRecord(
                        sender = it.sender,
                        body = it.body,
                        date = it.date,
                        processedTime = it.processedTime,
                        company = it.company,
                        smsCode = it.smsCode,
                        packageName = it.packageName,
                        simSlot = it.simSlot,
                        subId = it.subId,
                        msgType = it.msgType,
                        callType = it.callType,
                        forwardStatus = it.forwardStatus,
                        forwardTarget = it.forwardTarget,
                        forwardMessage = it.forwardMessage,
                        forwardTime = it.forwardTime,
                    )
                }
            }
        } else {
            null
        }
        val prefs = if (includeConfig) {
            withContext(Dispatchers.IO) { AppPreferencesDataStore.snapshotForBackup(context) }
        } else {
            null
        }
        return withContext(Dispatchers.IO) {
            backup.exportBackup(
                context = context,
                uri = uri,
                ruleList = rules,
                preferences = prefs,
                records = records,
                appVersion = BuildConfig.VERSION_NAME,
                includeDatabase = includeDatabase,
            )
        }
    }

    fun performRestore(
        uri: android.net.Uri,
        restoreConfig: Boolean,
        restoreRules: Boolean,
        restoreRecords: Boolean,
        restoreDatabase: Boolean,
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                XLog.i(
                    message = "Restore start: uri=%s restoreConfig=%s restoreRules=%s restoreRecords=%s restoreDatabase=%s",
                    uri.toString(),
                    restoreConfig,
                    restoreRules,
                    restoreRecords,
                    restoreDatabase,
                )
                val importResult = withContext(Dispatchers.IO) {
                    backup.importRuleList(context, uri, BuildConfig.VERSION_NAME)
                }
                XLog.i(
                    message = "Restore import result=%s rules=%d records=%d prefs=%d warning=%s",
                    importResult.result.name,
                    importResult.rules.size,
                    importResult.records?.size ?: 0,
                    importResult.preferences?.size ?: 0,
                    importResult.warning?.name ?: "none",
                )

                if (importResult.result == RuntimeBackupImportStatus.SUCCESS) {
                    withContext(Dispatchers.IO) {
                        if (restoreDatabase) {
                            val restored = backup.restoreDatabaseFromBackup(context, uri)
                            if (!restored) {
                                throw IllegalStateException("Restore database failed: backup zip has no database files")
                            }
                            if (restoreRules || restoreRecords) {
                                XLog.i(
                                    message = "Restore database enabled: skip logical restore rules=%s records=%s",
                                    restoreRules,
                                    restoreRecords,
                                )
                            }
                        } else {
                            if (restoreRules) restoreRules(context, importResult.rules)
                            if (restoreRecords) restoreRecords(context, importResult.records.orEmpty())
                        }
                        if (restoreConfig) restorePreferences(context, importResult.preferences.orEmpty())
                    }
                    XLog.i(message = "Restore apply finished")
                }
                _eventsFlow.tryEmit(SettingsEvent.RestoreResultEvent(importResult))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                    XLog.e(message = "Restore failed", e)
                    // Return failed event
                    _eventsFlow.tryEmit(
                        SettingsEvent.RestoreResultEvent(
                            RuntimeBackupImportResult(RuntimeBackupImportStatus.READ_FAILED),
                        ),
                    )
            } finally {
                discardWebDavRestore(uri)
            }
        }
    }

    private suspend fun restoreRules(context: Context, rules: List<RuntimeBackupRule>) {
        if (rules.isEmpty()) return
        val dbManager = storage.dbManager(context)
        val entities = rules.map {
            io.github.magisk317.smscode.db.entity.SmsCodeRule(it.company, it.codeKeyword, it.codeRegex)
        }
        dbManager.addSmsCodeRules(entities)
    }

    private suspend fun restoreRecords(context: Context, records: List<RuntimeBackupSmsRecord>) {
        val dbManager = storage.dbManager(context)
        if (records.isEmpty()) {
            XLog.w(message = "Restore records skipped: empty list")
            return
        }
        val beforeCount = dbManager.queryAllSmsMsg().size
        val entities = records.map {
            io.github.magisk317.smscode.db.entity.SmsMsg(
                sender = it.sender,
                body = it.body,
                date = it.date,
                processedTime = it.processedTime,
                company = it.company,
                smsCode = it.smsCode,
                packageName = it.packageName,
                simSlot = it.simSlot,
                subId = it.subId,
                msgType = it.msgType,
                callType = it.callType,
                forwardStatus = it.forwardStatus,
                forwardTarget = it.forwardTarget,
                forwardMessage = it.forwardMessage,
                forwardTime = it.forwardTime,
            )
        }
        dbManager.addSmsMsgList(entities)
        val afterCount = dbManager.queryAllSmsMsg().size
        XLog.i(
            message = "Restore records finished: requested=%d before=%d after=%d delta=%d",
            records.size,
            beforeCount,
            afterCount,
            afterCount - beforeCount,
        )
    }

    private suspend fun restorePreferences(context: Context, prefsMap: Map<String, String?>) {
        if (prefsMap.isEmpty()) return
        var launcherVisible: Boolean? = null
        val result = AppPreferenceTransactions.commit(context) {
            for ((key, rawValue) in prefsMap) {
                if (!PreferenceBackupPolicy.isPortable(key)) {
                    XLog.w(message = "Restore preference skipped: protected runtime key")
                    continue
                }
                if (rawValue == null) continue
                val coerced = coerceRestoreValue(key, rawValue)
                if (!coerced.shouldWrite) {
                    XLog.w(
                        message = "Restore preference skipped: key=%s raw=%s expectedType=%s",
                        key,
                        rawValue,
                        coerced.type.name,
                    )
                    continue
                }
                when (coerced.type) {
                    PrefValueType.BOOLEAN -> {
                        val value = coerced.booleanValue ?: continue
                        set(resolveBooleanRestoreSpec(key, value), value)
                        if (key == PrefConst.KEY_SHOW_LAUNCHER_ICON) launcherVisible = value
                    }

                    PrefValueType.INT -> {
                        val value = coerced.intValue ?: continue
                        set(PreferenceSpec.int(key, value), value)
                    }

                    PrefValueType.FLOAT -> {
                        val value = coerced.floatValue ?: continue
                        set(PreferenceSpec.float(key, value), value)
                    }

                    PrefValueType.STRING -> {
                        val value = coerced.stringValue ?: rawValue
                        set(resolveStringRestoreSpec(key, value), value)
                    }
                }
            }
        }
        when (result) {
            is PreferenceCommitResult.Persisted -> {
                result.postCommitFailures.forEach { failure ->
                    XLog.e(
                        message = "Restore preference post-commit hook failed: hook=%s error=%s",
                        failure.hookName,
                        failure.error.message ?: failure.error.javaClass.simpleName,
                    )
                }
                launcherVisible?.let(::setLauncherIconVisible)
            }
            is PreferenceCommitResult.NoChanges -> Unit
            is PreferenceCommitResult.NotPersisted -> {
                throw IllegalStateException("Restored preferences were not persisted", result.error)
            }
        }
    }

    private fun resolveBooleanRestoreSpec(key: String, restoredValue: Boolean): PreferenceSpec<Boolean> =
        when (key) {
            PrefConst.KEY_ENABLE_AUTO_INPUT_CODE -> HookPreferenceSpecs.autoInputEnabled
            PrefConst.KEY_ENABLE_AUTO_ENTER_CODE -> HookPreferenceSpecs.autoEnterEnabled
            else -> PreferenceSpec.boolean(key, restoredValue)
        }

    private fun resolveStringRestoreSpec(key: String, restoredValue: String): PreferenceSpec<String> =
        when (key) {
            PrefConst.KEY_AUTO_INPUT_CODE_DELAY -> HookPreferenceSpecs.autoInputDelay
            PrefConst.KEY_AUTO_INPUT_CODE_INTERVAL -> HookPreferenceSpecs.autoInputInterval
            else -> PreferenceSpec.string(key, restoredValue)
        }

    companion object {
        @JvmStatic
        fun coerceRestoreValue(key: String, rawValue: String): CoercedRestoreValue {
            return when (PrefRestoreTypeRegistry.typeOf(key)) {
                PrefValueType.BOOLEAN -> CoercedRestoreValue(
                    type = PrefValueType.BOOLEAN,
                    booleanValue = parseBooleanValue(rawValue),
                )

                PrefValueType.INT -> CoercedRestoreValue(
                    type = PrefValueType.INT,
                    intValue = rawValue.trim().toIntOrNull(),
                )

                PrefValueType.FLOAT -> CoercedRestoreValue(
                    type = PrefValueType.FLOAT,
                    floatValue = rawValue.trim().toFloatOrNull(),
                )

                PrefValueType.STRING -> CoercedRestoreValue(
                    type = PrefValueType.STRING,
                    stringValue = rawValue,
                )
            }
        }

        @JvmStatic
        fun parseBooleanValue(rawValue: String): Boolean? {
            return when (rawValue.trim().lowercase(Locale.ROOT)) {
                "1", "true", "yes", "y", "on" -> true
                "0", "false", "no", "n", "off" -> false
                else -> null
            }
        }
    }
}

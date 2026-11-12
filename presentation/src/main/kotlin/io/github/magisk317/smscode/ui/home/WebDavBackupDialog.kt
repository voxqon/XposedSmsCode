package io.github.magisk317.smscode.ui.home

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.magisk317.smscode.presentation.R
import io.github.magisk317.smscode.runtime.WebDavConfig
import io.github.magisk317.uikit.surface.AppAlertDialog
import io.github.magisk317.uikit.surface.AppIconButton
import io.github.magisk317.uikit.surface.AppPrimaryButton
import io.github.magisk317.uikit.text.AppText
import java.text.DateFormat
import java.util.Date

@Composable
internal fun WebDavBackupDialog(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val state by viewModel.webDavState.collectAsState()
    var url by remember(state.config) { mutableStateOf(state.config.url) }
    var username by remember(state.config) { mutableStateOf(state.config.username) }
    var password by remember(state.config) { mutableStateOf(state.config.password) }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectingBackup by remember { mutableStateOf(false) }
    val config = WebDavConfig(url.trim(), username, password)
    val saved = state.loaded && config == state.config && config.isValid()
    val enabled = !state.busy
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadWebDavConfig() }

    if (selectingBackup) {
        BackupDialog(
            onDismiss = { selectingBackup = false },
            onConfirm = { flags ->
                selectingBackup = false
                viewModel.performWebDavBackup(
                    flags.includeConfig, flags.includeRules, flags.includeRecords, flags.includeDatabase,
                )
            },
        )
        return
    }

    AppAlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { AppText(stringResource(R.string.webdav_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { AppText(stringResource(R.string.webdav_url)) },
                    placeholder = { AppText("https://dav.example.com/") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    enabled = enabled,
                    isError = url.isNotBlank() && !config.isValid(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { AppText(stringResource(R.string.webdav_username)) },
                    singleLine = true,
                    enabled = enabled,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { AppText(stringResource(R.string.webdav_password)) },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val label = stringResource(
                            if (passwordVisible) R.string.webdav_hide_password else R.string.webdav_show_password,
                        )
                        WebDavIconAction(label, enabled, onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, label)
                        }
                    },
                )
                AppPrimaryButton(
                    text = stringResource(R.string.webdav_save),
                    enabled = !state.busy && config.isValid() && !saved,
                    onClick = { viewModel.saveWebDavConfig(config) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AppText(stringResource(R.string.webdav_files), modifier = Modifier.weight(1f))
                    WebDavIconAction(
                        label = stringResource(R.string.webdav_refresh),
                        onClick = { viewModel.refreshWebDavBackups() },
                        enabled = saved && !state.busy,
                    ) { Icon(Icons.Default.Refresh, stringResource(R.string.webdav_refresh)) }
                    WebDavIconAction(
                        label = stringResource(R.string.webdav_upload),
                        onClick = { selectingBackup = true },
                        enabled = saved && !state.busy,
                    ) { Icon(Icons.Default.CloudUpload, stringResource(R.string.webdav_upload)) }
                }
                if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                state.error?.let { AppText(it, color = MaterialTheme.colorScheme.error) }
                if (!state.loaded && !state.busy) {
                    TextButton(onClick = { viewModel.loadWebDavConfig() }) {
                        AppText(stringResource(R.string.webdav_retry))
                    }
                }
                if (state.listed && state.files.isEmpty()) AppText(stringResource(R.string.webdav_empty))
                state.files.forEach { file ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = saved && !state.busy) { viewModel.prepareWebDavRestore(file) }
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AppText(file.name)
                        val details = listOfNotNull(
                            file.size?.let { Formatter.formatShortFileSize(context, it) },
                            file.modifiedAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) },
                        ).joinToString("  ")
                        if (details.isNotEmpty()) AppText(details)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !state.busy) {
                AppText(stringResource(R.string.webdav_close))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebDavIconAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { AppText(label) } },
        state = rememberTooltipState(),
    ) {
        AppIconButton(onClick = onClick, enabled = enabled, content = content)
    }
}

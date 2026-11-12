package io.github.magisk317.uikit.preference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import io.github.magisk317.uikit.common.AppSnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.magisk317.uikit.common.AppSnackbarHost
import io.github.magisk317.uikit.common.showLatestSnackbar
import io.github.magisk317.uikit.surface.AppTextField
import io.github.magisk317.uikit.surface.rememberSaveableTextFieldState
import io.github.magisk317.uikit.surface.AppTopBar
import java.net.URI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun UrlSourceSettingsScreen(
    title: String,
    fieldLabel: String,
    supportingText: String,
    invalidUrlMessage: String,
    savedMessage: String,
    saveFailedMessage: String,
    saveContentDescription: String,
    valueFlow: Flow<String>,
    onSaveValue: suspend (String) -> String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val persistedValue by valueFlow.collectAsState(initial = "")
    val fieldValue = rememberSaveableTextFieldState()
    val fieldText = fieldValue.text.toString()
    var lastObservedValue by rememberSaveable { mutableStateOf("") }
    var observedValue by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { AppSnackbarHostState() }
    val validationError = if (isValidHttpUrlSource(fieldText)) null else invalidUrlMessage

    LaunchedEffect(persistedValue) {
        if (!observedValue || fieldValue.text.toString() == lastObservedValue) {
            fieldValue.setTextAndPlaceCursorAtEnd(persistedValue)
        }
        lastObservedValue = persistedValue
        observedValue = true
    }

    fun save() {
        if (saving || validationError != null) return
        scope.launch {
            saving = true
            runCatching { onSaveValue(fieldValue.text.toString()) }
                .onSuccess { savedValue ->
                    lastObservedValue = savedValue
                    fieldValue.setTextAndPlaceCursorAtEnd(savedValue)
                    snackbarHostState.showLatestSnackbar(savedMessage)
                }
                .onFailure {
                    snackbarHostState.showLatestSnackbar(saveFailedMessage)
                }
            saving = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        enabled = !saving && validationError == null && fieldText.trim() != persistedValue,
                        onClick = ::save,
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = saveContentDescription)
                    }
                },
            )
        },
        snackbarHost = {
            AppSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTextField(
                state = fieldValue,
                label = fieldLabel,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(validationError ?: supportingText, color = MaterialTheme.colorScheme.onSurface)
                    Text(validationError ?: supportingText)
                },
                trailingIcon = if (fieldText.isNotEmpty()) {
                    {
                        IconButton(onClick = { fieldValue.edit { replace(0, length, "") } }) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
        }
    }
}

fun isValidHttpUrlSource(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return true
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
    return (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) &&
        !uri.host.isNullOrBlank() && uri.rawQuery == null && uri.rawFragment == null
}

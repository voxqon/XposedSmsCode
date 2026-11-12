package io.github.magisk317.smscode.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.magisk317.smscode.ui.theme.AppTheme
import io.github.magisk317.uikit.surface.AppIcon
import io.github.magisk317.uikit.surface.AppIconButton
import io.github.magisk317.uikit.surface.AppTopBar
import io.github.magisk317.uikit.text.AppText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalPrivacyActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var document by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                document = withContext(Dispatchers.IO) {
                    assets.open("documents/PRIVACY.md").bufferedReader().use { it.readText() }
                }
            }
            AppTheme {
                Scaffold(
                    topBar = {
                        AppTopBar(
                            title = getString(io.github.magisk317.uikit.R.string.pref_privacy_policy_title),
                            navigationIcon = {
                                AppIconButton(onClick = { finish() }) {
                                    AppIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = getString(android.R.string.cancel))
                                }
                            },
                        )
                    },
                ) { padding ->
                    SelectionContainer(
                        modifier = Modifier.fillMaxSize().padding(padding)
                            .verticalScroll(rememberScrollState()).padding(16.dp),
                    ) {
                        AppText(text = document)
                    }
                }
            }
        }
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(
                Intent(context, LocalPrivacyActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

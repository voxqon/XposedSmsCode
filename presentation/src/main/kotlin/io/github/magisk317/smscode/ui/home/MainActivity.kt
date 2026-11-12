@file:Suppress("LocalContextGetResourceValueCall")

package io.github.magisk317.smscode.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import io.github.magisk317.uikit.common.AppSnackbarHostState
import io.github.magisk317.uikit.surface.AppSurface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import io.github.magisk317.smscode.presentation.BuildConfig
import io.github.magisk317.smscode.presentation.R
import io.github.magisk317.smscode.runtime.common.utils.FrameworkCompatibilityMonitor
import io.github.magisk317.xposed.logging.XLog
import io.github.magisk317.smscode.common.utils.AppPreferences
import io.github.magisk317.smscode.common.utils.xscPackageActions
import io.github.magisk317.smscode.runtime.common.utils.StringUtils
import io.github.magisk317.uikit.theme.applyEdgeToEdge
import io.github.magisk317.uikit.common.AppSnackbarHost
import io.github.magisk317.uikit.foundation.LocalSnackbarHostState
import io.github.magisk317.smscode.ui.nav.SmsCodeNavHost
import io.github.magisk317.smscode.common.constant.Const
import io.github.magisk317.smscode.ui.theme.AppTheme
import io.github.magisk317.uikit.surface.AppAlertDialog
import io.github.magisk317.uikit.surface.AppPrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import io.github.magisk317.uikit.text.AppText
import io.github.magisk317.uikit.theme.AppColorRole
import io.github.magisk317.uikit.theme.appColor

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        XLog.w(message = "MainActivity.onCreate() called pid=%d", android.os.Process.myPid())
        applyEdgeToEdge(this)

        setContent {
            val viewModel: SettingsViewModel = koinViewModel()
            val themeState by viewModel.themeState.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val appSnackbarHostState = remember { AppSnackbarHostState() }
            var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
            var blockingStartupDialog by remember {
                mutableStateOf<FrameworkCompatibilityMonitor.FrameworkIssue?>(null)
            }
            var startupBlockingCheckComplete by remember { mutableStateOf(false) }
            var snackbarBottomOverlayPadding by remember { mutableStateOf(0.dp) }

            // Circular Reveal Animation State
            var currentThemeMode by remember { mutableIntStateOf(themeState.mode) }
            var currentUiKitStyle by remember { mutableIntStateOf(themeState.uiKitStyle) }
            val themeRevealState = io.github.magisk317.uikit.theme.rememberThemeRevealState()
            val view = LocalView.current
            var requestedTab by remember { mutableStateOf<Any?>(null) }

            LaunchedEffect(Unit) {
                if (!AppPreferences.isPrivacyPolicyAccepted(context)) {
                    showPrivacyPolicyDialog = true
                }
            }
            LaunchedEffect(Unit) {
                startupBlockingCheckComplete = false
                try {
                    val frameworkIssue = withContext(Dispatchers.IO) {
                        xscPackageActions.inspectFrameworkIssue(context)
                    }
                    if (frameworkIssue != null) {
                        blockingStartupDialog = frameworkIssue
                    }
                } finally {
                    startupBlockingCheckComplete = true
                }
            }
            // Effect to trigger logic when ThemeState changes
            LaunchedEffect(themeState) {
                if (themeState.mode != currentThemeMode) {
                    val requestedCenter = if (themeState.centerX >= 0f && themeState.centerY >= 0f) {
                        Offset(themeState.centerX, themeState.centerY)
                    } else {
                        Offset.Unspecified
                    }
                    val animated = themeRevealState.animateThemeChange(
                        view = view,
                        requestedCenter = requestedCenter,
                    ) {
                        currentThemeMode = themeState.mode
                        currentUiKitStyle = themeState.uiKitStyle
                    }
                    if (!animated) {
                        currentThemeMode = themeState.mode
                        currentUiKitStyle = themeState.uiKitStyle
                    }
                } else if (themeState.uiKitStyle != currentUiKitStyle) {
                    currentUiKitStyle = themeState.uiKitStyle
                } else {
                    // Initial load
                    currentThemeMode = themeState.mode
                    currentUiKitStyle = themeState.uiKitStyle
                }
            }

            // Collect navigation events
            LaunchedEffect(viewModel.eventsFlow) {
                viewModel.eventsFlow.collect { event ->
                    when (event) {
                        is SettingsEvent.ShowPrivacyPolicy -> showPrivacyPolicyDialog = true
                        is SettingsEvent.SmsCodeTestResult -> {
                            val message = if (event.code.isBlank()) {
                                context.getString(R.string.cannot_parse_smscode)
                            } else {
                                val base = context.getString(R.string.current_sms_code, event.code)
                                val hitRule = event.matchedRuleLabel?.takeIf { it.isNotBlank() }?.let {
                                    context.getString(R.string.hit_rule_label, it)
                                }
                                if (hitRule == null) {
                                    base
                                } else {
                                    context.getString(R.string.sms_code_test_result_with_rule, base, hitRule)
                                }
                            }
                            XLog.i(
                                message = "Sms code test result delivered in MainActivity: code=%s matchedRule=%s",
                                if (BuildConfig.DEBUG || BuildConfig.IS_CI_BUILD) {
                                    StringUtils.escape(event.code)
                                } else {
                                    StringUtils.summarizeCode(event.code)
                                },
                                event.matchedRuleLabel ?: "",
                            )
                            scope.launch { appSnackbarHostState.showSnackbar(message) }
                        }
                        is SettingsEvent.NavigateToRules -> {
                            requestedTab = io.github.magisk317.smscode.ui.nav.SmsCodeRulesRoute()
                        }
                        is SettingsEvent.NavigateToRecords -> requestedTab = io.github.magisk317.smscode.ui.nav.RecordsRoute
                        is SettingsEvent.NavigateToSettings -> requestedTab = io.github.magisk317.smscode.ui.nav.SettingsRoute
                        is SettingsEvent.ShowSnackbar -> {
                            scope.launch { appSnackbarHostState.showSnackbar(event.message) }
                        }
                        else -> {}
                    }
                }
            }

            CompositionLocalProvider(LocalSnackbarHostState provides appSnackbarHostState) {
                AppTheme {
                    AppSurface(color = appColor(AppColorRole.Background)) {
                    LaunchedEffect(Unit) {
                        viewModel.setInternalFilesWritable()
                    }
                    LaunchedEffect(intent) {
                        viewModel.handleArguments(intent.extras)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        val shouldShowRegularUi = startupBlockingCheckComplete && blockingStartupDialog == null
                        if (shouldShowRegularUi) {
                            SmsCodeNavHost(
                                navController = navController,
                                onBack = { finish() },
                                initialTab = requestedTab,
                                onInitialTabConsumed = { requestedTab = null },
                                onBottomOverlayPaddingChanged = { snackbarBottomOverlayPadding = it },
                                modifier = Modifier,
                            )
                        }

                        if (shouldShowRegularUi && showPrivacyPolicyDialog) {
                            PrivacyPolicyDialog(
                                onDismiss = {},
                                onConfirm = {
                                    scope.launch { AppPreferences.setPrivacyPolicyAccepted(context, true) }
                                    showPrivacyPolicyDialog = false
                                },
                                onCancel = {
                                    scope.launch { AppPreferences.setPrivacyPolicyAccepted(context, false) }
                                    showPrivacyPolicyDialog = false
                                    finish()
                                },
                                onViewPolicy = {
                                    LocalPrivacyActivity.open(context)
                                },
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false,
                            )
                        }

                        blockingStartupDialog?.let { issue ->
                            ExitOnlyConflictDialog(
                                title = getString(R.string.framework_incompatibility_title),
                                text = buildAnnotatedString {
                                    append(
                                        when (issue.issueType) {
                                            FrameworkCompatibilityMonitor.FrameworkIssueType.HOOKER_ANNOTATION_INCOMPATIBLE ->
                                                getString(R.string.framework_incompatibility_hooker_annotation_message)
                                        },
                                    )
                                },
                                confirmText = getString(R.string.relay_conflict_dialog_exit),
                                onExit = {
                                    blockingStartupDialog = null
                                    finish()
                                },
                            )
                        }

                        io.github.magisk317.uikit.theme.ThemeRevealOverlay(themeRevealState)
                        AppSnackbarHost(
                            hostState = appSnackbarHostState,
                            modifier = Modifier
                                .then(
                                    if (snackbarBottomOverlayPadding > 0.dp) {
                                        Modifier.padding(bottom = snackbarBottomOverlayPadding)
                                    } else {
                                        Modifier.navigationBarsPadding()
                                    },
                                ),
                        )
                    }
                }
                }
            }
        }
    }

}

@Composable
private fun ExitOnlyConflictDialog(
    title: String,
    text: androidx.compose.ui.text.AnnotatedString,
    confirmText: String,
    onExit: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = {},
        title = { AppText(title) },
        text = { AppText(text) },
        confirmButton = {
            AppPrimaryButton(
                text = confirmText,
                onClick = onExit,
            )
        },
    )
}

private const val MAX_CAPTURE_PIXELS = 8_388_608L
private const val MAX_CAPTURE_SIDE_PX = 4096
private const val LARGE_BITMAP_ERROR_KEYWORD = "trying to draw too large"

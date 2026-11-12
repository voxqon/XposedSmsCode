package io.github.magisk317.uikit.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Snackbar as MaterialSnackbar
import androidx.compose.material3.SnackbarData as MaterialSnackbarData
import androidx.compose.material3.SnackbarVisuals as MaterialSnackbarVisuals
import androidx.compose.material3.SnackbarDuration as MaterialSnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import io.github.magisk317.uikit.theme.LocalUiKitStyle
import io.github.magisk317.uikit.theme.UiKitStyle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import top.yukonga.miuix.kmp.basic.Snackbar as MiuixSnackbar
import top.yukonga.miuix.kmp.basic.SnackbarData as MiuixSnackbarData
import top.yukonga.miuix.kmp.basic.SnackbarDuration as MiuixSnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarVisuals as MiuixSnackbarVisuals

private const val SNACKBAR_SHORT_DURATION_MS = 4_000L
private const val SNACKBAR_LONG_DURATION_MS = 10_000L

/** Shared duration semantics without exposing either UI toolkit's enum. */
sealed interface AppSnackbarDuration {
    data object Short : AppSnackbarDuration
    data object Long : AppSnackbarDuration
    data object Indefinite : AppSnackbarDuration
    data class Custom(val durationMillis: kotlin.Long) : AppSnackbarDuration {
        init {
            require(durationMillis > 0) { "durationMillis must be greater than 0" }
        }
    }
}

enum class AppSnackbarResult {
    Dismissed,
    ActionPerformed,
}

@Stable
interface AppSnackbarData {
    val visuals: AppSnackbarVisuals

    suspend fun dismiss()

    suspend fun performAction()
}

@androidx.compose.runtime.Immutable
data class AppSnackbarVisuals(
    val message: String,
    val actionLabel: String?,
    val withDismissAction: Boolean,
    val duration: AppSnackbarDuration,
)

@Stable
class AppSnackbarHostState {
    private val mutex = Mutex()
    private val entries = ArrayDeque<Entry>()
    private var nextId = 0L

    var currentSnackbarData: AppSnackbarData? by mutableStateOf(null)
        private set

    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: AppSnackbarDuration = if (actionLabel == null) {
            AppSnackbarDuration.Short
        } else {
            AppSnackbarDuration.Indefinite
        },
    ): AppSnackbarResult {
        val result = CompletableDeferred<AppSnackbarResult>()
        lateinit var entry: Entry
        val data = object : AppSnackbarData {
            override val visuals = AppSnackbarVisuals(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
                duration = duration,
            )

            override suspend fun dismiss() {
                complete(entry, AppSnackbarResult.Dismissed)
            }

            override suspend fun performAction() {
                complete(entry, AppSnackbarResult.ActionPerformed)
            }
        }
        entry = Entry(++nextId, data, result)

        mutex.withLock {
            entries.addLast(entry)
            if (currentSnackbarData == null) {
                currentSnackbarData = entry.data
            }
        }
        return result.await()
    }

    suspend fun dismissCurrent() {
        currentSnackbarData?.dismiss()
    }

    private suspend fun complete(entry: Entry, result: AppSnackbarResult) {
        mutex.withLock {
            if (entry.result.isCompleted) return
            entry.result.complete(result)
            entries.remove(entry)
            currentSnackbarData = entries.firstOrNull()?.data
        }
    }

    private data class Entry(
        val id: Long,
        val data: AppSnackbarData,
        val result: CompletableDeferred<AppSnackbarResult>,
    )
}

/**
 * Shared snackbar host. Material/Expressive uses Material snackbar chrome and Miuix uses
 * its native snackbar chrome, while callers keep one state, queue, action and dismiss API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSnackbarHost(
    hostState: AppSnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 16.dp,
    snackbar: (@Composable (AppSnackbarData) -> Unit)? = null,
) {
    val data = hostState.currentSnackbarData ?: return

    LaunchedEffect(data) {
        when (val duration = data.visuals.duration) {
            AppSnackbarDuration.Indefinite -> Unit
            AppSnackbarDuration.Short -> {
                delay(SNACKBAR_SHORT_DURATION_MS)
                data.dismiss()
            }
            AppSnackbarDuration.Long -> {
                delay(SNACKBAR_LONG_DURATION_MS)
                data.dismiss()
            }
            is AppSnackbarDuration.Custom -> {
                delay(duration.durationMillis)
                data.dismiss()
            }
        }
    }

    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(data, dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            data.dismiss()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier.padding(bottom = bottomPadding),
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {},
        ) {
            (snackbar ?: { snackbarData -> AppSnackbar(snackbarData) })(data)
        }
    }
}

@Composable
fun AppSnackbar(
    data: AppSnackbarData,
    modifier: Modifier = Modifier,
    actionLabel: String? = data.visuals.actionLabel,
    onAction: (suspend () -> Unit)? = null,
) {
    val style = LocalUiKitStyle.current
    val scope = rememberCoroutineScope()
    if (style == UiKitStyle.Miuix) {
        MiuixSnackbar(
            data = MiuixSnackbarDataAdapter(data, actionLabel, onAction),
            modifier = modifier,
        )
    } else {
        MaterialSnackbar(
            MaterialSnackbarDataAdapter(data, actionLabel, onAction, scope),
            modifier = modifier,
        )
    }
}

private class MaterialSnackbarDataAdapter(
    private val data: AppSnackbarData,
    actionLabel: String?,
    private val onAction: (suspend () -> Unit)?,
    private val scope: CoroutineScope,
) : MaterialSnackbarData {
    override val visuals: MaterialSnackbarVisuals = object : MaterialSnackbarVisuals {
        override val message = data.visuals.message
        override val actionLabel = actionLabel
        override val withDismissAction = data.visuals.withDismissAction
        override val duration = data.visuals.duration.toMaterialDuration()
    }

    override fun dismiss() {
        scope.launch { data.dismiss() }
    }

    override fun performAction() {
        scope.launch {
            onAction?.invoke()
            data.performAction()
        }
    }
}

private class MiuixSnackbarDataAdapter(
    private val data: AppSnackbarData,
    actionLabel: String?,
    private val onAction: (suspend () -> Unit)?,
) : MiuixSnackbarData {
    override val visuals: MiuixSnackbarVisuals = MiuixSnackbarVisuals(
        message = data.visuals.message,
        actionLabel = actionLabel,
        withDismissAction = data.visuals.withDismissAction,
        duration = data.visuals.duration.toMiuixDuration(),
    )

    override suspend fun dismiss() = data.dismiss()

    override suspend fun performAction() {
        onAction?.invoke()
        data.performAction()
    }
}

private fun AppSnackbarDuration.toMaterialDuration(): MaterialSnackbarDuration = when (this) {
    AppSnackbarDuration.Short -> MaterialSnackbarDuration.Short
    AppSnackbarDuration.Long -> MaterialSnackbarDuration.Long
    AppSnackbarDuration.Indefinite -> MaterialSnackbarDuration.Indefinite
    is AppSnackbarDuration.Custom -> MaterialSnackbarDuration.Indefinite
}

private fun AppSnackbarDuration.toMiuixDuration(): MiuixSnackbarDuration = when (this) {
    AppSnackbarDuration.Short -> MiuixSnackbarDuration.Short
    AppSnackbarDuration.Long -> MiuixSnackbarDuration.Long
    AppSnackbarDuration.Indefinite -> MiuixSnackbarDuration.Indefinite
    is AppSnackbarDuration.Custom -> MiuixSnackbarDuration.Custom(durationMillis)
}

suspend fun AppSnackbarHostState.showLatestSnackbar(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: AppSnackbarDuration = if (actionLabel == null) {
        AppSnackbarDuration.Short
    } else {
        AppSnackbarDuration.Indefinite
    },
): AppSnackbarResult {
    dismissCurrent()
    return showSnackbar(message, actionLabel, withDismissAction, duration)
}

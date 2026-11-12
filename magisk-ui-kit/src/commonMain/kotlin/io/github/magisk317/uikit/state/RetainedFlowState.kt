package io.github.magisk317.uikit.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow

/** Process-local latest-value cache for UI state whose composition may leave the pager window. */
class RetainedValueCache<K : Any, V : Any> {
    private val values = mutableMapOf<K, V>()

    fun get(key: K): V? = values[key]

    fun put(key: K, value: V) {
        values[key] = value
    }

    fun remove(key: K) {
        values.remove(key)
    }

    fun clear() {
        values.clear()
    }
}

/**
 * Collects [flow] only while [isActive], while retaining its latest value outside composition.
 * This prevents a recreated pager page from briefly rendering [initialValue] again.
 */
@Composable
fun <K : Any, V : Any> rememberRetainedFlowState(
    cache: RetainedValueCache<K, V>,
    key: K,
    initialValue: V,
    isActive: Boolean,
    flow: Flow<V>,
): MutableState<V> {
    val state = remember(cache, key) {
        mutableStateOf(cache.get(key) ?: initialValue)
    }
    LaunchedEffect(cache, key, isActive, flow) {
        if (!isActive) return@LaunchedEffect
        flow.collect { value ->
            cache.put(key, value)
            state.value = value
        }
    }
    return state
}

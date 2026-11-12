package io.github.magisk317.uikit.state

import io.github.magisk317.uikit.surface.nowMillis

/** Thread-safe process-local value cache with a monotonic time-to-live. */
class TimedValueCache<T : Any>(
    val ttlMs: Long,
    private val clockMs: () -> Long = { nowMillis() },
) {
    private data class Entry<T>(val value: T, val storedAtMs: Long)

    @Volatile
    private var entry: Entry<T>? = null

    init {
        require(ttlMs > 0L) { "ttlMs must be positive" }
    }

    fun peek(): T? = entry?.value

    fun freshOrNull(nowMs: Long = clockMs()): T? {
        val snapshot = entry ?: return null
        val ageMs = nowMs - snapshot.storedAtMs
        return snapshot.value.takeIf { ageMs in 0 until ttlMs }
    }

    fun put(value: T, nowMs: Long = clockMs()): T {
        entry = Entry(value, nowMs)
        return value
    }

    fun invalidate() {
        entry = null
    }
}

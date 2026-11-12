package io.github.magisk317.uikit.foundation


object SessionLoadingRegistry {
    private val shownKeys = mutableSetOf<String>()

    fun shouldShowInitial(key: String): Boolean = !shownKeys.contains(key)

    fun markShown(key: String) {
        shownKeys.add(key)
    }
}
